package com.intellij.plugin.livy.session

import java.util.concurrent.{ConcurrentHashMap, ScheduledThreadPoolExecutor, TimeUnit}

import com.intellij.plugin.livy.ServerData.CreateSession.GetSessionLog
import com.intellij.plugin.livy.ServerData.{Statement, StatementState}

import scala.concurrent.{Future, Promise}
import collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global


class Session(manager: SessionManager, val id: Int) {
  private val statementPromices = new ConcurrentHashMap[Int, Promise[Statement]]()

  private def registerWait(statement: Statement): Future[Statement] = {
    val p = Promise[Statement]()

    statementPromices.putIfAbsent(statement.id, p)

    statementPromices.get(statement.id).future
  }

  private def checkStatements(): Unit = {
    for (statementId <- statementPromices.keys.asScala) {
      manager.getStatement(id, statementId) map {
        case result =>
          val state = StatementState.withName(result.state)
          state match {
            case StatementState.Available =>
              statementPromices.get(result.id) success result
              statementPromices.remove(result.id)
            case StatementState.Running =>
            case StatementState.Waiting =>
            case StatementState.Error =>
              statementPromices.get(result.id) failure new Exception("statement error: " + result.id)
            case StatementState.Cancelling =>
              throw new Exception()
            case StatementState.Cancelled =>
              throw new Exception()
          }
      }
    }
  }

  private val worker = new ScheduledThreadPoolExecutor(1)

  worker.scheduleWithFixedDelay(() => checkStatements, 100, 100, TimeUnit.MILLISECONDS)

  def runStatement(code: String): Future[Statement] = {
    val statementF = manager.invokeStatement(id, code)

    statementF flatMap {
      case statement =>
        val stState = statement.state
        StatementState.withName(statement.state) match {
          case StatementState.Available =>
            Future.successful(statement)
          case StatementState.Running =>
            registerWait(statement)
          case StatementState.Waiting =>
            registerWait(statement)
          case StatementState.Error =>
            throw new Exception()
          case StatementState.Cancelling =>
            throw new Exception()
          case StatementState.Cancelled =>
            throw new Exception()
        }
    }
  }

  // temporary shortcut
  def getLog(from: Int, size: Int): Future[Seq[String]] = {
    manager.getSessionLog(id, from, size).map(_.log)
  }
}
