import Dependencies._

lazy val root = (project in file("."))
  .settings(
    organization := "com.sreznick",
    name         := "idea-livy-plugin",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := "2.11.8",
    ideaBuild    := "181.4203.550",
    libraryDependencies ++= mainDependencies,
    unmanagedJars in Compile += file(System.getProperty("java.home")).getParentFile / "lib" / "tools.jar",
    ideaExternalPlugins += IdeaPlugin.Id("Scala", "org.intellij.scala", None)
  )
