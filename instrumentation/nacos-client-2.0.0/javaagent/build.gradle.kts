plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("com.alibaba.nacos")
    module.set("nacos-client")
    versions.set("[2.0.0,)")
    skip("0.5.0", "0.6.1", "1.1.2", "1.1.4", "1.4.7")
    assertInverse.set(true)
  }
}

dependencies {
  library("com.alibaba.nacos:nacos-client:2.0.0")
  testImplementation("javax.annotation:javax.annotation-api:1.3.2")

  latestDepTestLibrary("com.alibaba.nacos:nacos-client:2.0.4+")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.nacos-client.default-enabled=true")
}
