package com.intellij.plugin.livy.runcfg

import java.awt.TextField

import com.intellij.openapi.options.{ConfigurationException, SettingsEditor}
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import javax.swing.{JComponent, JPanel, JTextField}

class LivySettingsEditor extends SettingsEditor[LivyRunConfiguration] {
  private val myPanel = null
  private var myMainClass: LabeledComponent[ComponentWithBrowseButton[_ <: JComponent]] = null

  override protected def resetEditorFrom(runConfiguration: LivyRunConfiguration): Unit = {
  }

  @throws[ConfigurationException]
  override protected def applyEditorTo(demoRunConfiguration: LivyRunConfiguration): Unit = {
  }

  override protected def createEditor: JComponent = {
    new JTextField(10)
  }

  private def createUIComponents(): Unit = {
    println("CREATE UI COMPONENTS")


    myMainClass = new LabeledComponent[ComponentWithBrowseButton[_ <: JComponent]]
    myMainClass.setComponent(new TextFieldWithBrowseButton)
  }
}