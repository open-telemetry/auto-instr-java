/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.rpc;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.network.internal.NetworkAttributes;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

// this is temporary, see
// https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/3962#issuecomment-906606325
@SuppressWarnings("rawtypes")
final class MetricsView {

  private static final Set<AttributeKey> alwaysInclude = buildAlwaysInclude();
  private static final Set<AttributeKey> clientView = buildClientView();
  private static final Set<AttributeKey> serverView = buildServerView();
  private static final Set<AttributeKey> serverFallbackView = buildServerFallbackView();

  private static Set<AttributeKey> buildAlwaysInclude() {
    // the list of recommended metrics attributes is from
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#attributes
    Set<AttributeKey> view = new HashSet<>();
    view.add(SemanticAttributes.RPC_SYSTEM);
    view.add(SemanticAttributes.RPC_SERVICE);
    view.add(SemanticAttributes.RPC_METHOD);
    view.add(SemanticAttributes.RPC_GRPC_STATUS_CODE);
    // stable http semconv
    view.add(NetworkAttributes.NETWORK_TYPE);
    view.add(NetworkAttributes.NETWORK_TRANSPORT);
    view.add(NetworkAttributes.SERVER_ADDRESS);
    view.add(NetworkAttributes.SERVER_PORT);
    view.add(NetworkAttributes.SERVER_SOCKET_ADDRESS);
    view.add(NetworkAttributes.SERVER_SOCKET_PORT);
    return view;
  }

  private static Set<AttributeKey> buildClientView() {
    // the list of rpc client metrics attributes is from
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#attributes
    Set<AttributeKey> view = new HashSet<>(alwaysInclude);
    if (SemconvStability.emitOldHttpSemconv()) {
      view.add(SemanticAttributes.NET_PEER_NAME);
      view.add(SemanticAttributes.NET_PEER_PORT);
      view.add(SemanticAttributes.NET_TRANSPORT);
    }
    return view;
  }

  private static Set<AttributeKey> buildServerView() {
    // the list of rpc server metrics attributes is from
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#attributes
    Set<AttributeKey> view = new HashSet<>(alwaysInclude);
    if (SemconvStability.emitOldHttpSemconv()) {
      view.add(SemanticAttributes.NET_HOST_NAME);
      view.add(SemanticAttributes.NET_TRANSPORT);
    }
    return view;
  }

  private static Set<AttributeKey> buildServerFallbackView() {
    // the list of rpc server metrics attributes is from
    // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/rpc.md#attributes
    Set<AttributeKey> view = new HashSet<>(alwaysInclude);
    if (SemconvStability.emitOldHttpSemconv()) {
      view.add(SemanticAttributes.NET_SOCK_HOST_ADDR);
      view.add(SemanticAttributes.NET_TRANSPORT);
    }
    return view;
  }

  private static <T> boolean containsAttribute(
      AttributeKey<T> key, Attributes startAttributes, Attributes endAttributes) {
    return startAttributes.get(key) != null || endAttributes.get(key) != null;
  }

  static Attributes applyClientView(Attributes startAttributes, Attributes endAttributes) {
    return applyView(clientView, startAttributes, endAttributes);
  }

  static Attributes applyServerView(Attributes startAttributes, Attributes endAttributes) {
    Set<AttributeKey> fullSet = serverView;
    if (SemconvStability.emitOldHttpSemconv()
        && !containsAttribute(SemanticAttributes.NET_HOST_NAME, startAttributes, endAttributes)) {
      fullSet = serverFallbackView;
    }
    return applyView(fullSet, startAttributes, endAttributes);
  }

  static Attributes applyView(
      Set<AttributeKey> view, Attributes startAttributes, Attributes endAttributes) {
    AttributesBuilder filtered = Attributes.builder();
    applyView(filtered, startAttributes, view);
    applyView(filtered, endAttributes, view);
    return filtered.build();
  }

  @SuppressWarnings("unchecked")
  private static void applyView(
      AttributesBuilder filtered, Attributes attributes, Set<AttributeKey> view) {
    attributes.forEach(
        (BiConsumer<AttributeKey, Object>)
            (key, value) -> {
              if (view.contains(key)) {
                filtered.put(key, value);
              }
            });
  }

  private MetricsView() {}
}
