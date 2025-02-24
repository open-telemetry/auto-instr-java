/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.junit.v5_0;

enum TestLifecycle {
  BEFORE_ALL,
  CLASS_CONSTRUCTOR,
  TEST_CLASS,
  BEFORE_EACH,
  DISABLED,
  FACTORY_METHOD,
  TEST,
  AFTER_EACH,
  AFTER_ALL
}
