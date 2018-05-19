package com.intellij.plugin.livy.session

import com.intellij.plugin.livy.data.ServerData.CreateSession.GetSessionLog
import com.intellij.plugin.livy.data.ServerData.Statement
import org.apache.livy.LivyClient
import org.apache.livy.scalaapi.LivyScalaClient

import scala.concurrent.Future

trait SessionManager {
  def livyClient(sessionId: Int): LivyScalaClient
  def startSession(): Future[Session]
  def selectSession(id: Int): Session = new Session(this, id)
  def stopSession(id: Int): Future[Unit]
  def invokeStatement(sessionId: Int, code: String): Future[Statement]
  def getStatement(sessionId: Int, statementId: Int): Future[Statement]
  def getSessionLog(sessionId: Int, from: Int, to: Int): Future[GetSessionLog.Response]
}
