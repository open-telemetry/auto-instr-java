plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.awspring.cloud")
    module.set("spring-cloud-aws-sqs")
    versions.set("(,)")
    assertInverse.set(true)
  }
}

dependencies {
  library("io.awspring.cloud:spring-cloud-aws-starter-sqs:3.0.0")
  implementation(project(":instrumentation:aws-sdk:aws-sdk-2.2:library"))

  testInstrumentation(project(":instrumentation:aws-sdk:aws-sdk-2.2:javaagent"))

  testImplementation("org.elasticmq:elasticmq-rest-sqs_2.13")

  testLibrary("org.springframework.boot:spring-boot-starter-test:3.0.0")
  testLibrary("org.springframework.boot:spring-boot-starter-web:3.0.0")
}
