package com.intellij.plugin.livy.session

import java.net.URI
import java.util
import java.util.concurrent._

import com.intellij.plugin.livy.ServerData
import com.intellij.plugin.livy.ServerData.CreateSession.GetSessionLog
import com.intellij.plugin.livy.ServerData.Statement
import com.intellij.plugin.livy.rest.LivyRest
import org.apache.livy.{LivyClient, LivyClientBuilder}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.livy.scalaapi._


class RestSessionManager(rest: LivyRest) extends SessionManager {
  override def livyClient(sessionId: Int): LivyScalaClient = {
    val builder = new LivyClientBuilder(false)
    val base = rest.baseUri
    val uri = s"$base/sessions/$sessionId"
    builder.setURI(new URI(uri)).build().asScalaClient
  }

  private def waitState(state: String, sessionId: Int): Future[Session] = {
    rest.getSession(sessionId).flatMap {
      case session:ServerData.Session =>
        if (session.state == state)
            Future.successful(new Session(this, sessionId))
        else {
            Thread.sleep(2000)
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
            val builder = new LivyClientBuilder()
            Future.successful(new Session(this, session.id))
          case "starting" =>
            waitState("idle", session.id)
        }
    }
  }

  override def selectSession(id: Int): Session = {
    new Session(this, id)
  }

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