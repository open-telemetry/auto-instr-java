plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    coreJdk()
  }
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.executors.include=ExecutorInstrumentationTest\$CustomThreadPoolExecutor")
  jvmArgs("-Djava.awt.headless=true")
}
