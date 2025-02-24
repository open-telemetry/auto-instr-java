plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.activej:activej-http")
    module.set("activej-http")
    versions.set("[6.0,)")
    assertInverse.set(true)
  }
}

dependencies {
  library("io.activej:activej-http:6.0-rc2")
  latestDepTestLibrary("io.activej:activej-http:6.0-rc2") // documented limitation
}

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_17)
}
