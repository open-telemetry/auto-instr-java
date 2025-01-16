/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.common;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

public class CamundaVariableAttributeExtractor
    implements AttributesExtractor<CamundaCommonRequest, String> {

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, CamundaCommonRequest request) {

    request
        .getProcessDefinitionKey()
        .ifPresent(pdk -> attributes.put("camunda.processdefinitionkey", pdk));
    request
        .getProcessDefinitionId()
        .ifPresent(pdi -> attributes.put("camunda.processdefinitionid", pdi));
    request
        .getProcessInstanceId()
        .ifPresent(pid -> attributes.put("camunda.processinstanceid", pid));
    request.getActivityId().ifPresent(aid -> attributes.put("camunda.activityid", aid));
    request.getActivityName().ifPresent(an -> attributes.put("camunda.activityname", an));
    request.getTopicName().ifPresent(tn -> attributes.put("camunda.topicname", tn));
    request.getTopicWorkerId().ifPresent(twi -> attributes.put("camunda.topicworkerid", twi));
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      CamundaCommonRequest request,
      String response,
      Throwable error) {
    // TODO Auto-generated method stub

  }
}
