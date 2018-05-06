package com.intellij.plugin.livy

import java.awt.event.{KeyEvent, KeyListener}
import java.util.concurrent.atomic.AtomicReference

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.execution.console.LanguageConsoleImpl.Helper
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.{ConsoleView, ConsoleViewContentType}
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.plugin.livy.session.RestSessionManager
import com.intellij.testFramework.LightVirtualFile
import javax.swing.JTextArea
import com.intellij.ui.content.ContentFactory

class TerminalFactory extends ToolWindowFactory {
  private val terminal = new JTextArea()

  def line() = {
    val endInd = terminal.getCaretPosition - 1
    val txt = terminal.getText.take(endInd)
    val prevInd = txt.lastIndexOf('\n')
    txt.drop(prevInd + 1)
  }

  val executor: AtomicReference[LivyExecutor] = new AtomicReference[LivyExecutor](null)

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow) = {
    terminal.addKeyListener(new KeyListener {
      override def keyPressed(e: KeyEvent): Unit = {
      }
      override def keyTyped(e: KeyEvent): Unit = {
        if (e.getKeyChar.toInt == 10) {
          if (executor.get() == null && ConsoleFactory.consoleLog.get() != null) {
            //  rough logic, to be reconsidered
            executor.set(new LivyExecutor(ConsoleFactory.consoleResult.get(), ConsoleFactory.consoleLog.get()))
          }

          if (executor.get() != null) {
            executor.get().consoleResult.print("> " + line + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
            executor.get().execute(line)
          }
        }
      }
      override def keyReleased(e: KeyEvent): Unit = {
      }

    })

    val helper = new Helper(project, new LightVirtualFile("QQQ", JavaFileType.INSTANCE, "1234567890"))

    try {
      val console = new LanguageConsoleImpl(helper)
      println(console)
      println(console.isEditable)
      println(console.isConsoleEditorEnabled)
      println(console.isEnabled)
      println(console.isValid)
      println(console.isVisible)
      println(console.getVirtualFile.isWritable)
      println(helper.getDocument.isWritable)
      println(console.getPromptAttributes)

      console.setEditable(true)
      console.setPrompt(">")
      console.setEnabled(true)

      console.validate()
      println(console)
      println(terminal)

      val contentFactory = ContentFactory.SERVICE.getInstance

      val content2 = contentFactory.createContent(terminal, "Livy2", false)
      val content = contentFactory.createContent(console, "Livy", false)

      toolWindow.getContentManager.addContent(content2)
      toolWindow.getContentManager.addContent(content)
    } catch {
      case e =>
        println("thrown: " + e)
        e.printStackTrace()
        throw e
    }
 }
}
