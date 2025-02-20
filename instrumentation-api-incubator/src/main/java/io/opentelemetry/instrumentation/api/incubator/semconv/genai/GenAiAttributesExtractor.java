/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai;

import static io.opentelemetry.api.common.AttributeKey.doubleKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringArrayKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Extractor of <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-spans/">GenAI
 * attributes</a>.
 *
 * <p>This class delegates to a type-specific {@link GenAiAttributesGetter} for individual attribute
 * extraction from request/response objects.
 */
public final class GenAiAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  // copied from GenAiIncubatingAttributes
  private static final AttributeKey<String> GEN_AI_OPERATION_NAME =
      stringKey("gen_ai.operation.name");
  private static final AttributeKey<List<String>> GEN_AI_REQUEST_ENCODING_FORMATS =
      stringArrayKey("gen_ai.request.encoding_formats");
  private static final AttributeKey<Double> GEN_AI_REQUEST_FREQUENCY_PENALTY =
      doubleKey("gen_ai.request.frequency_penalty");
  private static final AttributeKey<Long> GEN_AI_REQUEST_MAX_TOKENS =
      longKey("gen_ai.request.max_tokens");
  private static final AttributeKey<String> GEN_AI_REQUEST_MODEL =
      stringKey("gen_ai.request.model");
  private static final AttributeKey<Double> GEN_AI_REQUEST_PRESENCE_PENALTY =
      doubleKey("gen_ai.request.presence_penalty");
  private static final AttributeKey<Long> GEN_AI_REQUEST_SEED = longKey("gen_ai.request.seed");
  private static final AttributeKey<List<String>> GEN_AI_REQUEST_STOP_SEQUENCES =
      stringArrayKey("gen_ai.request.stop_sequences");
  private static final AttributeKey<Double> GEN_AI_REQUEST_TEMPERATURE =
      doubleKey("gen_ai.request.temperature");
  private static final AttributeKey<Double> GEN_AI_REQUEST_TOP_K =
      doubleKey("gen_ai.request.top_k");
  private static final AttributeKey<Double> GEN_AI_REQUEST_TOP_P =
      doubleKey("gen_ai.request.top_p");
  private static final AttributeKey<List<String>> GEN_AI_RESPONSE_FINISH_REASONS =
      stringArrayKey("gen_ai.response.finish_reasons");
  private static final AttributeKey<String> GEN_AI_RESPONSE_ID = stringKey("gen_ai.response.id");
  private static final AttributeKey<String> GEN_AI_RESPONSE_MODEL =
      stringKey("gen_ai.response.model");
  private static final AttributeKey<String> GEN_AI_SYSTEM = stringKey("gen_ai.system");
  private static final AttributeKey<Long> GEN_AI_USAGE_INPUT_TOKENS =
      longKey("gen_ai.usage.input_tokens");
  private static final AttributeKey<Long> GEN_AI_USAGE_OUTPUT_TOKENS =
      longKey("gen_ai.usage.output_tokens");

  /** Creates the GenAI attributes extractor. */
  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      GenAiAttributesGetter<REQUEST, RESPONSE> attributesGetter) {
    return new GenAiAttributesExtractor<>(attributesGetter);
  }

  private final GenAiAttributesGetter<REQUEST, RESPONSE> getter;

  private GenAiAttributesExtractor(GenAiAttributesGetter<REQUEST, RESPONSE> getter) {
    this.getter = getter;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    internalSet(attributes, GEN_AI_OPERATION_NAME, getter.getOperationName(request));
    internalSet(attributes, GEN_AI_SYSTEM, getter.getSystem(request));
    internalSet(attributes, GEN_AI_REQUEST_MODEL, getter.getRequestModel(request));
    internalSet(attributes, GEN_AI_REQUEST_SEED, getter.getRequestSeed(request));
    internalSet(
        attributes, GEN_AI_REQUEST_ENCODING_FORMATS, getter.getRequestEncodingFormats(request));
    internalSet(
        attributes, GEN_AI_REQUEST_FREQUENCY_PENALTY, getter.getRequestFrequencyPenalty(request));
    internalSet(attributes, GEN_AI_REQUEST_MAX_TOKENS, getter.getRequestMaxTokens(request));
    internalSet(
        attributes, GEN_AI_REQUEST_PRESENCE_PENALTY, getter.getRequestPresencePenalty(request));
    internalSet(attributes, GEN_AI_REQUEST_STOP_SEQUENCES, getter.getRequestStopSequences(request));
    internalSet(attributes, GEN_AI_REQUEST_TEMPERATURE, getter.getRequestTemperature(request));
    internalSet(attributes, GEN_AI_REQUEST_TOP_K, getter.getRequestTopK(request));
    internalSet(attributes, GEN_AI_REQUEST_TOP_P, getter.getRequestTopP(request));
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    internalSet(
        attributes,
        GEN_AI_RESPONSE_FINISH_REASONS,
        getter.getResponseFinishReasons(request, response));
    internalSet(attributes, GEN_AI_RESPONSE_ID, getter.getResponseId(request, response));
    internalSet(attributes, GEN_AI_RESPONSE_MODEL, getter.getResponseModel(request, response));
    internalSet(
        attributes, GEN_AI_USAGE_INPUT_TOKENS, getter.getUsageInputTokens(request, response));
    internalSet(
        attributes, GEN_AI_USAGE_OUTPUT_TOKENS, getter.getUsageOutputTokens(request, response));
  }
}
