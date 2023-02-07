package io.opentelemetry.instrumentation.kafka.internal;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import java.time.Duration;

@SuppressWarnings("OtelInternalJavadoc")
public abstract class KafkaClientPropagationBaseTest extends KafkaClientBaseTest {
  private static final boolean producerPropagationEnabled = Boolean.parseBoolean(
      System.getProperty("otel.instrumentation.kafka.producer-propagation.enabled", "true"));

  @Test
  public void testClientHeaderPropagationManualConfig() throws InterruptedException {
    String message = "Testing without headers";
    producer.send(new ProducerRecord<>(SHARED_TOPIC, message));

    awaitUntilConsumerIsReady();
    // check that the message was received
    ConsumerRecords<?, ?> records = consumer.poll(Duration.ofSeconds(5).toMillis());
    assertThat(records.count()).isEqualTo(1);
    for (ConsumerRecord<?, ?> record : records) {
      assertThat(record.headers().iterator().hasNext()).isEqualTo(producerPropagationEnabled);
    }
  }
}
