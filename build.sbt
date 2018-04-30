import Dependencies._

lazy val root = (project in file(".")).
  enablePlugins(SbtIdeaPlugin).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.5",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "org.http4s" %% "http4s-blaze-client" % "0.18.5" ,
    libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.18.5", 
    libraryDependencies += "org.http4s" %% "http4s-circe" % "0.18.5", 

    libraryDependencies += "io.circe" %% "circe-literal" % "0.9.3", 
    libraryDependencies += "io.circe" %% "circe-generic" % "0.9.3",
    libraryDependencies += "io.circe" %% "circe-generic-extras" % "0.9.3",
  )
