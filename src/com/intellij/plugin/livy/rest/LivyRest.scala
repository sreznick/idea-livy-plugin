package com.intellij.plugin.livy.rest

import com.intellij.plugin.livy.ServerData.CreateSession.{GetSessionLog, GetStatements, PostStatements}
import com.intellij.plugin.livy.ServerData._

import scala.concurrent.Future


trait LivyRest {
  def newSession(request: CreateSession.Request): Future[Session]

  def getSession(sessionId: Int): Future[Session]

  def getSessions(request: GetSessions.Request): Future[GetSessions.Response]

  def getSessionState(sessionId: Int): Future[SessionState]

  def deleteSession(sessionId: Int): Future[Unit]

  def getSessionLog(sessionId:Int, request: GetSessionLog.Request): Future[GetSessionLog.Response]

  def runStatement(sessionId: Int, request: PostStatements.Request): Future[Statement]

  def getStatements(sessionId: Int): Future[Seq[GetStatements.Response]]
}