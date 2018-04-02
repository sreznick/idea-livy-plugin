package com.intellij.plugin.livy.rest

import com.intellij.plugin.livy.ServerData.CreateSession.{GetSessionLog, GetStatements, PostStatements}
import com.intellij.plugin.livy.ServerData._

import scala.concurrent.Future
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global

trait TimedLivyRest extends LivyRest {
  private def measure[T](action: () => Future[T]): Future[T] = {
    val start = System.currentTimeMillis()

    val res = action()

    res.onComplete {
      case Failure(e) =>
        val finish = System.currentTimeMillis()
        // сообщим куда следует
      case _ =>
        val finish = System.currentTimeMillis()
    }

    res
  }

  abstract override def newSession(request: CreateSession.Request): Future[Session] = {
    measure(() => super.newSession(request))
  }

  abstract override def getSessions(request: GetSessions.Request): Future[GetSessions.Response] = {
    measure(() => super.getSessions(request))
  }

  abstract override def getSession(sessionId: Int): Future[Session] = {
    measure(() => super.getSession(sessionId))
  }

  abstract override def getSessionState(sessionId: Int): Future[SessionState] = {
    measure(() => super.getSessionState(sessionId))
  }

  abstract override def deleteSession(sessionId: Int): Future[Unit] = {
    measure(() => super.deleteSession(sessionId))
  }

  abstract override def getSessionLog(sessionId: Int, request: GetSessionLog.Request): Future[GetSessionLog.Response] = {
    measure(() => super.getSessionLog(sessionId, request))
  }

  abstract override def runStatement(sessionId: Int, request: PostStatements.Request): Future[Statement] = {
    measure(() => super.runStatement(sessionId, request))
  }

  abstract override def getStatements(sessionId: Int): Future[Seq[GetStatements.Response]] = {
    measure(() => super.getStatements(sessionId))
  }

}
