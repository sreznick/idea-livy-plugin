import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.8",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "pi",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "org.apache.livy" %% "livy-scala-api" % "0.5.0-incubating",
    libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.3.0" 
  )
