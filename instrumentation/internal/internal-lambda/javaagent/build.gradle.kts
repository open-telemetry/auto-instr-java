plugins {
  id("otel.javaagent-instrumentation")
}

dependencies {
  compileOnly(project(":javaagent-bootstrap"))

  testImplementation(project(":javaagent-bootstrap"))
}
