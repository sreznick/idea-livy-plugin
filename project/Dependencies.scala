import sbt._

object Dependencies {
  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

  val http4sVersion = "0.18.5"
  val http4sGroup = Seq(
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion
  )

  val circeVersion = "0.9.3"
  val circeGroup = Seq(
    "io.circe" %% "circe-literal" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-generic-extras" % circeVersion
  )

  val mainDependencies: Seq[ModuleID] = http4sGroup ++ circeGroup :+ scalaTest
}
