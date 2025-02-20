/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0;

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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

public class NacosClientHelper {
  private static final NacosClientRequestOperator UNKNOWN_OPERATOR =
      new NacosClientRequestOperator(request -> request.getClass().getSimpleName(), null);
  private static final Map<Class<? extends Request>, NacosClientRequestOperator>
      KNOWN_OPERATOR_MAP = new HashMap<>();

  private NacosClientHelper() {}

  static {
    KNOWN_OPERATOR_MAP.put(
        InstanceRequest.class,
        new NacosClientRequestOperator(
            request -> ((InstanceRequest) request).getType(),
            (attributesBuilder, request) -> {
              InstanceRequest instanceRequest = (InstanceRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_NAME_SPACE_ATTR, instanceRequest.getNamespace());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_NAME_ATTR, instanceRequest.getGroupName());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_SERVICE_NAME_ATTR, instanceRequest.getServiceName());
            }));

    KNOWN_OPERATOR_MAP.put(
        ServiceQueryRequest.class,
        new NacosClientRequestOperator(
            request -> NacosClientConstants.QUERY_SERVICE,
            (attributesBuilder, request) -> {
              ServiceQueryRequest serviceQueryRequest = (ServiceQueryRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_NAME_SPACE_ATTR, serviceQueryRequest.getNamespace());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_NAME_ATTR, serviceQueryRequest.getGroupName());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_SERVICE_NAME_ATTR,
                  serviceQueryRequest.getServiceName());
            }));

    KNOWN_OPERATOR_MAP.put(
        SubscribeServiceRequest.class,
        new NacosClientRequestOperator(
            request ->
                ((SubscribeServiceRequest) request).isSubscribe()
                    ? NacosClientConstants.SUBSCRIBE_SERVICE
                    : NacosClientConstants.UNSUBSCRIBE_SERVICE,
            (attributesBuilder, request) -> {
              SubscribeServiceRequest subscribeServiceRequest = (SubscribeServiceRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_NAME_SPACE_ATTR,
                  subscribeServiceRequest.getNamespace());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_NAME_ATTR,
                  subscribeServiceRequest.getGroupName());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_SERVICE_NAME_ATTR,
                  subscribeServiceRequest.getServiceName());
            }));

    KNOWN_OPERATOR_MAP.put(
        ServiceListRequest.class,
        new NacosClientRequestOperator(
            request -> NacosClientConstants.GET_SERVICE_LIST,
            (attributesBuilder, request) -> {
              ServiceListRequest serviceListRequest = (ServiceListRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_NAME_SPACE_ATTR, serviceListRequest.getNamespace());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_NAME_ATTR, serviceListRequest.getGroupName());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_SERVICE_NAME_ATTR,
                  serviceListRequest.getServiceName());
            }));

    KNOWN_OPERATOR_MAP.put(
        ConfigQueryRequest.class,
        new NacosClientRequestOperator(
            request -> NacosClientConstants.QUERY_CONFIG,
            (attributesBuilder, request) -> {
              ConfigQueryRequest configQueryRequest = (ConfigQueryRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_DATA_ID_ATTR, configQueryRequest.getDataId());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_ATTR, configQueryRequest.getGroup());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_TENANT_ATTR, configQueryRequest.getTenant());
            }));

    KNOWN_OPERATOR_MAP.put(
        ConfigPublishRequest.class,
        new NacosClientRequestOperator(
            request -> NacosClientConstants.PUBLISH_CONFIG,
            (attributesBuilder, request) -> {
              ConfigPublishRequest configPublishRequest = (ConfigPublishRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_DATA_ID_ATTR, configPublishRequest.getDataId());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_ATTR, configPublishRequest.getGroup());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_TENANT_ATTR, configPublishRequest.getTenant());
            }));

    KNOWN_OPERATOR_MAP.put(
        ConfigRemoveRequest.class,
        new NacosClientRequestOperator(
            request -> NacosClientConstants.REMOVE_CONFIG,
            (attributesBuilder, request) -> {
              ConfigRemoveRequest configRemoveRequest = (ConfigRemoveRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_DATA_ID_ATTR, configRemoveRequest.getDataId());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_ATTR, configRemoveRequest.getGroup());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_TENANT_ATTR, configRemoveRequest.getTenant());
            }));

    KNOWN_OPERATOR_MAP.put(
        NotifySubscriberRequest.class,
        new NacosClientRequestOperator(
            request -> NacosClientConstants.NOTIFY_SUBSCRIBE_CHANGE,
            (attributesBuilder, request) -> {
              NotifySubscriberRequest notifySubscriberRequest = (NotifySubscriberRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_NAME_SPACE_ATTR,
                  notifySubscriberRequest.getNamespace());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_NAME_ATTR,
                  notifySubscriberRequest.getGroupName());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_SERVICE_NAME_ATTR,
                  notifySubscriberRequest.getServiceName());
            }));

    KNOWN_OPERATOR_MAP.put(
        ConfigChangeNotifyRequest.class,
        new NacosClientRequestOperator(
            request -> NacosClientConstants.NOTIFY_CONFIG_CHANGE,
            (attributesBuilder, request) -> {
              ConfigChangeNotifyRequest configChangeNotifyRequest =
                  (ConfigChangeNotifyRequest) request;
              attributesBuilder.put(
                  NacosClientConstants.NACOS_DATA_ID_ATTR, configChangeNotifyRequest.getDataId());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_GROUP_ATTR, configChangeNotifyRequest.getGroup());
              attributesBuilder.put(
                  NacosClientConstants.NACOS_TENANT_ATTR, configChangeNotifyRequest.getTenant());
            }));
  }

  @Nonnull
  public static NacosClientRequestOperator getOperator(@Nonnull Request request) {
    NacosClientRequestOperator nacosClientRequestOperator =
        KNOWN_OPERATOR_MAP.get(request.getClass());
    if (nacosClientRequestOperator != null) {
      return nacosClientRequestOperator;
    }
    return UNKNOWN_OPERATOR;
  }
}
