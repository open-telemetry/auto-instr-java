plugins {
  id("otel.library-instrumentation")
}

dependencies {
  implementation("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")

  library("software.amazon.awssdk:aws-core:2.2.0")
  library("software.amazon.awssdk:sqs:2.2.0")
  library("software.amazon.awssdk:lambda:2.2.0")
  library("software.amazon.awssdk:sns:2.2.0")
  library("software.amazon.awssdk:aws-json-protocol:2.2.0")
  // json-utils was added in 2.17.0
  compileOnly("software.amazon.awssdk:json-utils:2.17.0")
  compileOnly(project(":muzzle")) // For @NoMuzzle

  // Don't use library to make sure base test is run with the floor version.
  // bedrock runtime is tested separately in testBedrockRuntime.
  // First release with Converse API
  compileOnly("software.amazon.awssdk:bedrockruntime:2.25.63")

  testImplementation(project(":instrumentation:aws-sdk:aws-sdk-2.2:testing"))

  testLibrary("software.amazon.awssdk:dynamodb:2.2.0")
  testLibrary("software.amazon.awssdk:ec2:2.2.0")
  testLibrary("software.amazon.awssdk:kinesis:2.2.0")
  testLibrary("software.amazon.awssdk:rds:2.2.0")
  testLibrary("software.amazon.awssdk:s3:2.2.0")
  testLibrary("software.amazon.awssdk:ses:2.2.0")
}

testing {
  suites {
    val testCoreOnly by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project())
        implementation(project(":instrumentation:aws-sdk:aws-sdk-2.2:testing"))
        compileOnly("software.amazon.awssdk:sqs:2.2.0")
        if (findProperty("testLatestDeps") as Boolean) {
          implementation("software.amazon.awssdk:aws-core:+")
          implementation("software.amazon.awssdk:aws-json-protocol:+")
          implementation("software.amazon.awssdk:dynamodb:+")
          implementation("software.amazon.awssdk:lambda:+")
        } else {
          implementation("software.amazon.awssdk:aws-core:2.2.0")
          implementation("software.amazon.awssdk:aws-json-protocol:2.2.0")
          implementation("software.amazon.awssdk:dynamodb:2.2.0")
          implementation("software.amazon.awssdk:lambda:2.2.0")
        }
      }
    }

    val testLambda by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project())
        implementation(project(":instrumentation:aws-sdk:aws-sdk-2.2:testing"))
        if (findProperty("testLatestDeps") as Boolean) {
          implementation("software.amazon.awssdk:lambda:+")
        } else {
          implementation("software.amazon.awssdk:lambda:2.17.0")
        }
      }
    }

    val testBedrockRuntime by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project())
        implementation(project(":instrumentation:aws-sdk:aws-sdk-2.2:testing"))
        if (findProperty("testLatestDeps") as Boolean) {
          implementation("software.amazon.awssdk:bedrockruntime:+")
        } else {
          implementation("software.amazon.awssdk:bedrockruntime:2.25.63")
        }
      }
    }
  }
}

tasks {
  withType<Test>().configureEach {
    // NB: If you'd like to change these, there is some cleanup work to be done, as most tests ignore this and
    // set the value directly (the "library" does not normally query it, only library-autoconfigure)
    systemProperty("otel.instrumentation.aws-sdk.experimental-span-attributes", true)
    systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)
  }

  val testStableSemconv by registering(Test::class) {
    jvmArgs("-Dotel.semconv-stability.opt-in=database")
  }

  check {
    dependsOn(testing.suites)
    dependsOn(testStableSemconv)
  }
}
