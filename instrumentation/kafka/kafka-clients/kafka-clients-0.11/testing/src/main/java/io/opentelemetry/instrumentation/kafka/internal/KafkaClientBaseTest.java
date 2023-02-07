package io.opentelemetry.instrumentation.kafka.internal;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("OtelInternalJavadoc")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract public class KafkaClientBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(KafkaClientBaseTest.class);

  protected static final String SHARED_TOPIC = "shared.topic";


  public static KafkaContainer kafka;
  public static Producer<Integer, String> producer;
  public static Consumer<Integer, String> consumer;
  public CountDownLatch consumerReady = new CountDownLatch(1);

  public static final int partition = 0;
  public static TopicPartition topicPartition = new TopicPartition(SHARED_TOPIC, partition);

  @BeforeAll
  public void setupClass()
      throws ExecutionException, InterruptedException, TimeoutException {
    kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))
        .withLogConsumer(new Slf4jLogConsumer(logger))
        .waitingFor(Wait.forLogMessage(".*started \\(kafka.server.KafkaServer\\).*", 1))
        .withStartupTimeout(Duration.ofMinutes(1));
    kafka.start();

    // create test topic
    HashMap<String, Object> adminProps = new HashMap<>();
    adminProps.put("bootstrap.servers", kafka.getBootstrapServers());

    try (AdminClient admin = AdminClient.create(adminProps)) {
      admin.createTopics(Collections.singletonList(new NewTopic(SHARED_TOPIC, 1, (short) 1))).all().get(30, TimeUnit.SECONDS);
    }

    producer = new KafkaProducer<>(producerProps());

    consumer = new KafkaConsumer<>(consumerProps());

    consumer.subscribe(Collections.singletonList(SHARED_TOPIC), new ConsumerRebalanceListener() {
      @Override
      public void onPartitionsRevoked(Collection<TopicPartition> collection) {
      }

      @Override
      public void onPartitionsAssigned(Collection<TopicPartition> collection) {
        consumerReady.countDown();
      }
    });
  }

  public HashMap<String, Object> consumerProps() {
    HashMap<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", kafka.getBootstrapServers());
    props.put("group.id", "test");
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms",10);
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", IntegerDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    return props;
  }

  public HashMap<String, Object> producerProps() {
    HashMap<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", kafka.getBootstrapServers());
    props.put("retries", 0);
    props.put("batch.size", "16384");
    props.put("linger.ms", 1);
    props.put("buffer.memory", "33554432");
    props.put("key.serializer", IntegerSerializer.class.getName());
    props.put("value.serializer",StringSerializer.class.getName());
    return props;
  }

  @AfterAll
  public static void cleanupClass() {
    if (producer != null) {
      producer.close();
    }
    if (consumer != null) {
      consumer.close();
    }
    kafka.stop();
  }

  public void awaitUntilConsumerIsReady() throws InterruptedException {
    if (consumerReady.await(0, TimeUnit.SECONDS)) {
      return;
    }
    for (int i = 0; i < 10; i++ ) {
      consumer.poll(0);
      if (consumerReady.await(1, TimeUnit.SECONDS)) {
        break;
      }
    }
    if (consumerReady.getCount() != 0) {
      throw new AssertionError("Consumer wasn't assigned any partitions!");
    }
    consumer.seekToBeginning(Collections.emptyList());
  }
}
