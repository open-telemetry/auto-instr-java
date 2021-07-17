plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  fail {
    group.set("javax.ws.rs")
    module.set("jsr311-api")
    versions.set("[,]")
  }
  pass {
    group.set("javax.ws.rs")
    module.set("javax.ws.rs-api")
    versions.set("[,]")
  }
}

dependencies {
  compileOnly("javax.ws.rs:javax.ws.rs-api:2.0")
}