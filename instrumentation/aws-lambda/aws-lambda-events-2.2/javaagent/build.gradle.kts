plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("com.amazonaws")
    module.set("aws-lambda-java-core")
    versions.set("[1.0.0,)")
    extraDependency("com.amazonaws:aws-lambda-java-events:3.9.0")
    extraDependency("com.amazonaws.serverless:aws-serverless-java-container-core:1.5.2")
  }
}

dependencies {
  compileOnly(project(":javaagent-bootstrap"))

  implementation(project(":instrumentation:aws-lambda:aws-lambda-core-1.0:library"))

  implementation(project(":instrumentation:aws-lambda:aws-lambda-events-2.2:library")) {
    // Only needed by wrappers, not the javaagent. Muzzle will catch if we accidentally change this.
    exclude("com.fasterxml.jackson.core", "jackson-databind")
  }

  library("com.amazonaws:aws-lambda-java-core:1.0.0")
  // First version to includes support for SQSEvent, currently the most popular message queue used
  // with lambda.
  // NB: 2.2.0 includes a class called SQSEvent but isn't usable due to it returning private classes
  // in public API.
  library("com.amazonaws:aws-lambda-java-events:2.2.1")

  testImplementation(project(":instrumentation:aws-lambda:aws-lambda-events-2.2:testing"))
  testInstrumentation(project(":instrumentation:aws-lambda:aws-lambda-core-1.0:javaagent"))
}
