package com.intellij.plugin.livy.rest

class DefaultLivyRest(override val url: String)
  extends Http4sLivyRest
  with TimedLivyRest
  with LoggingLivyRest