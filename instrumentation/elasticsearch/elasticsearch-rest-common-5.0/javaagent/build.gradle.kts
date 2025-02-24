plugins {
  id("otel.javaagent-instrumentation")
}

dependencies {
  compileOnly("org.elasticsearch.client:rest:5.0.0")

  api(project(":instrumentation:elasticsearch:elasticsearch-rest-common-5.0:library"))
}
