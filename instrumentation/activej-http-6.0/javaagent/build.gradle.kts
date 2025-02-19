plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.activej:activej-http")
    module.set("activej-http")

    versions.set("[6.0,)")
  }
}

dependencies {
  constraints {
    implementation("io.activej:activej-http:6.0-rc2")
  }
  implementation("io.activej:activej-http")
}
