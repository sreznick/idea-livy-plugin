package com.intellij.plugin.livy.rest

import cats.effect._
import com.intellij.plugin.livy.ServerData.CreateSession.{GetSessionLog, GetStatements, PostStatements}
import com.intellij.plugin.livy.ServerData._
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze.Http1Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._

import scala.concurrent.Future

import com.intellij.plugin.livy.ServerData.Decoders._
import com.intellij.plugin.livy.ServerData.Encoders._


case class Q(kind: SessionKind.Value)
case class S(id: Int, state: String)

abstract class Http4sLivyRest extends LivyRest with Http4sClientDsl[IO]  {
  protected val url: String

  private val serverUri = Uri.fromString(url) match {
    case Left(v) =>
      throw new IllegalArgumentException()
    case Right(v) =>
      v
  }

  override def config = serverUri.toString

  def newSession(request: CreateSession.Request): Future[Session] = {
    val httpRequest = POST(serverUri.withPath("/sessions"),
      request.asJson.pretty(Printer.noSpaces.copy(dropNullValues = true)))

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[Session](httpRequest)(jsonOf[IO, Session])
    } unsafeToFuture()
  }

  override def getSession(sessionId: Int): Future[Session] = {
    val httpRequest = GET(serverUri.withPath(s"/sessions/$sessionId"))

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[Session](httpRequest)(jsonOf[IO, Session])
    } unsafeToFuture()
  }


  override def getSessions(request: GetSessions.Request): Future[GetSessions.Response] = {
    val httpRequest = GET(serverUri.withPath("/sessions")).withBody(
      request.asJson.pretty(Printer.noSpaces.copy(dropNullValues = true)))

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[GetSessions.Response](httpRequest)(jsonOf[IO, GetSessions.Response])
    } unsafeToFuture()
  }

  override def getSessionState(sessionId: Int): Future[SessionState] = {
    val httpRequest = GET(serverUri.withPath(s"/sessions/$sessionId/state"))

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[SessionState](httpRequest)(jsonOf[IO, SessionState])
    } unsafeToFuture()
  }

  override def deleteSession(sessionId: Int): Future[Unit] = {
    val httpRequest = DELETE(serverUri.withPath(s"/sessions/$sessionId"))

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[Unit](httpRequest)
    } unsafeToFuture()
  }

  override def getSessionLog(sessionId: Int, request: GetSessionLog.Request): Future[GetSessionLog.Response] = {
    val httpRequest = GET(serverUri.withPath(s"/sessions/$sessionId/log")).withBody(request.asJson)

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[GetSessionLog.Response](httpRequest)(jsonOf[IO, GetSessionLog.Response])
    } unsafeToFuture()
  }

  override def runStatement(sessionId: Int, request: PostStatements.Request): Future[Statement] = {
    val data = request.asJson.pretty(Printer.noSpaces.copy(dropNullValues = true))
    val path = s"/sessions/$sessionId/statements"
    val httpRequest = POST(serverUri.withPath(s"/sessions/$sessionId/statements"), data)

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[Statement](httpRequest)(jsonOf[IO, Statement])
    } unsafeToFuture()
  }

  override def getStatements(sessionId: Int): Future[GetStatements.Response] = {
    val httpRequest = GET(serverUri.withPath(s"/sessions/$sessionId/statements"))

    import Decoders._

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[GetStatements.Response](httpRequest)(jsonOf[IO, GetStatements.Response])
    } unsafeToFuture()
  }

  def getStatement(sessionId: Int, statementId: Int): Future[Statement] = {
    val httpRequest = GET(serverUri.withPath(s"/sessions/$sessionId/statements/$statementId"))

    import Decoders._

    Http1Client[IO]().flatMap { httpClient =>
      httpClient.expect[Statement](httpRequest)(jsonOf[IO, Statement])
    } unsafeToFuture()
  }
}