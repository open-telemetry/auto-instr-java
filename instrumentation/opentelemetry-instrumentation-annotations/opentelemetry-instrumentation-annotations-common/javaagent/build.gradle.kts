plugins {
  id("otel.javaagent-instrumentation")
}

dependencies {
  compileOnly(project(":instrumentation-annotations-support"))

  compileOnly(project(":javaagent-tooling"))
}
