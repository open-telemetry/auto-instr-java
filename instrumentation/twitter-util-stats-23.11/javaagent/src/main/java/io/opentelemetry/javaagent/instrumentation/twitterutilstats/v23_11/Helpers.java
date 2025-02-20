/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.twitterutilstats.v23_11;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.twitter.finagle.stats.Bytes$;
import com.twitter.finagle.stats.CustomUnit;
import com.twitter.finagle.stats.Kilobytes$;
import com.twitter.finagle.stats.Megabytes$;
import com.twitter.finagle.stats.MetricBuilder;
import com.twitter.finagle.stats.MetricUnit;
import com.twitter.finagle.stats.Microseconds$;
import com.twitter.finagle.stats.Milliseconds$;
import com.twitter.finagle.stats.Percentage$;
import com.twitter.finagle.stats.Requests$;
import com.twitter.finagle.stats.Seconds$;
import com.twitter.finagle.stats.Unspecified$;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.javaagent.bootstrap.internal.AgentInstrumentationConfig;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import scala.jdk.CollectionConverters;

public class Helpers {

  private static final Logger logger = Logger.getLogger(Helpers.class.getName());

  private static final AttributeKey<List<String>> HIERARCHICAL_NAME_LABEL_KEY =
      AttributeKey.stringArrayKey("twitter-util-stats/hierarchical_name");

  private static final Pattern NAME_REGEX = Pattern.compile("^[A-Za-z][_.-/A-Za-z0-9]{0,254}$");

  private static final String DEFAULT_DELIM = ".";

  public static final String delimiter;

  static {
    String delim =
        AgentInstrumentationConfig.get()
            .getString(
                "otel.instrumentation.twitter-util-stats.metrics.name.delimiter", DEFAULT_DELIM);

    if (delim.isEmpty()) {
      logger.warning("Delimiter must be non-empty");
      delim = DEFAULT_DELIM;
    } else if (!delim.equals(".") && !delim.equals("/") && !delim.equals("-")) {
      logger.warning("Unsupported delimiter detected: " + delim);
      delim = DEFAULT_DELIM;
    }

    delimiter = delim;
  }

  private Helpers() {}

  public static Attributes attributesFromLabels(MetricBuilder builder) {
    return builder
        .identity()
        .labels()
        .foldLeft(
            // put the hierarchical name as a label bc hierarchical names are not cleanly mapped
            // to dimensional names via twitter stats or the final usage in finagle in a way otel
            // can concisely reason about; by adding the label, downstream systems can aggregate
            // on their own -- or not -- achieving a finer granularity without compromising
            // the general patterns applied herein, and without the complexities applied in
            // the finagle PrometheusExporter (implicitly, how the MetricsView & registries work,
            // etc.), for example
            Attributes.builder()
                .put(
                    HIERARCHICAL_NAME_LABEL_KEY,
                    scala.jdk.CollectionConverters.SeqHasAsJava(
                            builder.identity().hierarchicalName())
                        .asJava()),
            (v1, v2) -> v1.put(v2._1(), v2._2()))
        .build();
  }

  @VisibleForTesting
  public static String nameConversion(MetricBuilder schema) {
    return schema.identity().dimensionalName().mkString(delimiter);
  }

  /*
  Always use "byte" for size units to avoid precision loss.
  Why: the aggregating services treat unit scales uniformly, so this should present little issue.
   */
  // unchecked casting to efficiently handle the generic number conversion for long vs float
  @SuppressWarnings("unchecked")
  public static <T extends Number> UnitConversion<T> unitConverter(
      MetricUnit unit, MetricBuilder.MetricType metricType) {
    if (unit instanceof CustomUnit) {
      return UnitConversion.create(((CustomUnit) unit).name().toLowerCase(Locale.getDefault()));
    } else if (unit instanceof Unspecified$) {
      return UnitConversion.create();
    } else if (unit instanceof Bytes$) {
      return UnitConversion.create("byte");
    } else if (unit instanceof Kilobytes$) {
      // base-10 to base-2 translation
      if (metricType == MetricBuilder.CounterType$.MODULE$) {
        return (UnitConversion<T>) UnitConversion.create("byte", (Long x) -> (x * 1000));
      } else {
        return (UnitConversion<T>) UnitConversion.create("byte", (Float x) -> (x * 1000));
      }
    } else if (unit instanceof Megabytes$) {
      // base-10 to base-2 translation
      if (metricType == MetricBuilder.CounterType$.MODULE$) {
        return (UnitConversion<T>) UnitConversion.create("byte", (Long x) -> (x * 1000 * 1000));
      } else {
        return (UnitConversion<T>) UnitConversion.create("byte", (Float x) -> (x * 1000 * 1000));
      }
    } else if (unit instanceof Seconds$) {
      return UnitConversion.create("second");
    } else if (unit instanceof Milliseconds$) {
      return UnitConversion.create("millisecond");
    } else if (unit instanceof Microseconds$) {
      return UnitConversion.create("microsecond");
    } else if (unit instanceof Requests$) {
      return UnitConversion.create("request");
    } else if (unit instanceof Percentage$) {
      return UnitConversion.create("percent");
    } else {
      throw new IllegalArgumentException("unsupported metric unit: " + unit.toString());
    }
  }

  /*
  Identical semantics to the finagle-stats PrometheusExporter.
   */
  @VisibleForTesting
  public static boolean shouldEmit(MetricBuilder schema) {
    return schema
            .identity()
            .identityType()
            .bias(com.twitter.finagle.stats.MetricBuilder$IdentityType$HierarchicalOnly$.MODULE$)
        == com.twitter.finagle.stats.MetricBuilder$IdentityType$Full$.MODULE$;
  }

  /*
  Guard against avoidable, incorrect metric names.
  Finagle produces and emits a number of metrics which contain invalid chars and entirely free-form,
  making them hard to adapt to the otel spec, whitespace, in particular. This, along with names
  containing other identifying attributes which are unpredictable except in specific known cases
  but which are indistinguishable from other potential metrics. IOW, a metric bearing some pattern
  of identifying attributes in its name is indistinguishable from others created for a different
  purpose and in a different context and is therefore not reasonably translated into otel metrics.

  Suggestion: write another instrumentation that adapts those specific cases.
   */
  @VisibleForTesting
  public static boolean canEmit(MetricBuilder schema) {
    return CollectionConverters.SeqHasAsJava(schema.identity().dimensionalName()).asJava().stream()
        .allMatch(NAME_REGEX.asPredicate());
  }

  @AutoValue
  public abstract static class UnitConversion<T extends Number> {
    public abstract Optional<String> getUnits();

    public abstract Function<T, T> getConverter();

    static <T extends Number> UnitConversion<T> create(String units, Function<T, T> converter) {
      return new AutoValue_Helpers_UnitConversion<>(Optional.of(units), converter);
    }

    static <T extends Number> UnitConversion<T> create(String units) {
      return create(units, Function.identity());
    }

    static <T extends Number> UnitConversion<T> create() {
      return new AutoValue_Helpers_UnitConversion<>(Optional.empty(), Function.identity());
    }
  }
}
