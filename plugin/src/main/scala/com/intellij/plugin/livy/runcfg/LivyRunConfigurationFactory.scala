package com.intellij.plugin.livy.runcfg

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

object LivyRunConfigurationFactory {
  val FACTORY_NAME = "Livy configuration factory"
}

class LivyConfigurationFactory (val configType: ConfigurationType) extends ConfigurationFactory(configType) {
  def createTemplateConfiguration(project: Project) = new LivyRunConfiguration(project, this, "Demo")

  import LivyRunConfigurationFactory._
  override def getName: String = FACTORY_NAME
}