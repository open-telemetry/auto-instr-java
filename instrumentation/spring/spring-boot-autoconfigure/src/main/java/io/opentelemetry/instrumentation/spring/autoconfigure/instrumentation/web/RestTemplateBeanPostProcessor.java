/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.instrumentation.web;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

final class RestTemplateBeanPostProcessor implements BeanPostProcessor {

  private final ObjectProvider<OpenTelemetry> openTelemetryProvider;

  RestTemplateBeanPostProcessor(ObjectProvider<OpenTelemetry> openTelemetryProvider) {
    this.openTelemetryProvider = openTelemetryProvider;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (!(bean instanceof RestTemplate)) {
      return bean;
    }

    return addRestTemplateInterceptorIfNotPresent(
        (RestTemplate) bean, openTelemetryProvider.getObject());
  }

  @CanIgnoreReturnValue
  static RestTemplate addRestTemplateInterceptorIfNotPresent(
      RestTemplate restTemplate, OpenTelemetry openTelemetry) {
    ClientHttpRequestInterceptor instrumentationInterceptor =
        SpringWebTelemetry.create(openTelemetry).newInterceptor();

    List<ClientHttpRequestInterceptor> restTemplateInterceptors = restTemplate.getInterceptors();
    if (restTemplateInterceptors.stream()
        .noneMatch(
            interceptor -> interceptor.getClass() == instrumentationInterceptor.getClass())) {
      restTemplateInterceptors.add(0, instrumentationInterceptor);
    }
    return restTemplate;
  }
}
