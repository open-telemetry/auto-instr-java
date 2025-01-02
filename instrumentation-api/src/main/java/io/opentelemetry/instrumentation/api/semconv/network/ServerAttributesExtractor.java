/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.network;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.network.internal.AddressAndPortExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.internal.InternalServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.internal.ServerAddressAndPortExtractor;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/v1.23.0/docs/general/attributes.md#server-attributes">server
 * attributes</a>.
 *
 * @since 2.0.0
 */
public final class ServerAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  /**
   * Creates the server attributes extractor.
   *
   * @see InstrumenterBuilder#addAttributesExtractor(AttributesExtractor)
   */
  public static <REQUEST, RESPONSE> ServerAttributesExtractor<REQUEST, RESPONSE> create(
      ServerAttributesGetter<REQUEST> getter) {
    return new ServerAttributesExtractor<>(getter);
  }

  private final InternalServerAttributesExtractor<REQUEST> internalExtractor;

  ServerAttributesExtractor(ServerAttributesGetter<REQUEST> getter) {
    internalExtractor =
        new InternalServerAttributesExtractor<>(
            new ServerAddressAndPortExtractor<>(getter, AddressAndPortExtractor.noop()));
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {}

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    internalExtractor.onEnd(attributes, request);
  }
}
