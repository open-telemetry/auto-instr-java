plugins {
  id("otel.javaagent-instrumentation")
  id("otel.scala-conventions")
}

muzzle {
  pass {
    group.set("com.twitter")
    module.set("util-stats_2.12")
    versions.set("[23.11.0,]")
  }

  pass {
    group.set("com.twitter")
    module.set("util-stats_2.13")
    versions.set("[23.11.0,]")
  }
}

val twitterUtilVersion = "23.11.0"
val scalaVersion = "2.13.10"

val scalaMinor = Regex("""^([0-9]+\.[0-9]+)\.?.*$""").find(scalaVersion)!!.run {
  val (minorVersion) = this.destructured
  minorVersion
}

val scalified = fun(pack: String): String {
  return "${pack}_$scalaMinor"
}

dependencies {
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  library("${scalified("com.twitter:util-stats")}:$twitterUtilVersion")

  testImplementation("${scalified("com.twitter:finagle-http")}:$twitterUtilVersion")
  // get all the metric services loaded
  testImplementation("${scalified("com.twitter:finagle-stats")}:$twitterUtilVersion")

  // should wire netty contexts
//  testInstrumentation(project(":instrumentation:netty:netty-4.1:javaagent"))
}

tasks {
  test {
    jvmArgs("-Dotel.instrumentation.twitter-util-stats.stats-receiver.mode=additive")
  }
}
