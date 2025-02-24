/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.common;

import java.util.Optional;

public class CamundaCommonRequest {

  private Optional<String> processDefinitionId = Optional.empty();
  private Optional<String> processDefinitionKey = Optional.empty();
  private Optional<String> processInstanceId = Optional.empty();
  private Optional<String> businessKey = Optional.empty();
  private Optional<String> activityId = Optional.empty();
  private Optional<String> activityName = Optional.empty();
  private Optional<String> topicName = Optional.empty();
  private Optional<String> topicWorkerId = Optional.empty();

  public Optional<String> getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(Optional<String> processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public Optional<String> getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public void setProcessDefinitionKey(Optional<String> processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  public Optional<String> getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(Optional<String> processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public Optional<String> getBusinessKey() {
    return businessKey;
  }

  public void setBusinessKey(Optional<String> businessKey) {
    this.businessKey = businessKey;
  }

  public Optional<String> getActivityId() {
    return activityId;
  }

  public void setActivityId(Optional<String> activityId) {
    this.activityId = activityId;
  }

  public Optional<String> getActivityName() {
    return activityName;
  }

  public void setActivityName(Optional<String> activityName) {
    this.activityName = activityName;
  }

  public Optional<String> getTopicName() {
    return topicName;
  }

  public void setTopicName(Optional<String> topicName) {
    this.topicName = topicName;
  }

  public Optional<String> getTopicWorkerId() {
    return topicWorkerId;
  }

  public void setTopicWorkerId(Optional<String> topicWorkerId) {
    this.topicWorkerId = topicWorkerId;
  }
}
