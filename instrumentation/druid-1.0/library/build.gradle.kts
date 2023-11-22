plugins {
  id("otel.library-instrumentation")
  id("otel.nullaway-conventions")
}

dependencies {
  library("com.alibaba:druid:1.1.22")

  testImplementation(project(":instrumentation:druid-1.0:testing"))
}
