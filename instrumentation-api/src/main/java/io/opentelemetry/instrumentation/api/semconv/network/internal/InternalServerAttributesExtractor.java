/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.network.internal;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.ServerAttributes;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class InternalServerAttributesExtractor<REQUEST> {

  private final AddressAndPortExtractor<REQUEST> addressAndPortExtractor;

  public InternalServerAttributesExtractor(
      AddressAndPortExtractor<REQUEST> addressAndPortExtractor) {
    this.addressAndPortExtractor = addressAndPortExtractor;
  }

  public void onEnd(AttributesBuilder attributes, REQUEST request) {
    AddressAndPort serverAddressAndPort = addressAndPortExtractor.extract(request);

    if (serverAddressAndPort.address != null) {
      internalSet(attributes, ServerAttributes.SERVER_ADDRESS, serverAddressAndPort.address);

      if (serverAddressAndPort.port != null && serverAddressAndPort.port > 0) {
        internalSet(attributes, ServerAttributes.SERVER_PORT, (long) serverAddressAndPort.port);
      }
    }
  }
}
