/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.TreeTraverser;
import io.opentelemetry.api.common.AttributeConsumer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.testing.common.AgentTestingExporterAccess;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryExporter implements SpanProcessor {

  private static final Logger log = LoggerFactory.getLogger(InMemoryExporter.class);

  // not using span startEpochNanos since that is not strictly increasing so can lead to ties
  private final Map<String, Integer> spanOrders = new ConcurrentHashMap<>();
  private final AtomicInteger nextSpanOrder = new AtomicInteger();

  private volatile boolean forceFlushCalled;

  @Override
  public void onStart(Context context, ReadWriteSpan readWriteSpan) {
    SpanData sd = readWriteSpan.toSpanData();
    log.debug(
        ">>>{} SPAN START: {} id={} traceid={} parent={}, library={}",
        sd.getStartEpochNanos(),
        sd.getName(),
        sd.getSpanId(),
        sd.getTraceId(),
        sd.getParentSpanId(),
        sd.getInstrumentationLibraryInfo());
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan readableSpan) {}

  private String printSpanAttributes(SpanData sd) {
    final StringBuilder attributes = new StringBuilder();
    sd.getAttributes()
        .forEach(
            new AttributeConsumer() {
              @Override
              public <T> void accept(AttributeKey<T> key, T value) {
                attributes.append(String.format("Attribute %s=%s", key, value));
              }
            });
    return attributes.toString();
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  public List<List<SpanData>> getTraces() {
    List<SpanData> spans = AgentTestingExporterAccess.getExportedSpans();
    return groupTraces(spans);
  }

  public static List<List<SpanData>> groupTraces(List<SpanData> spans) {
    List<List<SpanData>> traces =
        new ArrayList<>(
            spans.stream().collect(Collectors.groupingBy(SpanData::getTraceId)).values());
    sortTraces(traces);
    for (int i = 0; i < traces.size(); i++) {
      List<SpanData> trace = traces.get(i);
      traces.set(i, sort(trace));
    }
    return traces;
  }

  public void waitForTraces(int number) throws InterruptedException, TimeoutException {
    waitForTraces(number, Predicates.<List<SpanData>>alwaysFalse());
  }

  public List<List<SpanData>> waitForTraces(int number, Predicate<List<SpanData>> excludes)
      throws InterruptedException, TimeoutException {
    // Wait for returned spans to stabilize.
    int previousNumSpans = -1;
    for (int attempt = 0; attempt < 2000; attempt++) {
      int numSpans = AgentTestingExporterAccess.getExportedSpans().size();
      if (numSpans != 0 && numSpans == previousNumSpans) {
        break;
      }
      previousNumSpans = numSpans;
      Thread.sleep(10);
    }
    List<List<SpanData>> traces = getFilteredTraces(excludes);
    if (traces.size() < number) {
      throw new TimeoutException(
          "Timeout waiting for "
              + number
              + " completed/filtered trace(s), found "
              + traces.size()
              + " completed/filtered trace(s) and "
              + traces.size()
              + " total trace(s): "
              + traces);
    }
    return traces;
  }

  private List<List<SpanData>> getFilteredTraces(Predicate<List<SpanData>> excludes) {
    List<List<SpanData>> traces = new ArrayList<>();
    for (List<SpanData> trace : getTraces()) {
      if (!excludes.apply(trace)) {
        traces.add(trace);
      }
    }
    return traces;
  }

  public void clear() {
    AgentTestingExporterAccess.reset();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode forceFlush() {
    forceFlushCalled = true;
    return CompletableResultCode.ofSuccess();
  }

  public boolean forceFlushCalled() {
    return forceFlushCalled;
  }

  // must be called under tracesLock
  private static void sortTraces(List<List<SpanData>> traces) {
    Collections.sort(traces, Comparator.comparingLong(InMemoryExporter::getMinSpanOrder));
  }

  private static long getMinSpanOrder(List<SpanData> spans) {
    return spans.stream().mapToLong(SpanData::getStartEpochNanos).min().orElse(0);
  }

  private static List<SpanData> sort(List<SpanData> trace) {

    Map<String, Node> lookup = new HashMap<>();
    for (SpanData span : trace) {
      lookup.put(span.getSpanId(), new Node(span));
    }

    for (Node node : lookup.values()) {
      String parentSpanId = node.span.getParentSpanId();
      if (SpanId.isValid(parentSpanId)) {
        Node parentNode = lookup.get(parentSpanId);
        if (parentNode != null) {
          parentNode.childNodes.add(node);
          node.root = false;
        }
      }
    }

    List<Node> rootNodes = new ArrayList<>();
    for (Node node : lookup.values()) {
      sortOneLevel(node.childNodes);
      if (node.root) {
        rootNodes.add(node);
      }
    }
    sortOneLevel(rootNodes);

    TreeTraverser<Node> traverser =
        new TreeTraverser<Node>() {
          @Override
          public Iterable<Node> children(Node node) {
            return node.childNodes;
          }
        };

    List<Node> orderedNodes = new ArrayList<>();
    for (Node rootNode : rootNodes) {
      Iterables.addAll(orderedNodes, traverser.preOrderTraversal(rootNode));
    }

    List<SpanData> orderedSpans = new ArrayList<>();
    for (Node node : orderedNodes) {
      orderedSpans.add(node.span);
    }
    return orderedSpans;
  }

  private static void sortOneLevel(List<Node> nodes) {
    Collections.sort(nodes, Comparator.comparingLong(node -> node.span.getStartEpochNanos()));
  }

  private static class Node {

    private final SpanData span;
    private final List<Node> childNodes = new ArrayList<>();
    private boolean root = true;

    private Node(SpanData span) {
      this.span = span;
    }
  }
}
