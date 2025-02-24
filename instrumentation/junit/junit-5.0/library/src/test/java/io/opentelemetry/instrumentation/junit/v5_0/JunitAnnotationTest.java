/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.junit.v5_0;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("test-tag-for-oTel")
@JunitOpenTelemetryTracing
public class JunitAnnotationTest {

  @BeforeAll
  static void setup() {
    System.out.println("JunitAnnotationTest setup");
  }

  @AfterAll
  static void teardown() {
    System.out.println("JunitAnnotationTest teardown");
  }

  @BeforeEach
  void beforeEach() {
    System.out.println("JunitAnnotationTest beforeEach");
  }

  @AfterEach
  void afterEach() {
    System.out.println("JunitAnnotationTest afterEach");
  }

  @Test
  void shouldTest() {
    System.out.println("shouldTest");
  }

  @Test
  void shouldTest2() {
    System.out.println("shouldTest2");
    throw new RuntimeException();
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "2"})
  void shouldTest3(String value) {
    // TODO: нужно добавлять параметры в теги трейса?
    System.out.println("shouldTest3: " + value);
  }

  // TODO: добавить Disabled тесты как спан/рут спан в трейсе
}
