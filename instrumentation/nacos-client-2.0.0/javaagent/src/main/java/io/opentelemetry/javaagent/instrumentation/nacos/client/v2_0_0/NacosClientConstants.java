/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0;

import io.opentelemetry.api.common.AttributeKey;

public class NacosClientConstants {
  private NacosClientConstants() {}

  public static final String OTEL_NACOS_CLIENT_ENABLED =
      "otel.instrumentation.nacos-client.default-enabled";

  public static final String NACOS_PREFIX = "Nacos/";

  public static final String SERVER_CHECK = "serverCheck";

  public static final String QUERY_SERVICE = "queryService";

  public static final String SUBSCRIBE_SERVICE = "subscribeService";

  public static final String UNSUBSCRIBE_SERVICE = "unsubscribeService";

  public static final String QUERY_CONFIG = "queryConfig";

  public static final String PUBLISH_CONFIG = "publishConfig";

  public static final String REMOVE_CONFIG = "removeConfig";

  public static final String GET_SERVICE_LIST = "getServiceList";

  public static final String NOTIFY_SUBSCRIBE_CHANGE = "notifySubscribeChange";

  public static final String NOTIFY_CONFIG_CHANGE = "notifyConfigChange";

  public static final AttributeKey<String> NACOS_NAME_SPACE_ATTR =
      AttributeKey.stringKey("nacos.namespace");

  public static final AttributeKey<String> NACOS_GROUP_NAME_ATTR =
      AttributeKey.stringKey("nacos.group.name");

  public static final AttributeKey<String> NACOS_SERVICE_NAME_ATTR =
      AttributeKey.stringKey("nacos.service.name");

  public static final AttributeKey<String> NACOS_DATA_ID_ATTR =
      AttributeKey.stringKey("nacos.data.id");

  public static final AttributeKey<String> NACOS_GROUP_ATTR = AttributeKey.stringKey("nacos.group");

  public static final AttributeKey<String> NACOS_TENANT_ATTR =
      AttributeKey.stringKey("nacos.tenant");
}
