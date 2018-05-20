package com.intellij.plugin.livy.jar

import java.io.{ByteArrayOutputStream, InputStream}
import java.util.jar.JarFile

import org.apache.commons.io.IOUtils
import org.objectweb.asm.tree.{ClassNode, MethodNode}
import org.objectweb.asm.{ClassReader, Opcodes}

import scala.collection.JavaConversions._

object JarUtils {
  case class Entry(name: String)

  private def staticMethods(is: InputStream): Seq[MethodNode] = {
    var baos: ByteArrayOutputStream = null
    val reader = try {
      baos = new ByteArrayOutputStream()

      IOUtils.copy(is, baos)

      new ClassReader(baos.toByteArray)
    } finally {
      IOUtils.closeQuietly(baos)
    }

    val classNode = new ClassNode()
    reader.accept(classNode, 0)

    val flags = Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC
    classNode.methods.map(_.asInstanceOf[MethodNode]).filter(m => (m.access & flags) == flags)
  }

  val MainParameterClass = "org.apache.livy.scalaapi.ScalaJobContext"

  def sigClassName(className: String) = s"L${className.replace('.', '/')};"

  def javaClassName(fileName: String) = {
    val ext = ".class"
    println(s"fileName: $fileName, ext: ")
    val noExt = if (fileName.endsWith(ext)) {
      fileName.substring(0, fileName.size - ext.size)
    } else fileName

    noExt.replace('/', '.')
  }

  def parseDesc(desc: String): (Seq[String], String) = {
    val cpInd = desc.indexOf(')')
    val params = desc.substring(1, cpInd)
    (params.split(",").toSeq, desc.substring(cpInd + 1))
  }

  def submittableClasses(path: String): Seq[String] = {
    val jarFile = new JarFile(path)

    val classEntries = jarFile.entries().toSeq.filter(_.getName.endsWith(".class"))

    val res = for (ce <- classEntries) yield {
      var is: InputStream = null
      val statics = try {
        is = jarFile.getInputStream(ce)
        staticMethods(is)
      } finally {
        IOUtils.closeQuietly(is)
      }


      val parsed = statics.map(m => (m.name, parseDesc(m.desc)))
      val matched = parsed.filter {
        case pm =>
          val params = pm._2._1
          params.size == 1 && params(0) == sigClassName(MainParameterClass) && pm._1 == "main"
      }

      if (matched.size >= 1) Some(ce.getName) else None
    }

    res.flatten.toList.map(javaClassName)
  }
}
