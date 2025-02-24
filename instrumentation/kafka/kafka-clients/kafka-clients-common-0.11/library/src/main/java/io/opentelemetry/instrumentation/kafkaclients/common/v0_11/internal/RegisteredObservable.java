/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import org.apache.kafka.common.MetricName;

@AutoValue
abstract class RegisteredObservable {

  abstract MetricName getKafkaMetricName();

  abstract InstrumentDescriptor getInstrumentDescriptor();

  abstract Attributes getAttributes();

  abstract AutoCloseable getObservable();

  static RegisteredObservable create(
      MetricName metricName,
      InstrumentDescriptor instrumentDescriptor,
      Attributes attributes,
      AutoCloseable observable) {
    return new AutoValue_RegisteredObservable(
        metricName, instrumentDescriptor, attributes, observable);
  }
}
