/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MAX_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_STOP_SEQUENCES;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TEMPERATURE;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_P;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_FINISH_REASONS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_SYSTEM;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.opentelemetry.instrumentation.awssdk.v2_2.recording.RecordingExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;
import java.net.URI;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

public abstract class AbstractAws2BedrockRuntimeTest {

  private static final String API_URL = "https://bedrock-runtime.us-east-1.amazonaws.com";

  @RegisterExtension static final RecordingExtension recording = new RecordingExtension(API_URL);

  protected abstract InstrumentationExtension getTesting();

  protected abstract ClientOverrideConfiguration.Builder createOverrideConfigurationBuilder();

  private static void configureClient(BedrockRuntimeClientBuilder builder) {
    builder
        .region(Region.US_EAST_1)
        .endpointOverride(URI.create("http://localhost:" + recording.getPort()));
    if (recording.isRecording()) {
      builder.putAuthScheme(new FixedHostAwsV4AuthScheme(API_URL));
    } else {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(AwsBasicCredentials.create("testing", "testing")));
    }
  }

  @Test
  void testConverseBasic() {
    BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder();
    builder.overrideConfiguration(createOverrideConfigurationBuilder().build());
    configureClient(builder);
    BedrockRuntimeClient client = builder.build();

    String modelId = "amazon.titan-text-lite-v1";
    ConverseResponse response =
        client.converse(
            ConverseRequest.builder()
                .modelId(modelId)
                .messages(
                    Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText("Say this is a test"))
                        .build())
                .build());

    assertThat(response.output().message().content().get(0).text())
        .isEqualTo("Hi there! How can I help you today?");

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("chat amazon.titan-text-lite-v1")
                            .hasAttributesSatisfying(
                                equalTo(
                                    GEN_AI_SYSTEM,
                                    GenAiIncubatingAttributes.GenAiSystemIncubatingValues
                                        .AWS_BEDROCK),
                                equalTo(
                                    GEN_AI_OPERATION_NAME,
                                    GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues
                                        .CHAT),
                                equalTo(GEN_AI_REQUEST_MODEL, modelId),
                                equalTo(GEN_AI_USAGE_INPUT_TOKENS, 8),
                                equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 14),
                                equalTo(
                                    GEN_AI_RESPONSE_FINISH_REASONS, Arrays.asList("end_turn")))));
  }

  @Test
  void testConverseOptions() {
    BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder();
    builder.overrideConfiguration(createOverrideConfigurationBuilder().build());
    configureClient(builder);
    BedrockRuntimeClient client = builder.build();

    String modelId = "amazon.titan-text-lite-v1";
    ConverseResponse response =
        client.converse(
            ConverseRequest.builder()
                .modelId(modelId)
                .messages(
                    Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText("Say this is a test"))
                        .build())
                .inferenceConfig(
                    InferenceConfiguration.builder()
                        .maxTokens(10)
                        .temperature(0.8f)
                        .topP(1f)
                        .stopSequences("|")
                        .build())
                .build());

    assertThat(response.output().message().content().get(0).text()).isEqualTo("This is an LLM (");

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("chat amazon.titan-text-lite-v1")
                            .hasAttributesSatisfying(
                                equalTo(
                                    GEN_AI_SYSTEM,
                                    GenAiIncubatingAttributes.GenAiSystemIncubatingValues
                                        .AWS_BEDROCK),
                                equalTo(
                                    GEN_AI_OPERATION_NAME,
                                    GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues
                                        .CHAT),
                                equalTo(GEN_AI_REQUEST_MODEL, modelId),
                                equalTo(GEN_AI_REQUEST_MAX_TOKENS, 10),
                                satisfies(
                                    GEN_AI_REQUEST_TEMPERATURE,
                                    temp -> temp.isCloseTo(0.8, within(0.0001))),
                                equalTo(GEN_AI_REQUEST_TOP_P, 1.0),
                                equalTo(GEN_AI_REQUEST_STOP_SEQUENCES, Arrays.asList("|")),
                                equalTo(GEN_AI_USAGE_INPUT_TOKENS, 8),
                                equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 10),
                                equalTo(
                                    GEN_AI_RESPONSE_FINISH_REASONS, Arrays.asList("max_tokens")))));
  }
}
