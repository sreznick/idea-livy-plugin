package com.intellij.plugin.livy.session

import com.intellij.plugin.livy.ServerData.CreateSession.GetSessionLog
import com.intellij.plugin.livy.ServerData.Statement

import scala.concurrent.Future

trait SessionManager {
  def startSession(): Future[Session]
  def selectSession(id: Int): Session = new Session(this, id)
  def invokeStatement(sessionId: Int, code: String): Future[Statement]
  def getStatement(sessionId: Int, statementId: Int): Future[Statement]
  def getSessionLog(sessionId: Int, from: Int, to: Int): Future[GetSessionLog.Response]
}
