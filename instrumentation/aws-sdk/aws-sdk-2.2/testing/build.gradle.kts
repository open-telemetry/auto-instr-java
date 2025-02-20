plugins {
  id("otel.java-conventions")
}

dependencies {
  api(project(":testing-common"))

  api("software.amazon.awssdk:apache-client:2.2.0")
  // older versions don't play nice with armeria http server
  api("software.amazon.awssdk:netty-nio-client:2.11.0")

  // compileOnly because we never want to pin the low version implicitly; need to add dependencies
  // explicitly in user projects, e.g. using testLatestDeps.
  compileOnly("software.amazon.awssdk:bedrockruntime:2.25.63")
  compileOnly("software.amazon.awssdk:dynamodb:2.2.0")
  compileOnly("software.amazon.awssdk:ec2:2.2.0")
  compileOnly("software.amazon.awssdk:kinesis:2.2.0")
  compileOnly("software.amazon.awssdk:lambda:2.2.0")
  compileOnly("software.amazon.awssdk:rds:2.2.0")
  compileOnly("software.amazon.awssdk:s3:2.2.0")
  compileOnly("software.amazon.awssdk:sqs:2.2.0")
  compileOnly("software.amazon.awssdk:sns:2.2.0")
  compileOnly("software.amazon.awssdk:ses:2.2.0")

  // needed for SQS - using emq directly as localstack references emq v0.15.7 ie WITHOUT AWS trace header propagation
  implementation("org.elasticmq:elasticmq-rest-sqs_2.13")

  // used to record LLM responses in bedrock tests
  implementation("com.github.tomakehurst:wiremock-jre8:2.35.2")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")

  implementation("com.google.guava:guava")

  implementation("io.opentelemetry:opentelemetry-api")
}
