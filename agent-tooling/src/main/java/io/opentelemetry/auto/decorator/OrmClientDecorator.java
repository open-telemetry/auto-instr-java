/*
 * Copyright 2020, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentelemetry.auto.decorator;

import io.opentelemetry.auto.instrumentation.api.MoreTags;
import io.opentelemetry.trace.Span;

public abstract class OrmClientDecorator extends DatabaseClientDecorator {

  public abstract String entityName(final Object entity);

  public Span onOperation(final Span span, final Object entity) {

    assert span != null;
    if (entity != null) {
      final String name = entityName(entity);
      if (name != null) {
        span.setAttribute(MoreTags.RESOURCE_NAME, name);
      } // else we keep any existing resource.
    }
    return span;
  }
}
