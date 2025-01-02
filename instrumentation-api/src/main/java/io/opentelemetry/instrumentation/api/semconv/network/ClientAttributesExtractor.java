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
import io.opentelemetry.instrumentation.api.semconv.network.internal.ClientAddressAndPortExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.internal.InternalClientAttributesExtractor;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/v1.23.0/docs/general/attributes.md#client-attributes">client
 * attributes</a>.
 *
 * @since 2.0.0
 */
public final class ClientAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  /**
   * Creates the client attributes extractor.
   *
   * @see InstrumenterBuilder#addAttributesExtractor(AttributesExtractor)
   */
  public static <REQUEST, RESPONSE> ClientAttributesExtractor<REQUEST, RESPONSE> create(
      ClientAttributesGetter<REQUEST> getter) {
    return new ClientAttributesExtractor<>(getter);
  }

  private final InternalClientAttributesExtractor<REQUEST> internalExtractor;

  ClientAttributesExtractor(ClientAttributesGetter<REQUEST> getter) {
    internalExtractor =
        new InternalClientAttributesExtractor<>(
            new ClientAddressAndPortExtractor<>(getter, AddressAndPortExtractor.noop()),
            /* capturePort= */ true);
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
