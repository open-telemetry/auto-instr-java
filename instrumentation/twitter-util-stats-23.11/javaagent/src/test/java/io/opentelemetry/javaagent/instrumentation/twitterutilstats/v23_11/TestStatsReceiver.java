/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.twitterutilstats.v23_11;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.twitter.finagle.stats.Counter;
import com.twitter.finagle.stats.Gauge;
import com.twitter.finagle.stats.InMemoryStatsReceiver;
import com.twitter.finagle.stats.Metadata;
import com.twitter.finagle.stats.MetricBuilder;
import com.twitter.finagle.stats.ReadableCounter;
import com.twitter.finagle.stats.ReadableStat;
import com.twitter.finagle.stats.Stat;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.finagle.stats.StatsReceiverProxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import scala.Function0;

public class TestStatsReceiver implements StatsReceiverProxy {
  @Override
  public StatsReceiver self() {
    return IMPL;
  }

  static Impl getInstance() {
    return IMPL;
  }

  private static final Impl IMPL =
      new Impl() {
        // used to generate finagle-computed values to establish baseline for comparisons
        final InMemoryStatsReceiver self = new InMemoryStatsReceiver();

        private final Map<MetricBuilder, RichCounter> counters = new ConcurrentHashMap<>();
        private final Map<MetricBuilder, RichStat> stats = new ConcurrentHashMap<>();
        private final Map<MetricBuilder, GaugeWithMemory> gauges = new ConcurrentHashMap<>();

        @Override
        public Multimap<String, RichCounter> getCounters() {
          return counters.entrySet().stream()
              .collect(
                  ImmutableSetMultimap.toImmutableSetMultimap(
                      k -> Helpers.nameConversion(k.getKey()), Map.Entry::getValue));
        }

        @Override
        public Multimap<String, RichStat> getStats() {
          return stats.entrySet().stream()
              .collect(
                  ImmutableSetMultimap.toImmutableSetMultimap(
                      k -> Helpers.nameConversion(k.getKey()), Map.Entry::getValue));
        }

        @Override
        public Multimap<String, GaugeWithMemory> getGauges() {
          return gauges.entrySet().stream()
              .collect(
                  ImmutableSetMultimap.toImmutableSetMultimap(
                      k -> Helpers.nameConversion(k.getKey()), Map.Entry::getValue));
        }

        @Override
        public Object repr() {
          return this;
        }

        @Override
        public RichCounter counter(MetricBuilder schema) {
          return counters.computeIfAbsent(
              schema,
              key -> {
                ReadableCounter counter = self.counter(schema);
                AtomicBoolean isInitialized = new AtomicBoolean(false);
                return new RichCounter() {
                  final AtomicReference<OtelStatsReceiver.OtelCounter> counterpart =
                      new AtomicReference<>();

                  @Override
                  public void incr(long delta) {
                    isInitialized.set(true);
                    counter.incr(delta);
                  }

                  @Override
                  public Metadata metadata() {
                    return counter.metadata();
                  }

                  @Override
                  public boolean isInitialized() {
                    return isInitialized.get();
                  }

                  @Override
                  public ReadableCounter getCounter() {
                    return counter;
                  }

                  @Override
                  public OtelStatsReceiver.OtelCounter getCounterpart() {
                    // safe to cast
                    return counterpart.updateAndGet(
                        value ->
                            value == null
                                ? (OtelStatsReceiver.OtelCounter)
                                    OtelStatsReceiverProxy.getInstance().counter(schema)
                                : value);
                  }
                };
              });
        }

        @Override
        public RichStat stat(MetricBuilder schema) {
          return stats.computeIfAbsent(
              schema,
              key -> {
                ReadableStat stat = self.stat(schema);
                AtomicBoolean isInitialized = new AtomicBoolean(false);
                return new RichStat() {
                  final AtomicReference<OtelStatsReceiver.OtelStat> counterpart =
                      new AtomicReference<>();

                  @Override
                  public void add(float value) {
                    isInitialized.set(true);
                    stat.add(value);
                  }

                  @Override
                  public Metadata metadata() {
                    return stat.metadata();
                  }

                  @Override
                  public boolean isInitialized() {
                    return isInitialized.get();
                  }

                  @Override
                  public ReadableStat getStat() {
                    return stat;
                  }

                  @Override
                  public OtelStatsReceiver.OtelStat getCounterpart() {
                    // safe to cast
                    return counterpart.updateAndGet(
                        value ->
                            value == null
                                ? (OtelStatsReceiver.OtelStat)
                                    OtelStatsReceiverProxy.getInstance().stat(schema)
                                : value);
                  }
                };
              });
        }

        @Override
        public GaugeWithMemory addGauge(MetricBuilder metricBuilder, Function0<Object> f) {
          return gauges.computeIfAbsent(
              metricBuilder,
              key -> {
                AtomicReference<Float> ref = new AtomicReference<>();
                Gauge gauge =
                    self.addGauge(
                        metricBuilder,
                        () -> {
                          float value = (float) f.apply();
                          ref.getAndSet(value);
                          return value;
                        });
                return new GaugeWithMemory() {
                  final AtomicReference<OtelStatsReceiver.OtelGauge> counterpart =
                      new AtomicReference<>();

                  @Override
                  public boolean isInitialized() {
                    return ref.get() != null;
                  }

                  @Override
                  public Float getLast() {
                    return ref.get();
                  }

                  @Override
                  public OtelStatsReceiver.OtelGauge getCounterpart() {
                    // safe to cast
                    return counterpart.updateAndGet(
                        value ->
                            value == null
                                ? (OtelStatsReceiver.OtelGauge)
                                    OtelStatsReceiverProxy.getInstance().addGauge(metricBuilder, f)
                                : value);
                  }

                  @Override
                  public void remove() {
                    gauge.remove();
                  }

                  @Override
                  public Metadata metadata() {
                    return gauge.metadata();
                  }
                };
              });
        }
      };

  interface Impl extends StatsReceiver {

    Multimap<String, RichCounter> getCounters();

    Multimap<String, RichStat> getStats();

    Multimap<String, GaugeWithMemory> getGauges();
  }

  interface GaugeWithMemory extends Gauge {
    boolean isInitialized();

    Float getLast();

    OtelStatsReceiver.OtelGauge getCounterpart();
  }

  interface RichCounter extends Counter {
    boolean isInitialized();

    ReadableCounter getCounter();

    OtelStatsReceiver.OtelCounter getCounterpart();
  }

  interface RichStat extends Stat {
    boolean isInitialized();

    ReadableStat getStat();

    OtelStatsReceiver.OtelStat getCounterpart();
  }
}
