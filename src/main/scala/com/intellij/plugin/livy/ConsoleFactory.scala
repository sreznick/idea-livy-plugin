package com.intellij.plugin.livy

import java.util.concurrent.atomic.AtomicReference

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowFactory
import javax.swing.JTextArea

class ConsoleFactory extends ToolWindowFactory {

  import com.intellij.openapi.wm.ToolWindow

  override def createToolWindowContent(project: Project, toolWindow: ToolWindow) = {
    import com.intellij.ui.content.ContentFactory
    val contentFactory = ContentFactory.SERVICE.getInstance

    val consoleResult: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole
    val contentResult = contentFactory.createContent(consoleResult.getComponent, "Result", false)
    toolWindow.getContentManager.addContent(contentResult)

    val consoleLog: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole
    val contentLog = contentFactory.createContent(consoleLog.getComponent, "Log", false)
    toolWindow.getContentManager.addContent(contentLog)

    ConsoleFactory.consoleResult.set(consoleResult)
    ConsoleFactory.consoleLog.set(consoleLog)
  }

}

object ConsoleFactory {
  val consoleResult = new AtomicReference[ConsoleView](null)
  val consoleLog = new AtomicReference[ConsoleView]()
}
