plugins {
  id("otel.javaagent-instrumentation")
}

val minVersion = "1.11.106"

// All v1.11.x versions are out of support (https://github.com/aws/aws-sdk-java#supported-minor-versions)
// but we opportunistically try to work with this minor version as well. Versions from
// 1.11.106 (released Mar 21, 2017) are expected to work (this is where the handler context became
// available from AmazonWebServiceRequest instead of only from Request; also pre-106 causes some
// weird behavior with IntelliJ in some cases).
muzzle {
  pass {
    group.set("com.amazonaws")
    module.set("aws-java-sdk-core")

    // Here we use 1.10(!).33 because at the moment, that version still passes Muzzle and we want to
    // keep the assertInverse. But feel free to upgrade the version up to $minVersion if needed.
    versions.set("[1.10.33,)")
    assertInverse.set(true)

    excludeInstrumentationName("aws-sdk-1.11-sqs")
  }

  fail {
    // Set name manually, otherwise this may cause random configuration errors due to the overlap with
    // the previous pass/assertInverse which might attempt to generate configurations with colliding
    // names.
    name.set("core-only-should-disable-sqs")
    group.set("com.amazonaws")
    module.set("aws-java-sdk-core")
    versions.set("[$minVersion,)")

    excludeInstrumentationName("aws-sdk-1.11-core")
  }

  pass {
    group.set("com.amazonaws")
    module.set("aws-java-sdk-sqs")
    versions.set("[$minVersion,)")
  }
}

dependencies {
  compileOnly("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")

  implementation(project(":instrumentation:aws-sdk:aws-sdk-1.11:library"))

  library("com.amazonaws:aws-java-sdk-core:$minVersion")

  testLibrary("com.amazonaws:aws-java-sdk-s3:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-rds:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-ec2:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-kinesis:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-dynamodb:$minVersion")
  testLibrary("com.amazonaws:aws-java-sdk-sns:$minVersion")

  testImplementation(project(":instrumentation:aws-sdk:aws-sdk-1.11:testing"))

  // Include httpclient instrumentation for testing because it is a dependency for aws-sdk.
  testInstrumentation(project(":instrumentation:apache-httpclient:apache-httpclient-4.0:javaagent"))

  // needed for kinesis:
  testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")

  // needed for SNS
  testImplementation("org.testcontainers:localstack")

  // needed by S3
  testImplementation("javax.xml.bind:jaxb-api:2.3.1")
}

testing {
  suites {
    // We test SQS separately since we have special logic for it and want to make sure the presence of
    // SQS on the classpath doesn't conflict with tests for usage of the core SDK. This only affects
    // the agent.
    val testSqs by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project(":instrumentation:aws-sdk:aws-sdk-1.11:testing"))

        implementation("com.amazonaws:aws-java-sdk-sqs:$minVersion")
      }
    }
  }
}

tasks {
  if (!(findProperty("testLatestDeps") as Boolean)) {
    check {
      dependsOn(testing.suites)
    }
  }

  test {
    systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)
  }

  withType<Test>().configureEach {
    // TODO run tests both with and without experimental span attributes
    jvmArgs("-Dotel.instrumentation.aws-sdk.experimental-span-attributes=true")
  }
}
