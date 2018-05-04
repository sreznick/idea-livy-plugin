package com.intellij.plugin.livy.session

import java.util
import java.util.concurrent._

import com.intellij.plugin.livy.ServerData
import com.intellij.plugin.livy.ServerData.CreateSession.GetSessionLog
import com.intellij.plugin.livy.ServerData.Statement
import com.intellij.plugin.livy.rest.LivyRest

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global


class RestSessionManager(rest: LivyRest) extends SessionManager {

  private def waitState(state: String, sessionId: Int): Future[Session] = {
    rest.getSession(sessionId).flatMap {
      case session:ServerData.Session =>
        if (session.state == state)
            Future.successful(new Session(this, sessionId))
        else {
            waitState(state, sessionId)
        }
    }
  }

  override def startSession(): Future[Session] = {
    val sessionData = ServerData.CreateSession.Request("spark")
    val sessionF = rest.newSession(sessionData)

    sessionF.flatMap {
      case session:ServerData.Session =>
        session.state match {
          case "idle" =>
            Future.successful(new Session(this, session.id))
          case "starting" =>
            waitState("idle", session.id)
        }
    }
  }

  override def selectSession(id: Int): Session = new Session(this, id)

  override def invokeStatement(sessionId: Int, code: String): Future[Statement] = {
    rest.runStatement(sessionId, ServerData.CreateSession.PostStatements.Request(code))
  }

  override def getStatement(sessionId: Int, statementId: Int): Future[Statement] = {
    rest.getStatement(sessionId, statementId)
  }

  override def getSessionLog(sessionId: Int, from: Int, size: Int): Future[GetSessionLog.Response] = {
    rest.getSessionLog(sessionId, GetSessionLog.Request(from, size))
  }

}
