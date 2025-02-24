/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import static java.lang.System.lineSeparator;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("OtelInternalJavadoc")
@Testcontainers
public abstract class AbstractOpenTelemetryMetricsReporterTest {

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractOpenTelemetryMetricsReporterTest.class);

  private static final List<String> TOPICS = Arrays.asList("foo", "bar", "baz", "qux");
  private static final Random RANDOM = new Random();

  private static KafkaContainer kafka;
  private static KafkaProducer<byte[], byte[]> producer;
  private static KafkaConsumer<byte[], byte[]> consumer;

  private static final List<OpenTelemetryMetricsReporter> metricsReporters =
      new CopyOnWriteArrayList<>();

  static {
    OpenTelemetryMetricsReporter.setListener(metricsReporters::add);
  }

  @BeforeEach
  void beforeAll() {
    // only start the kafka container the first time this runs
    if (kafka != null) {
      return;
    }

    kafka =
        new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_HEAP_OPTS", "-Xmx256m")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .waitingFor(Wait.forLogMessage(".*started \\(kafka.server.Kafka.*Server\\).*", 1))
            .withStartupTimeout(Duration.ofMinutes(1));
    kafka.start();
    producer = new KafkaProducer<>(producerConfig());
    consumer = new KafkaConsumer<>(consumerConfig());
  }

  @AfterAll
  static void afterAll() {
    producer.close();
    consumer.close();
    kafka.stop();
  }

  @AfterEach
  void tearDown() {
    for (OpenTelemetryMetricsReporter metricsReporter : metricsReporters) {
      metricsReporter.resetForTest();
    }
  }

  protected abstract InstrumentationExtension testing();

  protected abstract Map<String, ?> additionalConfig();

  protected Map<String, Object> producerConfig() {
    Map<String, Object> producerConfig = new HashMap<>();
    producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    producerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-client-id");
    producerConfig.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    producerConfig.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    producerConfig.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
    producerConfig.putAll(additionalConfig());
    producerConfig.merge(
        CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
        TestMetricsReporter.class.getName(),
        AbstractOpenTelemetryMetricsReporterTest::mergeValue);
    return producerConfig;
  }

  protected Map<String, Object> consumerConfig() {
    Map<String, Object> consumerConfig = new HashMap<>();
    consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "sample-group");
    consumerConfig.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    consumerConfig.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2);
    consumerConfig.putAll(additionalConfig());
    consumerConfig.merge(
        CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
        TestMetricsReporter.class.getName(),
        AbstractOpenTelemetryMetricsReporterTest::mergeValue);
    return consumerConfig;
  }

  @SuppressWarnings("unchecked")
  private static Object mergeValue(Object o1, Object o2) {
    List<Object> result = new MetricsReporterList<>();
    result.addAll((List<Object>) o1);
    result.add(o2);
    return result;
  }

  @Test
  void noDuplicateMetricsReporter() {
    List<MetricsReporter> producerMetricsReporters = getMetricsReporters(producer);
    assertThat(countOpenTelemetryMetricsReporters(producerMetricsReporters)).isEqualTo(1);
    List<MetricsReporter> consumerMetricsReporters = getMetricsReporters(consumer);
    assertThat(countOpenTelemetryMetricsReporters(consumerMetricsReporters)).isEqualTo(1);
  }

  private static List<MetricsReporter> getMetricsReporters(Object producerOrConsumer) {
    return getMetricsRegistry(producerOrConsumer).reporters();
  }

  private static Metrics getMetricsRegistry(Object producerOrConsumer) {
    Class<?> clazz = producerOrConsumer.getClass();
    try {
      Field field = clazz.getDeclaredField("metrics");
      field.setAccessible(true);
      return (Metrics) field.get(producerOrConsumer);
    } catch (Exception ignored) {
      // Ignore
    }
    try {
      Method method = clazz.getDeclaredMethod("metricsRegistry");
      method.setAccessible(true);
      return (Metrics) method.invoke(producerOrConsumer);
    } catch (Exception ignored) {
      // Ignore
    }
    throw new IllegalStateException(
        "Failed to get metrics registry from " + producerOrConsumer.getClass().getName());
  }

  private static long countOpenTelemetryMetricsReporters(List<MetricsReporter> metricsReporters) {
    return metricsReporters.stream()
        .filter(reporter -> reporter.getClass().getName().endsWith("OpenTelemetryMetricsReporter"))
        .count();
  }

  @Test
  void observeMetrics() {
    // Firstly create new producer and consumer and close them. This is done tp verify that metrics
    // are still produced after closing one producer/consumer. See
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/11880
    KafkaProducer<byte[], byte[]> producer2 = new KafkaProducer<>(producerConfig());
    KafkaConsumer<byte[], byte[]> consumer2 = new KafkaConsumer<>(consumerConfig());
    producer2.close();
    consumer2.close();

    produceRecords();
    consumeRecords();

    Set<String> expectedMetricNames =
        new HashSet<>(
            Arrays.asList(
                "kafka.consumer.commit_latency_avg",
                "kafka.consumer.commit_latency_max",
                "kafka.consumer.commit_rate",
                "kafka.consumer.commit_total",
                "kafka.consumer.failed_rebalance_rate_per_hour",
                "kafka.consumer.failed_rebalance_total",
                "kafka.consumer.heartbeat_rate",
                "kafka.consumer.heartbeat_response_time_max",
                "kafka.consumer.heartbeat_total",
                "kafka.consumer.join_rate",
                "kafka.consumer.join_time_avg",
                "kafka.consumer.join_time_max",
                "kafka.consumer.join_total",
                "kafka.consumer.last_heartbeat_seconds_ago",
                "kafka.consumer.last_rebalance_seconds_ago",
                "kafka.consumer.rebalance_latency_avg",
                "kafka.consumer.rebalance_latency_max",
                "kafka.consumer.rebalance_latency_total",
                "kafka.consumer.rebalance_rate_per_hour",
                "kafka.consumer.rebalance_total",
                "kafka.consumer.sync_rate",
                "kafka.consumer.sync_time_avg",
                "kafka.consumer.sync_time_max",
                "kafka.consumer.sync_total",
                "kafka.consumer.bytes_consumed_rate",
                "kafka.consumer.bytes_consumed_total",
                "kafka.consumer.fetch_latency_avg",
                "kafka.consumer.fetch_latency_max",
                "kafka.consumer.fetch_rate",
                "kafka.consumer.fetch_size_avg",
                "kafka.consumer.fetch_size_max",
                "kafka.consumer.fetch_throttle_time_avg",
                "kafka.consumer.fetch_throttle_time_max",
                "kafka.consumer.fetch_total",
                "kafka.consumer.records_consumed_rate",
                "kafka.consumer.records_consumed_total",
                "kafka.consumer.records_lag",
                "kafka.consumer.records_lag_avg",
                "kafka.consumer.records_lag_max",
                "kafka.consumer.records_lead",
                "kafka.consumer.records_lead_avg",
                "kafka.consumer.records_lead_min",
                "kafka.consumer.records_per_request_avg",
                "kafka.consumer.connection_close_rate",
                "kafka.consumer.connection_close_total",
                "kafka.consumer.connection_count",
                "kafka.consumer.connection_creation_rate",
                "kafka.consumer.connection_creation_total",
                "kafka.consumer.failed_authentication_rate",
                "kafka.consumer.failed_authentication_total",
                "kafka.consumer.failed_reauthentication_rate",
                "kafka.consumer.failed_reauthentication_total",
                "kafka.consumer.incoming_byte_rate",
                "kafka.consumer.incoming_byte_total",
                "kafka.consumer.io_ratio",
                "kafka.consumer.io_time_ns_avg",
                "kafka.consumer.io_wait_ratio",
                "kafka.consumer.io_wait_time_ns_avg",
                "kafka.consumer.io_waittime_total",
                "kafka.consumer.iotime_total",
                "kafka.consumer.last_poll_seconds_ago",
                "kafka.consumer.network_io_rate",
                "kafka.consumer.network_io_total",
                "kafka.consumer.outgoing_byte_rate",
                "kafka.consumer.outgoing_byte_total",
                "kafka.consumer.poll_idle_ratio_avg",
                "kafka.consumer.request_rate",
                "kafka.consumer.request_size_avg",
                "kafka.consumer.request_size_max",
                "kafka.consumer.request_total",
                "kafka.consumer.response_rate",
                "kafka.consumer.response_total",
                "kafka.consumer.select_rate",
                "kafka.consumer.select_total",
                "kafka.consumer.successful_authentication_no_reauth_total",
                "kafka.consumer.successful_authentication_rate",
                "kafka.consumer.successful_authentication_total",
                "kafka.consumer.successful_reauthentication_rate",
                "kafka.consumer.successful_reauthentication_total",
                "kafka.consumer.time_between_poll_avg",
                "kafka.consumer.time_between_poll_max",
                "kafka.producer.batch_size_avg",
                "kafka.producer.batch_size_max",
                "kafka.producer.batch_split_rate",
                "kafka.producer.batch_split_total",
                "kafka.producer.buffer_available_bytes",
                "kafka.producer.buffer_exhausted_rate",
                "kafka.producer.buffer_exhausted_total",
                "kafka.producer.buffer_total_bytes",
                "kafka.producer.bufferpool_wait_ratio",
                "kafka.producer.bufferpool_wait_time_total",
                "kafka.producer.compression_rate_avg",
                "kafka.producer.connection_close_rate",
                "kafka.producer.connection_close_total",
                "kafka.producer.connection_count",
                "kafka.producer.connection_creation_rate",
                "kafka.producer.connection_creation_total",
                "kafka.producer.failed_authentication_rate",
                "kafka.producer.failed_authentication_total",
                "kafka.producer.failed_reauthentication_rate",
                "kafka.producer.failed_reauthentication_total",
                "kafka.producer.incoming_byte_rate",
                "kafka.producer.incoming_byte_total",
                "kafka.producer.io_ratio",
                "kafka.producer.io_time_ns_avg",
                "kafka.producer.io_wait_ratio",
                "kafka.producer.io_wait_time_ns_avg",
                "kafka.producer.io_waittime_total",
                "kafka.producer.iotime_total",
                "kafka.producer.metadata_age",
                "kafka.producer.network_io_rate",
                "kafka.producer.network_io_total",
                "kafka.producer.outgoing_byte_rate",
                "kafka.producer.outgoing_byte_total",
                "kafka.producer.produce_throttle_time_avg",
                "kafka.producer.produce_throttle_time_max",
                "kafka.producer.record_error_rate",
                "kafka.producer.record_error_total",
                "kafka.producer.record_queue_time_avg",
                "kafka.producer.record_queue_time_max",
                "kafka.producer.record_retry_rate",
                "kafka.producer.record_retry_total",
                "kafka.producer.record_send_rate",
                "kafka.producer.record_send_total",
                "kafka.producer.record_size_avg",
                "kafka.producer.record_size_max",
                "kafka.producer.records_per_request_avg",
                "kafka.producer.request_latency_avg",
                "kafka.producer.request_latency_max",
                "kafka.producer.request_rate",
                "kafka.producer.request_size_avg",
                "kafka.producer.request_size_max",
                "kafka.producer.request_total",
                "kafka.producer.requests_in_flight",
                "kafka.producer.response_rate",
                "kafka.producer.response_total",
                "kafka.producer.select_rate",
                "kafka.producer.select_total",
                "kafka.producer.successful_authentication_no_reauth_total",
                "kafka.producer.successful_authentication_rate",
                "kafka.producer.successful_authentication_total",
                "kafka.producer.successful_reauthentication_rate",
                "kafka.producer.successful_reauthentication_total",
                "kafka.producer.waiting_threads",
                "kafka.producer.byte_rate",
                "kafka.producer.byte_total",
                "kafka.producer.compression_rate"));

    List<MetricData> metrics = testing().metrics();
    Set<String> metricNames = metrics.stream().map(MetricData::getName).collect(toSet());
    assertThat(metricNames).containsAll(expectedMetricNames);

    assertThat(metrics)
        .allSatisfy(
            metricData -> {
              Set<String> expectedKeys =
                  metricData.getData().getPoints().stream()
                      .findFirst()
                      .map(
                          point ->
                              point.getAttributes().asMap().keySet().stream()
                                  .map(AttributeKey::getKey)
                                  .collect(toSet()))
                      .orElse(Collections.emptySet());
              assertThat(metricData.getData().getPoints())
                  .extracting(PointData::getAttributes)
                  .extracting(
                      attributes ->
                          attributes.asMap().keySet().stream()
                              .map(AttributeKey::getKey)
                              .collect(toSet()))
                  .allSatisfy(attributeKeys -> assertThat(attributeKeys).isEqualTo(expectedKeys));
            });

    // Print mapping table
    printMappingTable();
  }

  private static void produceRecords() {
    for (int i = 0; i < 100; i++) {
      producer.send(
          new ProducerRecord<>(
              TOPICS.get(RANDOM.nextInt(TOPICS.size())),
              0,
              System.currentTimeMillis(),
              "key".getBytes(StandardCharsets.UTF_8),
              "value".getBytes(StandardCharsets.UTF_8)));
    }
  }

  private static void consumeRecords() {
    consumer.subscribe(TOPICS);
    Instant stopTime = Instant.now().plusSeconds(10);
    while (Instant.now().isBefore(stopTime)) {
      consumer.poll(1_000);
    }
  }

  /**
   * Print a table mapping kafka metrics to equivalent OpenTelemetry metrics, in markdown format.
   */
  private static void printMappingTable() {
    StringBuilder sb = new StringBuilder();
    // Append table headers
    sb.append(
            "| Metric Group | Metric Name | Attribute Keys | Instrument Name | Instrument Description | Instrument Type |")
        .append(lineSeparator())
        .append(
            "|--------------|-------------|----------------|-----------------|------------------------|-----------------|")
        .append(lineSeparator());
    Map<String, List<KafkaMetricId>> kafkaMetricsByGroup =
        TestMetricsReporter.seenMetrics.stream().collect(groupingBy(KafkaMetricId::getGroup));
    List<RegisteredObservable> registeredObservables =
        metricsReporters.stream()
            .flatMap(metricsReporter -> metricsReporter.getRegisteredObservables().stream())
            .collect(toList());
    // Iterate through groups in alpha order
    for (String group : kafkaMetricsByGroup.keySet().stream().sorted().collect(toList())) {
      List<KafkaMetricId> kafkaMetricIds =
          kafkaMetricsByGroup.get(group).stream()
              .sorted(
                  comparing(KafkaMetricId::getName)
                      .thenComparing(kafkaMetricId -> kafkaMetricId.getAttributeKeys().size()))
              .collect(toList());
      // Iterate through metrics in alpha order by name
      for (KafkaMetricId kafkaMetricId : kafkaMetricIds) {
        // Find first (there may be multiple) registered instrument that matches the kafkaMetricId
        Optional<InstrumentDescriptor> descriptor =
            registeredObservables.stream()
                .filter(
                    registeredObservable ->
                        KafkaMetricId.create(registeredObservable.getKafkaMetricName())
                            .equals(kafkaMetricId))
                .findFirst()
                .map(RegisteredObservable::getInstrumentDescriptor);
        if (!descriptor.isPresent()) {
          continue;
        }
        // Append table row
        sb.append(
            String.format(
                "| %s | %s | %s | %s | %s | %s |%n",
                "`" + group + "`",
                "`" + kafkaMetricId.getName() + "`",
                kafkaMetricId.getAttributeKeys().stream()
                    .map(key -> "`" + key + "`")
                    .collect(joining(",")),
                descriptor.map(i -> "`" + i.getName() + "`").orElse(""),
                descriptor.map(i -> toDescription(i)).orElse(""),
                descriptor.map(i -> "`" + i.getInstrumentType() + "`").orElse("")));
      }
    }
    logger.info("Mapping table" + System.lineSeparator() + sb);
  }

  private static String toDescription(InstrumentDescriptor instrumentDescriptor) {
    String description = instrumentDescriptor.getDescription();
    if (!description.isEmpty() && !description.endsWith(".")) {
      return description + ".";
    } else if (description.isEmpty()
        && "kafka.consumer.request_latency_avg".equals(instrumentDescriptor.getName())) {
      return "The average request latency in ms.";
    } else if (description.isEmpty()
        && "kafka.consumer.request_latency_max".equals(instrumentDescriptor.getName())) {
      return "The maximum request latency in ms.";
    }
    return description;
  }

  /**
   * This class is internal and is hence not for public use. Its APIs are unstable and can change at
   * any time.
   */
  public static class TestMetricsReporter implements MetricsReporter {

    private static final Set<KafkaMetricId> seenMetrics = new HashSet<>();

    @Override
    public void init(List<KafkaMetric> list) {
      list.forEach(this::metricChange);
    }

    @Override
    public void metricChange(KafkaMetric kafkaMetric) {
      try {
        kafkaMetric.measurable();
      } catch (IllegalStateException exception) {
        // ignore non-measurable metrics, we don't report them
        return;
      }
      seenMetrics.add(KafkaMetricId.create(kafkaMetric.metricName()));
    }

    @Override
    public void metricRemoval(KafkaMetric kafkaMetric) {}

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> map) {}
  }

  @AutoValue
  abstract static class KafkaMetricId {

    abstract String getGroup();

    abstract String getName();

    abstract Set<String> getAttributeKeys();

    static KafkaMetricId create(MetricName metricName) {
      return new AutoValue_AbstractOpenTelemetryMetricsReporterTest_KafkaMetricId(
          metricName.group(), metricName.name(), metricName.tags().keySet());
    }
  }
}
