plugins {
  id("otel.library-instrumentation")
}

base.archivesName.set("${base.archivesName.get()}-autoconfigure")

val minVersion = "1.11.106"

dependencies {
  implementation(project(":instrumentation:aws-sdk:aws-sdk-1.11:library"))

  library("com.amazonaws:aws-java-sdk-core:$minVersion")

  testImplementation(project(":instrumentation:aws-sdk:aws-sdk-1.11:testing"))

  testLibrary("com.amazonaws:aws-java-sdk-s3:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-rds:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-ec2:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-kinesis:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-dynamodb:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-sns:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-sqs:$minVersion")
}

tasks.test {
  systemProperty("otel.instrumentation.aws-sdk.experimental-span-attributes", "true")
}
