/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.net;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.internal.FallbackNamePortGetter;
import io.opentelemetry.instrumentation.api.instrumenter.net.internal.InternalNetClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.network.internal.InternalNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.network.internal.InternalServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.network.internal.NetworkTransportFilter;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/span-general.md#general-network-connection-attributes">Network
 * attributes</a>. It is common to have access to {@link java.net.InetSocketAddress}, in which case
 * it is more convenient to use {@link InetSocketAddressNetClientAttributesGetter}.
 *
 * <p>This class delegates to a type-specific {@link NetClientAttributesGetter} for individual
 * attribute extraction from request/response objects.
 */
public final class NetClientAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  private final InternalNetClientAttributesExtractor<REQUEST, RESPONSE> internalExtractor;
  private final InternalNetworkAttributesExtractor<REQUEST, RESPONSE> internalNetworkExtractor;
  private final InternalServerAttributesExtractor<REQUEST, RESPONSE> internalServerExtractor;

  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      NetClientAttributesGetter<REQUEST, RESPONSE> getter) {
    return new NetClientAttributesExtractor<>(getter);
  }

  private NetClientAttributesExtractor(NetClientAttributesGetter<REQUEST, RESPONSE> getter) {
    internalExtractor =
        new InternalNetClientAttributesExtractor<>(
            getter, FallbackNamePortGetter.noop(), SemconvStability.emitOldHttpSemconv());
    internalNetworkExtractor =
        new InternalNetworkAttributesExtractor<>(
            getter,
            NetworkTransportFilter.alwaysTrue(),
            SemconvStability.emitStableHttpSemconv(),
            SemconvStability.emitOldHttpSemconv());
    internalServerExtractor =
        new InternalServerAttributesExtractor<>(
            getter,
            (port, request) -> true,
            FallbackNamePortGetter.noop(),
            SemconvStability.emitStableHttpSemconv(),
            SemconvStability.emitOldHttpSemconv(),
            InternalServerAttributesExtractor.Mode.PEER);
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    internalServerExtractor.onStart(attributes, request);
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    internalExtractor.onEnd(attributes, request, response);
    internalNetworkExtractor.onEnd(attributes, request, response);
    internalServerExtractor.onEnd(attributes, request, response);
  }
}
