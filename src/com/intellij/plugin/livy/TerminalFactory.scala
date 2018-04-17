package com.intellij.plugin.livy

import java.awt.event.{KeyEvent, KeyListener}
import java.util.concurrent.atomic.AtomicReference

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindow
import javax.swing.JTextArea
import com.intellij.ui.content.ContentFactory

class TerminalFactory extends ToolWindowFactory {
  println("FFFFFFFFFFFFFFFFFFFFFFF")

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
          if (executor.get() == null && ConsoleFactory.consoleResult.get() != null) {
            //  rough logic, to be reconsidered
            executor.set(new LivyExecutor(ConsoleFactory.consoleResult.get()))
          }

          if (executor.get() != null) {
            executor.get().console.print("> " + line + "\n", ConsoleViewContentType.NORMAL_OUTPUT)
            executor.get().execute(line)
          }
        }
      }
      override def keyReleased(e: KeyEvent): Unit = {
      }

    })

    val contentFactory = ContentFactory.SERVICE.getInstance

    val content = contentFactory.createContent(terminal, "Livy", false)

    toolWindow.getContentManager.addContent(content)
 }
}
