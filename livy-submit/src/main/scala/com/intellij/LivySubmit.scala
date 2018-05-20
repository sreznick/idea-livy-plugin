package com.intellij

import java.net.URI
import java.io.FileNotFoundException
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.livy.LivyClientBuilder
import org.apache.livy.scalaapi.LivyScalaClient
import org.apache.livy.scalaapi.ScalaJobContext

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

object LivySubmit {
  def initClient(url: String): LivyScalaClient = {
    return new LivyScalaClient(new LivyClientBuilder(false).setURI(new URI(url)).build())
  }

  def main(args: Array[String]): Unit = {
    require(args.length >= 2)

    val url = args(0)

    val finishFlag = new AtomicBoolean(false)

    var client: LivyScalaClient = null
    try {
        client = initClient(url)

        uploadClasses(client, Seq(this.getClass(), client.getClass)).flatMap {
            case _ =>
                val className = args(1) 
                println(s"Submitting $className...")
                client.submit(ctx => {
                    val clazz = Class.forName(className)

                    val mainMethod = clazz.getMethods.find {
                        case method => method.getName == "main"
                    }

                    try {
                      mainMethod.get.invoke(null, ctx)
                    } catch {
                      case e: Throwable =>
                        Failure(e)
                    }
                })
        } onComplete {
            case Success(res) =>
              res match {
                case Failure(e) =>
                  e match {
                    case ite:InvocationTargetException =>
                      println("InvocationTargerException")
                      println("cause: " + ite.getCause)
                      for (ste <- ite.getCause.getStackTrace) {
                        println(ste)
                      }
                    case _ =>
                      println("other...")
                  }
                case res =>
                  println("RESULT: " + res)
              }
              finishFlag.set(true)
            case Failure(e) =>
              println(s"Unprocessed fail with $e")
        }

        while (!finishFlag.get()) {
            Thread.sleep(300)
        }
    } finally {
        if (client != null) client.stop(true)
    }
  }

  def getSourcePath(clazz: Class[_]): String = {
    val source = clazz.getProtectionDomain.getCodeSource
    if (source != null && source.getLocation.getPath != "") {
      source.getLocation.getPath
    } else {
      throw new FileNotFoundException(s"Jar containing ${clazz.getName} not found.")
    }
  }

  private def uploadClasses(client: LivyScalaClient, instances: Seq[Class[_]]) = {
    Future.sequence(instances.map(v => uploadJar(client, getSourcePath(v))))
  }

  private def uploadJar(client: LivyScalaClient, path: String) = {
    val file = new File(path)

    println(s"Uploading $file...")
    val res = client.uploadJar(file)

    res  onComplete {
      case v =>
        println("completed: " + v) 
    } 

    res
  }
}
