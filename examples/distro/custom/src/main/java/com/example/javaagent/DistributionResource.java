/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.TELEMETRY_SDK_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.TELEMETRY_SDK_VERSION;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public final class DistributionResource {

  private static final Resource INSTANCE = buildResource();

  private DistributionResource() {}

  public static Resource get() {
    return INSTANCE;
  }

  static Resource buildResource() {
    return Resource.create(
        Attributes.of(
            TELEMETRY_SDK_NAME,
            "opentelemetry-java-instrumentation-distro",
            TELEMETRY_SDK_VERSION,
            DistributionVersion.VERSION),
        ResourceAttributes.SCHEMA_URL);
  }
}
