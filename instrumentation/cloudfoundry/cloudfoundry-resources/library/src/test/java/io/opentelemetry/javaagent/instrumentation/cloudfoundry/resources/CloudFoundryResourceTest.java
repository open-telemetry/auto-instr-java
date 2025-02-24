/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.cloudfoundry.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.SchemaUrls;
import io.opentelemetry.semconv.incubating.CloudfoundryIncubatingAttributes;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CloudFoundryResourceTest {

  private static final String FULL_VCAP_APPLICATION =
      "{"
          + "\"application_id\":\"0193a038-e615-7e5e-92ca-f4bcd7ba0a25\","
          + "\"application_name\":\"cf-app-name\","
          + "\"instance_index\":1,"
          + "\"organization_id\":\"0193a375-8d8e-7e0c-a832-01ce9ded40dc\","
          + "\"organization_name\":\"cf-org-name\","
          + "\"process_id\":\"0193a4e3-8fd3-71b9-9fe3-5640c53bf1e2\","
          + "\"process_type\":\"web\","
          + "\"space_id\":\"0193a7e7-da17-7ea4-8940-b1e07b401b16\","
          + "\"space_name\":\"cf-space-name\"}";

  @Test
  void noVcapApplication() {
    Map<String, String> env = Collections.emptyMap();
    Resource resource = CloudFoundryResource.buildResource(env::get);
    assertThat(resource).isEqualTo(Resource.empty());
  }

  @Test
  void emptyVcapApplication() {
    Map<String, String> env = ImmutableMap.of("VCAP_APPLICATION", "");
    Resource resource = CloudFoundryResource.buildResource(env::get);
    assertThat(resource).isEqualTo(Resource.empty());
  }

  @Test
  void fullVcapApplication() {
    Map<String, String> env = ImmutableMap.of("VCAP_APPLICATION", FULL_VCAP_APPLICATION);

    Resource resource = CloudFoundryResource.buildResource(env::get);

    assertThat(resource.getSchemaUrl()).isEqualTo(SchemaUrls.V1_24_0);
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_APP_ID))
        .isEqualTo("0193a038-e615-7e5e-92ca-f4bcd7ba0a25");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_APP_INSTANCE_ID))
        .isEqualTo("1");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_APP_NAME))
        .isEqualTo("cf-app-name");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_ORG_ID))
        .isEqualTo("0193a375-8d8e-7e0c-a832-01ce9ded40dc");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_ORG_NAME))
        .isEqualTo("cf-org-name");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_PROCESS_ID))
        .isEqualTo("0193a4e3-8fd3-71b9-9fe3-5640c53bf1e2");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_PROCESS_TYPE))
        .isEqualTo("web");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_SPACE_ID))
        .isEqualTo("0193a7e7-da17-7ea4-8940-b1e07b401b16");
    assertThat(resource.getAttribute(CloudfoundryIncubatingAttributes.CLOUDFOUNDRY_SPACE_NAME))
        .isEqualTo("cf-space-name");
  }
}
