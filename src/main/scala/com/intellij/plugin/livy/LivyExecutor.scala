package com.intellij.plugin.livy

import java.util.concurrent.atomic.AtomicReference

import com.intellij.execution.ui.{ConsoleView, ConsoleViewContentType}
import com.intellij.plugin.livy.ServerData.StatementOutputStatus
import com.intellij.plugin.livy.rest.{DefaultLivyRest, LivyRest}
import com.intellij.plugin.livy.session.{RestSessionManager, Session, SessionManager}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class LivyExecutor(val consoleResult: ConsoleView,
                   val consoleLog: ConsoleView) {
  val session = new AtomicReference[Option[Session]](None)
  val sessionManager = new AtomicReference[Option[SessionManager]]()

  private def updateServer(livyRest: LivyRest, url: String): Unit = {
    sessionManager.set(Some(new RestSessionManager(new DefaultLivyRest(url))))
  }

  private def updateSession(session: Session): Unit = {
    this.session.set(Some(session))
  }

  private def currentSessionManager: Option[SessionManager] = sessionManager.get()
  private def currentSession = session.get()

  private def withSessionManager(f : SessionManager => Unit): Unit = {
    currentSessionManager match {
      case Some(sm) =>
        f(sm)
      case None =>
        reportError("Server is not defined")
    }
  }

  private def withSession(f : Session => Unit): Unit = {
    currentSession match {
      case Some(session) =>
        f(session)
      case None =>
        reportError("Session is not defined")
    }
  }


  private def report(s: String, consoleView: ConsoleView = consoleResult) = {
    consoleView.print(s + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
  }

  private def reportError(s: String, consoleView: ConsoleView = consoleResult, sep: String = "\n") = {
    consoleView.print(s + sep, ConsoleViewContentType.ERROR_OUTPUT)
  }

  def execute(command: String): Future[Unit] = {
    if (!command.isEmpty) {
      if (command(0) == ':') {
        // meta-command
        val args = command.drop(1).split(" ")

        args(0) match {
          case "status" =>
            val sessionInfo = currentSession.map(_.id.toString).getOrElse("Not Defined")
            report(s"session: $sessionInfo")

          case "server" =>
            val restServer = new DefaultLivyRest(args(1))
            restServer.getSessions(ServerData.GetSessions.Request())
              .map(result => {
                result.sessions.foreach {
                  case session =>
                    report(s"session ${session.id}: ${session.state}")
                }
                updateServer(restServer, args(1))
              }) recoverWith {
              case e =>
                reportError(e.toString)
                throw e
            }

          case "open" =>
            withSessionManager {
              case sm =>
                sm.startSession() map {
                  case session =>
                    report(s"created session ${session.id}")
                }
            }

          case "select" =>
            withSessionManager {
              case sm =>
                val session = sm.selectSession(args(1).toInt)
                report(s"switched to session ${session.id}")
                updateSession(session)
            }

          case "result" =>
            withSessionManager {
              case sm =>
                sm.getStatement(args(1).toInt, args(2).toInt).map(stat => {
                  report(s"id: ${stat.id}")
                  report(s"code: ${stat.code}")
                  report(s"state: ${stat.state}")
                  report(s"progress: ${stat.progress}")
                  for (out <- stat.output) {
                    report(s"output state: ${out.status}")
                    report(s"output execution count: ${out.executionCount}")
                    report("---")
                    report("output: " + out.data.map(_.plain).getOrElse(""))
                    report("traceback: " + out.traceback.map(_.mkString("")).getOrElse(""))
                  }
                })
            }
        }
      } else {
        withSession {
          case session =>
            val cmdF: Future[ServerData.Statement] = session.runStatement(command)

            cmdF.onComplete {
              case Success(result) =>
                // temporary stub
                session.getLog(0, 100000) map {
                  case lines =>
                    lines.foreach {
                      case line => report(line, consoleLog)
                    }
                }
              case Failure(_) =>
            }

            cmdF.map {
              case result =>
                for (out <- result.output) {
                  StatementOutputStatus.withName(out.status) match {
                    case StatementOutputStatus.Ok =>
                      report(s"${currentSession.get.id}/${result.id} done")
                      report(out.data.map(_.plain).getOrElse(""))
                    case StatementOutputStatus.Error =>
                      reportError(s"${currentSession.get.id}/${result.id} failed")
                      reportError("Error name:" + out.ename.getOrElse(""))
                      reportError("Error value:" + out.evalue.getOrElse(""))
                      out.traceback.getOrElse(Seq()).foreach {
                        case line =>
                          reportError(line, sep = "")
                      }
                    case state =>
                      reportError(s"Unexpected state: $state")
                  }
                }
            }
        }
      }
    }
    Future.successful()
  }
}