package com.intellij.plugin.livy

import io.circe.{Decoder, HCursor}
import io.circe._, io.circe.generic.semiauto._

object ServerData {
  object SessionKind extends Enumeration {
    val Spark = Value("spark")
    val PySpark = Value("pyspark")
    val SparkR = Value("sparkr")
    val Sql = Value("sql")
  }

  object GetSessions {
    case class Request(from: Option[Int] = None, size: Option[Int] = None)
    case class Response(from: Int, total: Int, sessions: Seq[Session])
  }

  object CreateSession {
    case class Request(kind: String, // временный заменитель enum
                       proxyUser: Option[String] = None,
                       jars: Option[Seq[String]] = None,
                       pyFiles: Option[Seq[String]] = None,
                       files: Option[Seq[String]] = None,
                       driverMemory: Option[String] = None,
                       driverCores: Option[Int] = None,
                       executorMemory: Option[String] = None,
                       executorCores: Option[Int] = None,
                       numExecutors: Option[Int] = None,
                       archives: Option[Seq[String]] = None,
                       queue: Option[String] = None,
                       name: Option[String] = None,
                       conf: Option[Map[String, String]] = None,
                       hearbeatTimeoutInSecond: Option[Int] = None
                      )

    object Request {
      def apply(kind: SessionKind.Value): Request = {
        val res = Request(kind.toString)

        println(res)

        res
      }
    }

    object GetSessionLog  {
      case class Request(from: Int, size: Int)
      case class Response(id: Int, from: Int, total: Int, log: Seq[String])
    }

    object PostStatements  {
      case class Request(code: String, kind: Option[String] = None)
    }

    object GetStatements {
      case class Response(totalStatements: Int, statements: Seq[Statement])
    }
  }

  case class Session(id: Int,
                     appId: Option[String],
                     owner: Option[String],
                     proxyUser: Option[String],
                     kind: String,
                     log: Seq[String],
                     state: String,
                      appInfo: Map[String, Option[String]]
                    )

  case class SessionState(id: Int, state: String)

  case class Statement(id: Int, code: String, state: String, output: Option[StatementOutput], progress: Float)

  case class OutputContents(plain: String)

  object Decoders {
    implicit val decodeOutputContents: Decoder[OutputContents] =
      Decoder.forProduct1("text/plain")(OutputContents.apply)

    implicit val decodeStatementOutput: Decoder[StatementOutput] =
      Decoder.forProduct3("status", "execution_count", "data")(StatementOutput.apply)

    implicit val decodeStatement: Decoder[Statement] =
      Decoder.forProduct5("id", "code", "state", "output", "progress")(Statement.apply)

    implicit val decodeSessionState: Decoder[SessionState] =
      Decoder.forProduct2("id", "state")(SessionState.apply)

    implicit val decodeSession: Decoder[Session] =
      Decoder.forProduct8("id", "appId", "owner", "proxyUser", "kind", "log", "state", "appInfo")(Session.apply)

    implicit val decodeGetStatements: Decoder[CreateSession.GetStatements.Response] =
      Decoder.forProduct2("total_statements", "statements")(CreateSession.GetStatements.Response.apply)

    implicit val decodeGetSessionLog: Decoder[CreateSession.GetSessionLog.Response] =
      Decoder.forProduct4("id", "from", "total", "log")(CreateSession.GetSessionLog.Response.apply)

    implicit val decodeGetSessions: Decoder[GetSessions.Response] =
      Decoder.forProduct3("from", "total", "sessions")(GetSessions.Response.apply)
  }

  object Encoders {
    implicit val encodeSessionLog: Encoder[CreateSession.GetSessionLog.Request] =
      Encoder.forProduct2("from", "size")(u => (u.from, u.size))

    implicit val encodePostStatements: Encoder[CreateSession.PostStatements.Request] =
      Encoder.forProduct2("code", "kind")(u => (u.code, u.kind))

    implicit val encodeGetSessions: Encoder[GetSessions.Request] =
      Encoder.forProduct2("from", "size")(u => (u.from, u.size))

    implicit val encodeCreateSession: Encoder[CreateSession.Request] =
      Encoder.forProduct15(
          "kind", "proxyUser", "jars", "pyFiles", "files",
          "driverMemory", "driverCores", "executorMemory", "executorCores",
          "numExecutors", "archives", "queue", "name", "conf", "hearbeatTimeoutInSecond")(u =>
           (u.kind, u.proxyUser, u.jars, u.pyFiles, u.files,
            u.driverMemory, u.driverCores, u.executorMemory, u.executorCores,
            u.numExecutors, u.archives, u.queue, u.name, u.conf, u.hearbeatTimeoutInSecond))
  }

  case class StatementOutput(status: String, executionCount: Int, data: OutputContents)
}