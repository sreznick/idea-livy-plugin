package com.intellij.plugin.livy

import java.util.concurrent.atomic.AtomicReference

import com.intellij.execution.ui.{ConsoleView, ConsoleViewContentType}
import com.intellij.plugin.livy.ServerData.CreateSession.{GetSessionLog, PostStatements}
import com.intellij.plugin.livy.ServerData.Session
import com.intellij.plugin.livy.rest.{DefaultLivyRest, LivyRest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class LivyExecutor(val console: ConsoleView) {
  val server = new AtomicReference[Option[LivyRest]](None)
  val session = new AtomicReference[Option[Session]](None)

  def execute(command: String): Future[Unit] = {
    println("EXECUTE " + command)
    if (!command.isEmpty) {
      if (command(0) == ':') {
        // meta-command
        val args = command.drop(1).split(" ")
        args(0) match {
          case "server" =>
            println("server: " + args.toSeq)
            server.set(Some(new DefaultLivyRest(args(1))))
            Future.successful()
          case "open" =>
            println("open: " + args.toSeq)
            val cmdF = server.get.get.newSession(ServerData.CreateSession.Request("spark"))
            cmdF.onComplete {
              case result =>
                println(result)
                console.print(result.toString, ConsoleViewContentType.NORMAL_OUTPUT)
                result match {
                  case Success(newSession) => session.set(Some(newSession))
                  case _ =>
                }
            }
            cmdF.map(_ => ())
        }
      } else {
        println("session:" + session.get.get.id)

        val cmdF = server.get.get.runStatement(session.get.get.id, PostStatements.Request(command))
        cmdF.onComplete {
          case Success(result) =>
            println("result: " + result)
            console.print(result.toString, ConsoleViewContentType.NORMAL_OUTPUT)
            val logF = server.get.get.getSessionLog(session.get.get.id, GetSessionLog.Request(0, 100000))
            logF.onComplete {
              case Success(res) =>
                for (s <- res.log) {
                  println(s)
                  console.print(s + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
                }
            }
        }
      }
    }
    Future.successful()
  }
}
