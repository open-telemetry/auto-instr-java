plugins {
  id("otel.javaagent-instrumentation")
  id("otel.scala-conventions")
}

muzzle {
  pass {
    group.set("com.twitter")
    module.set("finagle-http_2.12")
    versions.set("[23.11.0,]")
  }

  pass {
    group.set("com.twitter")
    module.set("finagle-http_2.13")
    versions.set("[23.11.0,]")
  }
}

val finagleVersion = "23.11.0"
val scalaVersion = "2.13.10"

val scalaMinor = Regex("""^([0-9]+\.[0-9]+)\.?.*$""").find(scalaVersion)!!.run {
  val (minorVersion) = this.destructured
  minorVersion
}

val scalified = fun(pack: String): String = "${pack}_$scalaMinor"

dependencies {
  bootstrap(project(":instrumentation:executors:bootstrap"))

  library("${scalified("com.twitter:finagle-http")}:$finagleVersion")

  // should wire netty contexts
  testInstrumentation(project(":instrumentation:netty:netty-4.1:javaagent"))

  implementation(project(":instrumentation:netty:netty-4.1:javaagent"))
  implementation(project(":instrumentation:netty:netty-4.1:library"))
  implementation(project(":instrumentation:netty:netty-common-4.0:library"))
}

tasks {
  test {
    jvmArgs("-Dotel.instrumentation.http.client.emit-experimental-telemetry=true")
    jvmArgs("-Dotel.instrumentation.http.server.emit-experimental-telemetry=true")
  }
}
