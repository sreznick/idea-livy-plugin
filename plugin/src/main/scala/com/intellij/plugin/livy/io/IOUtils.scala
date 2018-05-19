package com.intellij.plugin.livy.io

import java.io.BufferedReader

import scala.annotation.tailrec

object IOUtils {
  def processLines(reader: BufferedReader)(action: String => Unit): Unit = {
    @tailrec
    def go(): Unit = {
      Option(reader.readLine()) match {
        case Some(line) =>
          action(line)
          go()
        case None =>
      }
    }

    go()
  }
}
