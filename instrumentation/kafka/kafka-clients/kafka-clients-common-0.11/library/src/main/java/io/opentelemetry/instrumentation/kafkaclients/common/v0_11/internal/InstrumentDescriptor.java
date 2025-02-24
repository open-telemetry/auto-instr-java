/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import com.google.auto.value.AutoValue;

/** A description of an OpenTelemetry metric instrument. */
@AutoValue
abstract class InstrumentDescriptor {

  static final String INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE = "DOUBLE_OBSERVABLE_GAUGE";
  static final String INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_COUNTER = "DOUBLE_OBSERVABLE_COUNTER";

  abstract String getName();

  abstract String getDescription();

  abstract String getInstrumentType();

  static InstrumentDescriptor createDoubleGauge(String name, String description) {
    return new AutoValue_InstrumentDescriptor(
        name, description, INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE);
  }

  static InstrumentDescriptor createDoubleCounter(String name, String description) {
    return new AutoValue_InstrumentDescriptor(
        name, description, INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_COUNTER);
  }
}
