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
      case class Response(total_statements: Int, statements: Seq[Statement])
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
  }

  case class StatementOutput(status: String, executionCount: Int, data: OutputContents)
}