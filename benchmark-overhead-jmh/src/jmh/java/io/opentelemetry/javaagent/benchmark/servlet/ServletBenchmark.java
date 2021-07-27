/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.benchmark.servlet;

import io.opentelemetry.javaagent.benchmark.servlet.app.HelloWorldApplication;
import io.opentelemetry.javaagent.benchmark.util.HttpClient;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class ServletBenchmark {

  static {
    // using static initializer instead of @Setup since only want to initialize the app under test
    // once regardless of @State and @Threads
    HelloWorldApplication.main();
  }

  private HttpClient client;

  @Setup
  public void setup() throws IOException {
    client = new HttpClient("localhost", 8080, "/");
  }

  @TearDown
  public void tearDown() throws IOException {
    HelloWorldApplication.stop();
    client.close();
  }

  @Benchmark
  public void execute() throws IOException {
    client.execute();
  }
}
