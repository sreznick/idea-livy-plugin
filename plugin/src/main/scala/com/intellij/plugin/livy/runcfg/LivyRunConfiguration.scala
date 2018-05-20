package com.intellij.plugin.livy.runcfg

import java.io._

import com.intellij.execution.configurations._
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.{ExecutionEnvironment, ProgramRunner}
import com.intellij.execution.ui.{ConsoleViewContentType, ExecutionConsole}
import com.intellij.execution.{ExecutionResult, Executor}
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.plugin.livy.LivyExecutor
import com.intellij.plugin.livy.data.ServerData.GetSessions
import com.intellij.plugin.livy.io.IOUtils
import com.intellij.plugin.livy.jar.JarUtils
import com.intellij.plugin.livy.rest.DefaultLivyRest
import com.intellij.plugin.livy.session.{RestSessionManager, Session}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionException, Future}
import scala.util.{Failure, Success}

class LivyRunConfiguration (val project: Project,
                            val factory: ConfigurationFactory,
                            val name: String) extends RunConfigurationBase(project, factory, name) {
  val livyRest = new DefaultLivyRest("http://localhost:8998")
  val sessionManager = new RestSessionManager(livyRest)

  override def getConfigurationEditor = {
    new LivySettingsEditor()
  }

  @throws[RuntimeConfigurationException]
  override def checkConfiguration(): Unit = {
  }

  @throws[ExecutionException]
  def getState(executor: Executor, env: ExecutionEnvironment): RunProfileState = {
    def findJarToRun() =
      project.getBaseDir
        .findChild("target")
        .findChild("scala-2.11")
        .getChildren
        .filter(_.getName.endsWith(".jar")).headOption

    val consoleResult = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole

    def stdOutput(s: String) = {
      consoleResult.print(s + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
    }

    def errOutput(s: String) = {
      consoleResult.print(s + "\n", ConsoleViewContentType.ERROR_OUTPUT)
    }

    val jarName = findJarToRun().map(_.getCanonicalPath)

    stdOutput(s"Starting $jarName")

    val submittableClass = JarUtils.submittableClasses(jarName.get).head

    stdOutput(s"Classname: $submittableClass")

    val sessionsF = livyRest.getSessions(GetSessions.Request())

    sessionsF.onComplete {
      case Success(resp) =>
        stdOutput("Server is ready")
        for (session <- resp.sessions) {
          stdOutput("Session: " + session.toString)
        }
      case Failure(e) =>
        errOutput(s"FAILED: $e")
    }

    val sessionF = sessionsF.flatMap(_ => sessionManager.startSession())

    sessionF.onComplete {
      case Success(res) =>
        stdOutput("Created session" + res.toString)
      case Failure(e) =>
        errOutput(s"FAILED to create session: $e")
    }

    var currentSession: Session = null

    val uploadF = sessionF flatMap {
      case session =>
        currentSession = session
        session.uploadJar(jarName.get)
    }

    uploadF onComplete {
      case Success(res) =>
        stdOutput(res.toString)
      case Failure(e) =>
        errOutput(s"FAILED to upload jar: $e")
    }

    import LivyRunConfiguration._

    val runF = uploadF flatMap {
      case _ =>
          val pb = new ProcessBuilder()
          pb.environment().put("JAVA_HOME", submitterJava)
          val p = pb
            .directory(new File(submitterHome))
            .command(submitterCmd, submitterParam(currentSession.id, submittableClass))
            .start()

          def processStreamAsync(is: InputStream, action: String => Unit) = Future {
            IOUtils.processLines(new BufferedReader(new InputStreamReader(is))) {
              case line =>
                action(line)
            }
          }

          processStreamAsync(p.getInputStream, stdOutput)

          processStreamAsync(p.getErrorStream, errOutput)

          Future(p.waitFor())
    }

    val runRecoveredF: Future[AnyVal] = runF recover {
      case e =>
        errOutput(s"Thrown $e during execution")
    }

    val closeF = runRecoveredF flatMap  {
      case _ =>
        stdOutput("Closing session...")
      //  currentSession.close()
        Future()
    }

    closeF onComplete {
      case Success(_) =>
        stdOutput("Closed")
      case Failure(e) =>
        errOutput(s"Failed to close session: $e")
    }

    new RunProfileState {
      override def execute(executor: Executor, programRunner: ProgramRunner[_ <: RunnerSettings]): ExecutionResult = {

        new ExecutionResult {
          override def getExecutionConsole: ExecutionConsole = {
            consoleResult
          }

          override def getActions: Array[AnAction] = Array()

          override def getProcessHandler: ProcessHandler = {
            new ProcessHandler {
              override def destroyProcessImpl(): Unit = {
              }

              override def detachProcessImpl(): Unit = {
              }

              override def detachIsDefault(): Boolean = {
                return true
              }

              override def getProcessInput: OutputStream = {
                null
              }
            }
          }
        }
      }
    }
  }
}

object LivyRunConfiguration {
  private val ClassName = "org.apache.livy.examples.BasicArith"
  val submitterJava = "c:\\Java\\jdk1.8.0_161"
  val submitterHome = LivyExecutor.SubmitHome
  val submitterCmd = LivyExecutor.Sbt
  def submitterParam(sessionId: Int, className: String) =
    s"runMain com.intellij.LivySubmit http://localhost:8998/sessions/${sessionId} $className"
}