plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.apache.httpcomponents.client5")
    module.set("httpclient5")
    versions.set("[5.0,)")
  }
}

dependencies {
  implementation(project(":instrumentation:apache-httpclient:commons:javaagent"))
  library("org.apache.httpcomponents.client5:httpclient5:5.0")
}
