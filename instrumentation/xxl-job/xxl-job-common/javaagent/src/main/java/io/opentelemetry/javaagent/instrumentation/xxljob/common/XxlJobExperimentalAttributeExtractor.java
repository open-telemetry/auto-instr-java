/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.xxljob.common;

import com.xxl.job.core.glue.GlueTypeEnum;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import javax.annotation.Nullable;

class XxlJobExperimentalAttributeExtractor
    implements AttributesExtractor<XxlJobProcessRequest, Void> {

  private static final AttributeKey<String> XXL_JOB_GLUE_TYPE =
      AttributeKey.stringKey("scheduling.xxl-job.glue.type");

  @Override
  public void onStart(
      AttributesBuilder attributes,
      Context parentContext,
      XxlJobProcessRequest xxlJobProcessRequest) {
    GlueTypeEnum glueType = xxlJobProcessRequest.getGlueType();
    attributes.put(XXL_JOB_GLUE_TYPE, glueType.getDesc());
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      XxlJobProcessRequest xxlJobProcessRequest,
      @Nullable Void unused,
      @Nullable Throwable error) {}
}
