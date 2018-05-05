package com.intellij.plugin.livy.session

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

class LogManager(sessionManager: SessionManager, id: Integer, lineProcessor: String => Unit) {
  private val worker = new ScheduledThreadPoolExecutor(1)

  val lastLine = new AtomicInteger()

  lastLine.set(0)

  val action: Runnable = () => checkLog()

  private def schedule(after: Int) = worker.schedule(action, after, TimeUnit.MILLISECONDS)

  schedule(1000)

  private def checkLog(): Unit = {
    val step = 5
    sessionManager.getSessionLog(id, lastLine.get(), step).onComplete {
      case Success(resp) =>
        for (s <- resp.log) {
          lineProcessor(s)
        }
        lastLine.set(resp.from + resp.log.size)

        schedule(if (resp.log.size == 0) 1000 else 10)
      case Failure(e) =>
    }
  }
}
