/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * List implementation that can be used to hold metrics reporters in kafka configuration without
 * breaking serialization. When this list is serialized it removes OpenTelemetryMetricsReporter to
 * ensure that the configuration can be deserialized even when the instrumentation is not present.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class MetricsReporterList<T> extends ArrayList<T> {
  private static final long serialVersionUID = 1L;

  public static <T> List<T> singletonList(T o) {
    List<T> list = new MetricsReporterList<>();
    list.add(o);
    return list;
  }

  private Object writeReplace() {
    // serialize to plain ArrayList that does not contain OpenTelemetryMetricsReporter
    List<Object> result = new ArrayList<>();
    this.stream().filter(x -> x != OpenTelemetryMetricsReporter.class).forEach(result::add);
    return result;
  }
}
