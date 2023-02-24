plugins {
  id("otel.javaagent-instrumentation")
}

dependencies {
  implementation(project(":instrumentation:apache-httpclient:commons:javaagent"))
  compileOnly("org.apache.httpcomponents:httpcore:4.0")
}
