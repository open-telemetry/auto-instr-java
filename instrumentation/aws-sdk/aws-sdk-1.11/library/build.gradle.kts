plugins {
  id("otel.library-instrumentation")
}

val minVersion = "1.11.106"

dependencies {
  implementation("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")

  library("com.amazonaws:aws-java-sdk-core:$minVersion")
  library("com.amazonaws:aws-java-sdk-sqs:$minVersion")
  compileOnly(project(":muzzle"))

  testImplementation(project(":instrumentation:aws-sdk:aws-sdk-1.11:testing"))

  testLibrary("com.amazonaws:aws-java-sdk-s3:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-rds:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-ec2:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-kinesis:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-dynamodb:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-sns:$minVersion")
}
