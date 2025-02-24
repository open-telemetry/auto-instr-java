plugins {
  id("otel.javaagent-instrumentation")
}

dependencies {
  bootstrap(project(":instrumentation:executors:bootstrap"))
  implementation(project(":instrumentation:servlet:servlet-3.0:javaagent"))
  bootstrap(project(":instrumentation:servlet:servlet-common:bootstrap"))
  api(project(":instrumentation:servlet:servlet-common:javaagent"))
  compileOnly("javax.servlet:javax.servlet-api:3.1.0")
  library("org.apache.sling:org.apache.sling.api:2.0.6") // first non-incubator release
}
