/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.twitterutilstats.v23_11;

import com.google.auto.value.AutoValue;
import com.twitter.finagle.stats.Counter;
import com.twitter.finagle.stats.Gauge;
import com.twitter.finagle.stats.Metadata;
import com.twitter.finagle.stats.MetricBuilder;
import com.twitter.finagle.stats.Stat;
import com.twitter.finagle.stats.StatsReceiver;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class OtelStatsReceiver implements StatsReceiver {

  private static final Logger logger = Logger.getLogger(OtelStatsReceiver.class.getName());

  private static final Map<String, DoubleHistogram> HISTO_MAP = new ConcurrentHashMap<>();
  private static final Map<String, ObservableDoubleGauge> GAUGE_MAP = new ConcurrentHashMap<>();
  private static final Map<String, LongCounter> COUNTER_MAP = new ConcurrentHashMap<>();

  private static final Meter meter = GlobalOpenTelemetry.get().getMeter("twitter-util-stats");

  public OtelStatsReceiver() {}

  @Override
  public Object repr() {
    return this;
  }

  @Override
  public Stat stat(MetricBuilder schema) {
    Attributes attributes = Helpers.attributesFromLabels(schema);
    Helpers.UnitConversion<Float> conversion =
        Helpers.unitConverter(schema.units(), schema.metricType());

    if (!Helpers.canEmit(schema) && !Helpers.shouldEmit(schema)) {
      logger.fine("skipping stat: " + Helpers.nameConversion(schema));
      return OtelStat.createNull(schema, attributes, conversion);
    }

    logger.fine("instrumenting stat: " + Helpers.nameConversion(schema));
    return OtelStat.create(
        schema,
        attributes,
        conversion,
        HISTO_MAP.computeIfAbsent(
            // NOTE: histograms aren't native to twitter-util-stats but we're using the stat name
            Helpers.nameConversion(schema),
            name -> {
              DoubleHistogramBuilder builder =
                  meter.histogramBuilder(name).setDescription(schema.description());
              if (conversion.getUnits().isPresent()) {
                builder = builder.setUnit(conversion.getUnits().get());
              }
              return builder.build();
            }));
  }

  @Override
  public Gauge addGauge(MetricBuilder metricBuilder, scala.Function0<Object> f) {
    Attributes attributes = Helpers.attributesFromLabels(metricBuilder);
    Helpers.UnitConversion<Float> conversion =
        Helpers.unitConverter(metricBuilder.units(), metricBuilder.metricType());

    if (!Helpers.canEmit(metricBuilder) && !Helpers.shouldEmit(metricBuilder)) {
      logger.fine("skipping gauge: " + Helpers.nameConversion(metricBuilder));
      return OtelGauge.createNull(metricBuilder, attributes, conversion);
    }

    logger.fine("instrumenting gauge: " + Helpers.nameConversion(metricBuilder));
    return OtelGauge.create(
        metricBuilder,
        attributes,
        conversion,
        GAUGE_MAP.computeIfAbsent(
            Helpers.nameConversion(metricBuilder),
            name -> {
              DoubleGaugeBuilder builder =
                  meter.gaugeBuilder(name).setDescription(metricBuilder.description());
              if (conversion.getUnits().isPresent()) {
                builder = builder.setUnit(conversion.getUnits().get());
              }
              return builder.buildWithCallback(
                  odm ->
                      odm.record(conversion.getConverter().apply((float) f.apply()), attributes));
            }));
  }

  @Override
  public Counter counter(MetricBuilder schema) {
    Attributes attributes = Helpers.attributesFromLabels(schema);
    Helpers.UnitConversion<Long> conversion =
        Helpers.unitConverter(schema.units(), schema.metricType());

    if (!Helpers.canEmit(schema) && !Helpers.shouldEmit(schema)) {
      logger.fine("skipping counter: " + Helpers.nameConversion(schema));
      return OtelCounter.createNull(schema, attributes, conversion);
    }

    logger.fine("instrumenting counter: " + Helpers.nameConversion(schema));
    return OtelCounter.create(
        schema,
        attributes,
        conversion,
        COUNTER_MAP.computeIfAbsent(
            Helpers.nameConversion(schema),
            name -> {
              logger.fine("creating counter: " + Helpers.nameConversion(schema));
              LongCounterBuilder builder =
                  meter.counterBuilder(name).setDescription(schema.description());
              if (conversion.getUnits().isPresent()) {
                builder = builder.setUnit(conversion.getUnits().get());
              }
              return builder.build();
            }));
  }

  @AutoValue
  abstract static class OtelGauge implements Gauge {
    abstract MetricBuilder getSchema();

    abstract Attributes getAttributes();

    abstract Helpers.UnitConversion<Float> getConversion();

    @Nullable
    abstract ObservableDoubleGauge getGauge();

    static OtelGauge createNull(
        MetricBuilder schema, Attributes attributes, Helpers.UnitConversion<Float> conversion) {
      return new AutoValue_OtelStatsReceiver_OtelGauge(schema, attributes, conversion, null);
    }

    static OtelGauge create(
        MetricBuilder schema,
        Attributes attributes,
        Helpers.UnitConversion<Float> conversion,
        ObservableDoubleGauge gauge) {
      return new AutoValue_OtelStatsReceiver_OtelGauge(schema, attributes, conversion, gauge);
    }

    boolean isEmitted() {
      return getGauge() != null;
    }

    @Override
    public void remove() {
      if (getGauge() == null) {
        return;
      }
      getGauge().close();
    }

    @Override
    public Metadata metadata() {
      return getSchema();
    }
  }

  @AutoValue
  abstract static class OtelCounter implements Counter {
    abstract MetricBuilder getSchema();

    abstract Attributes getAttributes();

    abstract Helpers.UnitConversion<Long> getConversion();

    @Nullable
    abstract LongCounter getCounter();

    static OtelCounter createNull(
        MetricBuilder schema, Attributes attributes, Helpers.UnitConversion<Long> conversion) {
      return new AutoValue_OtelStatsReceiver_OtelCounter(schema, attributes, conversion, null);
    }

    static OtelCounter create(
        MetricBuilder schema,
        Attributes attributes,
        Helpers.UnitConversion<Long> conversion,
        LongCounter counter) {
      return new AutoValue_OtelStatsReceiver_OtelCounter(schema, attributes, conversion, counter);
    }

    boolean isEmitted() {
      return getCounter() != null;
    }

    @Override
    public void incr(long delta) {
      if (getCounter() == null) {
        return;
      }
      getCounter().add(getConversion().getConverter().apply(delta), getAttributes());
    }

    @Override
    public Metadata metadata() {
      return getSchema();
    }
  }

  @AutoValue
  abstract static class OtelStat implements Stat {
    abstract MetricBuilder getSchema();

    abstract Attributes getAttributes();

    abstract Helpers.UnitConversion<Float> getConversion();

    @Nullable
    abstract DoubleHistogram getHistogram();

    static OtelStat createNull(
        MetricBuilder schema, Attributes attributes, Helpers.UnitConversion<Float> conversion) {
      return new AutoValue_OtelStatsReceiver_OtelStat(schema, attributes, conversion, null);
    }

    static OtelStat create(
        MetricBuilder schema,
        Attributes attributes,
        Helpers.UnitConversion<Float> conversion,
        DoubleHistogram histogram) {
      return new AutoValue_OtelStatsReceiver_OtelStat(schema, attributes, conversion, histogram);
    }

    boolean isEmitted() {
      return getHistogram() != null;
    }

    @Override
    public void add(float value) {
      if (getHistogram() == null) {
        return;
      }
      getHistogram().record(getConversion().getConverter().apply(value), getAttributes());
    }

    @Override
    public Metadata metadata() {
      return getSchema();
    }
  }
}
