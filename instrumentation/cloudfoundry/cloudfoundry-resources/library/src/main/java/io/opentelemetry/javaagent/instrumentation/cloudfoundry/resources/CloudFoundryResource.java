/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.cloudfoundry.resources;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

public final class CloudFoundryResource {

  private static final String ENV_VCAP_APPLICATION = "VCAP_APPLICATION";

  // copied from CloudfoundryIncubatingAttributes
  private static final AttributeKey<String> CLOUDFOUNDRY_APP_ID =
      AttributeKey.stringKey("cloudfoundry.app.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_APP_INSTANCE_ID =
      AttributeKey.stringKey("cloudfoundry.app.instance.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_APP_NAME =
      AttributeKey.stringKey("cloudfoundry.app.name");
  private static final AttributeKey<String> CLOUDFOUNDRY_ORG_ID =
      AttributeKey.stringKey("cloudfoundry.org.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_ORG_NAME =
      AttributeKey.stringKey("cloudfoundry.org.name");
  private static final AttributeKey<String> CLOUDFOUNDRY_PROCESS_ID =
      AttributeKey.stringKey("cloudfoundry.process.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_PROCESS_TYPE =
      AttributeKey.stringKey("cloudfoundry.process.type");
  private static final AttributeKey<String> CLOUDFOUNDRY_SPACE_ID =
      AttributeKey.stringKey("cloudfoundry.space.id");
  private static final AttributeKey<String> CLOUDFOUNDRY_SPACE_NAME =
      AttributeKey.stringKey("cloudfoundry.space.name");
  private static final Resource INSTANCE = buildResource(System::getenv);

  private CloudFoundryResource() {}

  public static Resource get() {
    return INSTANCE;
  }

  static Resource buildResource(Function<String, String> getenv) {
    String vcapAppRaw = getenv.apply(ENV_VCAP_APPLICATION);
    // If there is no VCAP_APPLICATION in the environment, we are likely not running in CloudFoundry
    if (vcapAppRaw == null || vcapAppRaw.isEmpty()) {
      return Resource.empty();
    }
    CloudFoundryAttributesBuilder attributes = new CloudFoundryAttributesBuilder(vcapAppRaw);
    attributes
        .putString(CLOUDFOUNDRY_APP_ID, "application_id")
        .putString(CLOUDFOUNDRY_APP_NAME, "application_name")
        .putNumberAsString(CLOUDFOUNDRY_APP_INSTANCE_ID, "instance_index")
        .putString(CLOUDFOUNDRY_ORG_ID, "organization_id")
        .putString(CLOUDFOUNDRY_ORG_NAME, "organization_name")
        .putString(CLOUDFOUNDRY_PROCESS_ID, "process_id")
        .putString(CLOUDFOUNDRY_PROCESS_TYPE, "process_type")
        .putString(CLOUDFOUNDRY_SPACE_ID, "space_id")
        .putString(CLOUDFOUNDRY_SPACE_NAME, "space_name");
    return Resource.create(attributes.build(), SchemaUrls.V1_24_0);
  }

  private static class CloudFoundryAttributesBuilder {
    private final AttributesBuilder builder = Attributes.builder();
    private final Map<String, Object> parsedData;

    @SuppressWarnings("unchecked")
    private CloudFoundryAttributesBuilder(String rawData) {
      Load load = new Load(LoadSettings.builder().build());
      try {
        this.parsedData = (Map<String, Object>) load.loadFromString(rawData);
      } catch (ClassCastException ex) {
        this.parsedData = Collections.emptyMap();
      }
    }

    @CanIgnoreReturnValue
    private CloudFoundryAttributesBuilder putString(AttributeKey<String> key, String name) {
      Object value = parsedData.get(name);
      if (value instanceof String) {
        builder.put(key, (String) value);
      }
      return this;
    }

    @CanIgnoreReturnValue
    private CloudFoundryAttributesBuilder putNumberAsString(AttributeKey<String> key, String name) {
      Object value = parsedData.get(name);
      if (value instanceof Number) {
        builder.put(key, value.toString());
      }
      return this;
    }

    private Attributes build() {
      return builder.build();
    }
  }
}
