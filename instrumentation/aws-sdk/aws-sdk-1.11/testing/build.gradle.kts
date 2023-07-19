plugins {
  id("otel.java-conventions")
}

val minVersion = "1.11.106"

dependencies {
  api(project(":testing-common"))

  api("com.amazonaws:aws-java-sdk-core:$minVersion")

  compileOnly("com.amazonaws:aws-java-sdk-s3:$minVersion")
  compileOnly("com.amazonaws:aws-java-sdk-rds:$minVersion")
  compileOnly("com.amazonaws:aws-java-sdk-ec2:$minVersion")
  compileOnly("com.amazonaws:aws-java-sdk-kinesis:$minVersion")
  compileOnly("com.amazonaws:aws-java-sdk-dynamodb:$minVersion")
  compileOnly("com.amazonaws:aws-java-sdk-sns:$minVersion")
  compileOnly("com.amazonaws:aws-java-sdk-sqs:$minVersion")

  // needed for SQS - using emq directly as localstack references emq v0.15.7 ie WITHOUT AWS trace header propagation
  implementation("org.elasticmq:elasticmq-rest-sqs_2.12:1.0.0")

  implementation("com.google.guava:guava")

  implementation("org.apache.groovy:groovy")
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("org.spockframework:spock-core")
}
