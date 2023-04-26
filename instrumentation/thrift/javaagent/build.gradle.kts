plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.apache.thrift")
    module.set("apache-thrift")
    versions.set("0.14.1")
  }
}

dependencies {
  implementation("org.apache.thrift:libthrift:0.14.1")
}


