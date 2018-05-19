package com.intellij.plugin.livy.runcfg

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons

class LivyRunConfigurationType extends ConfigurationType {
  override def getDisplayName = "Livy"

  override def getConfigurationTypeDescription = "Livy Run Configuration Type"

  override def getIcon= AllIcons.General.Information

  override def getId = "LIVY_RUN_CONFIGURATION"

  override def getConfigurationFactories: Array[ConfigurationFactory] = Array[ConfigurationFactory](new LivyConfigurationFactory(this))
}