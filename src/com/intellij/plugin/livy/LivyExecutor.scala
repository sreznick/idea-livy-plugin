package com.intellij.plugin.livy

import java.util.concurrent.atomic.AtomicReference

import com.intellij.execution.ui.{ConsoleView, ConsoleViewContentType}
import com.intellij.plugin.livy.ServerData.CreateSession.{GetSessionLog, GetStatements, PostStatements}
import com.intellij.plugin.livy.ServerData.Session
import com.intellij.plugin.livy.rest.{DefaultLivyRest, LivyRest}
import org.eclipse.aether.SessionData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class LivyExecutor(val consoleResult: ConsoleView, val consoleLog: ConsoleView) {
  val server = new AtomicReference[Option[LivyRest]](None)
  val session = new AtomicReference[Option[Session]](None)

  private def updateServer(livyRest: LivyRest): Unit = {
    server.set(Some(livyRest))
  }

  private def currentServer: Option[LivyRest] = server.get()

  private def updateSession(session: Session): Unit = {
    this.session.set(Some(session))
  }

  private def currentSession = session.get()

  private def report(s: String, consoleView: ConsoleView = consoleResult) = {
    consoleView.print(s + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
  }

  private def reportError(s: String, consoleView: ConsoleView = consoleResult) = {
    consoleView.print(s + "\n", ConsoleViewContentType.ERROR_OUTPUT)
  }

  def execute(command: String): Future[Unit] = {
    if (!command.isEmpty) {
      if (command(0) == ':') {
        // meta-command
        val args = command.drop(1).split(" ")

        args(0) match {
          case "status" =>
            val serverInfo = currentServer.map(_.config).getOrElse("Not Defined")
            report(s"server: $serverInfo")
            val sessionInfo = currentSession.map(_.id.toString).getOrElse("Not Defined")
            report(s"session: $sessionInfo")

          case "server" =>
            val restServer = new DefaultLivyRest(args(1))
            restServer.getSessions(ServerData.GetSessions.Request())
              .map(sessions => {
                report("sessions: " + sessions)
                updateServer(restServer)
              }) recoverWith {
              case e =>
                reportError(e.toString)
                throw e
            }

          case "open" =>
            currentServer match {
              case Some(server) =>
                server.newSession(ServerData.CreateSession.Request("spark")).map(session => {
                  report(s"opened $session")
                  updateSession(session)
                })
              case None =>
                reportError("Server is node defined")
            }

          case "select" =>
            currentServer match {
              case Some(server) =>
                currentServer.get.getSession(args(1).toInt).map(session => {
                  report(s"switched to $session")
                  updateSession(session)
                })
              case None =>
                reportError("Server is node defined")
            }

          case "result" =>
            currentServer match {
              case Some(server) =>
                currentServer.get.getStatement(args(1).toInt, args(2).toInt).map(stat => {
                  report(s"id: ${stat.id}")
                  report(s"code: ${stat.code}")
                  report(s"state: ${stat.state}")
                  report(s"progress: ${stat.progress}")
                  for (out <- stat.output) {
                    report(s"output state: ${out.status}")
                    report(s"output execution count: ${out.executionCount}")
                    report("---")
                    report(out.data.plain)
                  }
                })
              case None =>
                reportError("Server is node defined")
            }

          case "results" =>
            currentServer match {
              case Some(server) =>
                currentServer.get.getStatements(args(1).toInt).map(stats => {
                  report(stats.toString)
                })
              case None =>
                reportError("Server is node defined")
            }
        }
      } else {
        currentServer match {
          case None =>
            reportError("Server is not defined")
          case Some(server) =>
            currentSession match {
              case None =>
                reportError("Session is not defined")
              case Some(session) =>
                val cmdF = server.runStatement(session.id, PostStatements.Request(command))
                cmdF.onComplete {
                  case Success(result) =>
                    println("RES: " + result)
                    report(result.toString)

                    val logF = server.getSessionLog(session.id, GetSessionLog.Request(0, 100000))
                    logF.onComplete {
                      case Success(res) =>
                        println("LOG RES: " + res)
                        for (s <- res.log) {
                          report(s, consoleLog)
                        }
                    }


                }

                cmdF.flatMap {
                  case stat =>
                    Thread.sleep(2000)
                    server.getStatement(session.id, stat.id).andThen {
                      case Success(result) =>
                        report(s"STATE: ${result.state}")
                        for (out <- result.output) {
                          report(out.data.plain)
                        }
                      case Failure(e) =>
                        reportError(s"failed: $e")
                    }
                }
            }
        }
      }
    }
    Future.successful()
  }
}
