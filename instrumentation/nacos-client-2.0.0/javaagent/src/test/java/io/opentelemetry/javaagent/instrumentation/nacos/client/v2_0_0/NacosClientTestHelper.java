/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.nacos.api.config.remote.request.ConfigChangeNotifyRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigPublishRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigQueryRequest;
import com.alibaba.nacos.api.config.remote.request.ConfigRemoveRequest;
import com.alibaba.nacos.api.naming.remote.request.InstanceRequest;
import com.alibaba.nacos.api.naming.remote.request.NotifySubscriberRequest;
import com.alibaba.nacos.api.naming.remote.request.ServiceListRequest;
import com.alibaba.nacos.api.naming.remote.request.ServiceQueryRequest;
import com.alibaba.nacos.api.naming.remote.request.SubscribeServiceRequest;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.response.HealthCheckResponse;
import com.alibaba.nacos.api.remote.response.Response;
import com.alibaba.nacos.api.remote.response.ResponseCode;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.assertj.AttributeAssertion;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NacosClientTestHelper {
  public static final List<Request> RPC_CLIENT_HANDLE_SERVER_REQUEST_REQUEST_LIST =
      new ArrayList<>();
  public static final List<Request> GRPC_CONNECTION_REQUEST_LIST = new ArrayList<>();
  public static final List<Request> REQUEST_LIST = new ArrayList<>();
  public static final Map<String, Request> NACOS_CLIENT_REQUEST_MAP = new HashMap<>();
  public static final Map<Request, String> NACOS_CLIENT_REQUEST_NAME_MAP = new HashMap<>();

  public static final String NAMESPACE = "namespace";
  public static final String GROUP_NAME = "groupName";
  public static final String SERVICE_NAME = "serviceName";
  public static final String DATA_ID = "dataId";
  public static final String GROUP = "group";
  public static final String TENANT = "tenant";
  public static final String INSTANCE_REQUEST_TYPE = "instanceRequestType";
  public static final ConfigChangeNotifyRequest CONFIG_CHANGE_NOTIFY_REQUEST;
  public static final ConfigPublishRequest CONFIG_PUBLISH_REQUEST;
  public static final ConfigQueryRequest CONFIG_QUERY_REQUEST;
  public static final ConfigRemoveRequest CONFIG_REMOVE_REQUEST;
  public static final InstanceRequest INSTANCE_REQUEST;
  public static final NotifySubscriberRequest NOTIFY_SUBSCRIBER_REQUEST;
  public static final ServiceListRequest SERVICE_LIST_REQUEST;
  public static final ServiceQueryRequest SERVICE_QUERY_REQUEST;
  public static final SubscribeServiceRequest SUBSCRIBE_SERVICE_REQUEST;
  public static final SubscribeServiceRequest UN_SUBSCRIBE_SERVICE_REQUEST;

  public static final Response SUCCESS_RESPONSE;
  public static final Response ERROR_RESPONSE;
  public static final Response NULL_RESPONSE;

  public static final String NACOS_PREFIX = "Nacos/";

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

  private NacosClientTestHelper() {}

  static {
    SUCCESS_RESPONSE = new HealthCheckResponse();
    HealthCheckResponse errorResponse = new HealthCheckResponse();
    errorResponse.setResultCode(ResponseCode.FAIL.getCode());
    ERROR_RESPONSE = errorResponse;
    NULL_RESPONSE = null;

    INSTANCE_REQUEST = mock(InstanceRequest.class);
    when(INSTANCE_REQUEST.getType()).thenReturn(INSTANCE_REQUEST_TYPE);
    when(INSTANCE_REQUEST.getNamespace()).thenReturn(NAMESPACE);
    when(INSTANCE_REQUEST.getGroupName()).thenReturn(GROUP_NAME);
    when(INSTANCE_REQUEST.getServiceName()).thenReturn(SERVICE_NAME);

    SERVICE_QUERY_REQUEST = mock(ServiceQueryRequest.class);
    when(SERVICE_QUERY_REQUEST.getNamespace()).thenReturn(NAMESPACE);
    when(SERVICE_QUERY_REQUEST.getGroupName()).thenReturn(GROUP_NAME);
    when(SERVICE_QUERY_REQUEST.getServiceName()).thenReturn(SERVICE_NAME);

    SUBSCRIBE_SERVICE_REQUEST = mock(SubscribeServiceRequest.class);
    when(SUBSCRIBE_SERVICE_REQUEST.isSubscribe()).thenReturn(true);
    when(SUBSCRIBE_SERVICE_REQUEST.getNamespace()).thenReturn(NAMESPACE);
    when(SUBSCRIBE_SERVICE_REQUEST.getGroupName()).thenReturn(GROUP_NAME);
    when(SUBSCRIBE_SERVICE_REQUEST.getServiceName()).thenReturn(SERVICE_NAME);

    UN_SUBSCRIBE_SERVICE_REQUEST = mock(SubscribeServiceRequest.class);
    when(UN_SUBSCRIBE_SERVICE_REQUEST.isSubscribe()).thenReturn(false);
    when(UN_SUBSCRIBE_SERVICE_REQUEST.getNamespace()).thenReturn(NAMESPACE);
    when(UN_SUBSCRIBE_SERVICE_REQUEST.getGroupName()).thenReturn(GROUP_NAME);
    when(UN_SUBSCRIBE_SERVICE_REQUEST.getServiceName()).thenReturn(SERVICE_NAME);

    SERVICE_LIST_REQUEST = mock(ServiceListRequest.class);
    when(SERVICE_LIST_REQUEST.getNamespace()).thenReturn(NAMESPACE);
    when(SERVICE_LIST_REQUEST.getGroupName()).thenReturn(GROUP_NAME);
    when(SERVICE_LIST_REQUEST.getServiceName()).thenReturn(SERVICE_NAME);

    CONFIG_QUERY_REQUEST = mock(ConfigQueryRequest.class);
    when(CONFIG_QUERY_REQUEST.getDataId()).thenReturn(DATA_ID);
    when(CONFIG_QUERY_REQUEST.getGroup()).thenReturn(GROUP);
    when(CONFIG_QUERY_REQUEST.getTenant()).thenReturn(TENANT);

    CONFIG_PUBLISH_REQUEST = mock(ConfigPublishRequest.class);
    when(CONFIG_PUBLISH_REQUEST.getDataId()).thenReturn(DATA_ID);
    when(CONFIG_PUBLISH_REQUEST.getGroup()).thenReturn(GROUP);
    when(CONFIG_PUBLISH_REQUEST.getTenant()).thenReturn(TENANT);

    CONFIG_REMOVE_REQUEST = mock(ConfigRemoveRequest.class);
    when(CONFIG_REMOVE_REQUEST.getDataId()).thenReturn(DATA_ID);
    when(CONFIG_REMOVE_REQUEST.getGroup()).thenReturn(GROUP);
    when(CONFIG_REMOVE_REQUEST.getTenant()).thenReturn(TENANT);

    NOTIFY_SUBSCRIBER_REQUEST = mock(NotifySubscriberRequest.class);
    when(NOTIFY_SUBSCRIBER_REQUEST.getNamespace()).thenReturn(NAMESPACE);
    when(NOTIFY_SUBSCRIBER_REQUEST.getGroupName()).thenReturn(GROUP_NAME);
    when(NOTIFY_SUBSCRIBER_REQUEST.getServiceName()).thenReturn(SERVICE_NAME);

    CONFIG_CHANGE_NOTIFY_REQUEST = mock(ConfigChangeNotifyRequest.class);
    when(CONFIG_CHANGE_NOTIFY_REQUEST.getDataId()).thenReturn(DATA_ID);
    when(CONFIG_CHANGE_NOTIFY_REQUEST.getGroup()).thenReturn(GROUP);
    when(CONFIG_CHANGE_NOTIFY_REQUEST.getTenant()).thenReturn(TENANT);

    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + NOTIFY_CONFIG_CHANGE, CONFIG_CHANGE_NOTIFY_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + PUBLISH_CONFIG, CONFIG_PUBLISH_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + QUERY_CONFIG, CONFIG_QUERY_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + REMOVE_CONFIG, CONFIG_REMOVE_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + INSTANCE_REQUEST_TYPE, INSTANCE_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + NOTIFY_SUBSCRIBE_CHANGE, NOTIFY_SUBSCRIBER_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + GET_SERVICE_LIST, SERVICE_LIST_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + QUERY_SERVICE, SERVICE_QUERY_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + SUBSCRIBE_SERVICE, SUBSCRIBE_SERVICE_REQUEST);
    NACOS_CLIENT_REQUEST_MAP.put(NACOS_PREFIX + UNSUBSCRIBE_SERVICE, UN_SUBSCRIBE_SERVICE_REQUEST);

    for (Request value : NACOS_CLIENT_REQUEST_MAP.values()) {
      REQUEST_LIST.add(value);
      if (value instanceof NotifySubscriberRequest || value instanceof ConfigChangeNotifyRequest) {
        RPC_CLIENT_HANDLE_SERVER_REQUEST_REQUEST_LIST.add(value);
      } else {
        GRPC_CONNECTION_REQUEST_LIST.add(value);
      }
    }
    NACOS_CLIENT_REQUEST_NAME_MAP.putAll(
        NACOS_CLIENT_REQUEST_MAP.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));
  }

  private static final String[] methodNames = {
    "getNamespace", "getGroupName", "getServiceName", "getDataId", "getGroup", "getTenant"
  };

  private static final Map<String, AttributeKey<String>> methodToAttributeMap = new HashMap<>();

  static {
    methodToAttributeMap.put("getNamespace", NACOS_NAME_SPACE_ATTR);
    methodToAttributeMap.put("getGroupName", NACOS_GROUP_NAME_ATTR);
    methodToAttributeMap.put("getServiceName", NACOS_SERVICE_NAME_ATTR);
    methodToAttributeMap.put("getDataId", NACOS_DATA_ID_ATTR);
    methodToAttributeMap.put("getGroup", NACOS_GROUP_ATTR);
    methodToAttributeMap.put("getTenant", NACOS_TENANT_ATTR);
  }

  public static List<AttributeAssertion> requestAttributeAssertions(
      String codeNamespace, String codeFunction, Request request) {
    List<AttributeAssertion> attributeAssertions =
        new ArrayList<>(
            asList(
                equalTo(AttributeKey.stringKey("code.namespace"), codeNamespace),
                equalTo(AttributeKey.stringKey("code.function"), codeFunction),
                equalTo(AttributeKey.stringKey("service.discovery.system"), "nacos")));

    for (String methodName : methodNames) {
      Method method = null;
      try {
        method = request.getClass().getMethod(methodName);
      } catch (NoSuchMethodException e) {
        continue;
      }
      if (method != null) {
        method.setAccessible(true);
        Object result = null;
        try {
          result = method.invoke(request);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        }
        attributeAssertions.add(
            equalTo(methodToAttributeMap.get(methodName), String.valueOf(result)));
      }
    }

    return attributeAssertions;
  }
}
