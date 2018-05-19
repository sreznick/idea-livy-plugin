package com.intellij.plugin.livy.rest

import com.intellij.plugin.livy.data.ServerData.CreateSession.{GetSessionLog, GetStatements, PostStatements}
import com.intellij.plugin.livy.data.ServerData._

import scala.concurrent.Future


trait LivyRest {
  def baseUri: String

  def newSession(request: CreateSession.Request): Future[Session]

  def getSession(sessionId: Int): Future[Session]

  def getSessions(request: GetSessions.Request): Future[GetSessions.Response]

  def getSessionState(sessionId: Int): Future[SessionState]

  def deleteSession(sessionId: Int): Future[DeleteResponse]

  def getSessionLog(sessionId:Int, request: GetSessionLog.Request): Future[GetSessionLog.Response]

  def runStatement(sessionId: Int, request: PostStatements.Request): Future[Statement]

  def getStatements(sessionId: Int): Future[GetStatements.Response]

  def getStatement(sessionId: Int, statementId: Int): Future[Statement]
}