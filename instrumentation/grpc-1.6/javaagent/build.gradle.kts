plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.grpc")
    module.set("grpc-core")
    versions.set("[1.6.0,)")
    assertInverse.set(true)
    extraDependency("io.grpc:grpc-protobuf:1.5.0")
  }
}

val grpcVersion = "1.6.0"

dependencies {
  implementation(project(":instrumentation:grpc-1.6:library"))

  library("io.grpc:grpc-core:$grpcVersion")

  testInstrumentation(project(":instrumentation:netty:netty-4.1:javaagent"))

  testLibrary("io.grpc:grpc-netty:$grpcVersion")
  library("io.grpc:grpc-protobuf:$grpcVersion")
  testLibrary("io.grpc:grpc-services:$grpcVersion")
  testLibrary("io.grpc:grpc-stub:$grpcVersion")

  testImplementation(project(":instrumentation:grpc-1.6:testing"))
}

tasks {
  test {
    systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)
    // The agent context debug mechanism isn't compatible with the bridge approach which may add a
    // gRPC context to the root.
    jvmArgs("-Dotel.javaagent.experimental.thread-propagation-debugger.enabled=false")
    jvmArgs("-Dotel.instrumentation.grpc.capture-metadata.client.request=some-client-key")
    jvmArgs("-Dotel.instrumentation.grpc.capture-metadata.server.request=some-server-key")
    jvmArgs("-Dotel.instrumentation.common.experimental.controller-telemetry.enabled=true")
    // latest dep test occasionally fails because network type is ipv6 instead of the expected ipv4
    // and peer address is 0:0:0:0:0:0:0:1 instead of 127.0.0.1
    jvmArgs("-Djava.net.preferIPv4Stack=true")

    // exclude our grpc library instrumentation, the ContextStorageOverride contained within it
    // breaks the tests
    classpath = classpath.filter {
      !it.absolutePath.contains("opentelemetry-grpc-1.6")
    }
  }
}

if (!(findProperty("testLatestDeps") as Boolean)) {
  configurations.testRuntimeClasspath {
    resolutionStrategy {
      eachDependency {
        // early versions of grpc are not compatible with netty 4.1.101.Final
        if (requested.group == "io.netty") {
          useVersion("4.1.100.Final")
        }
      }
    }
  }
}
