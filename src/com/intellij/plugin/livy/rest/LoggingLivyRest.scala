package com.intellij.plugin.livy.rest

import com.intellij.plugin.livy.ServerData.CreateSession.{GetSessionLog, GetStatements, PostStatements}
import com.intellij.plugin.livy.ServerData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

trait LoggingLivyRest extends LivyRest {
  private def reportFail[T](action: () => Future[T]): Future[T] = {
    val res = action()

    res.onComplete {
      case Failure(e) =>
        println("FAILED: " + e)  // нопмальный лог нужен
      case _ =>
    }

    res
  }

  abstract override def newSession(request: CreateSession.Request): Future[Session] = {
    reportFail(() => super.newSession(request))
  }

  abstract override def getSessions(request: GetSessions.Request): Future[GetSessions.Response] = {
    reportFail(() => super.getSessions(request))
  }

  abstract override def getSession(sessionId: Int): Future[Session] = {
    reportFail(() => super.getSession(sessionId))
  }

  abstract override def getSessionState(sessionId: Int): Future[SessionState] = {
    reportFail(() => super.getSessionState(sessionId))
  }

  abstract override def deleteSession(sessionId: Int): Future[Unit] = {
    reportFail(() => super.deleteSession(sessionId))
  }

  abstract override def getSessionLog(sessionId: Int, request: GetSessionLog.Request): Future[GetSessionLog.Response] = {
    reportFail(() => super.getSessionLog(sessionId, request))
  }

  abstract override def runStatement(sessionId: Int, request: PostStatements.Request): Future[Statement] = {
    reportFail(() => super.runStatement(sessionId, request))
  }

  abstract override def getStatements(sessionId: Int): Future[Seq[GetStatements.Response]] = {
    reportFail(() => super.getStatements(sessionId))
  }

}