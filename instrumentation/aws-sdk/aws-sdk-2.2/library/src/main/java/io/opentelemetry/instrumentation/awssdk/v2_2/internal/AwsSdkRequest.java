/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.internal;

import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.AwsSdkRequestType.BEDROCK_RUNTIME;
import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.AwsSdkRequestType.DYNAMODB;
import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.AwsSdkRequestType.KINESIS;
import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.AwsSdkRequestType.S3;
import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.AwsSdkRequestType.SNS;
import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.AwsSdkRequestType.SQS;
import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.FieldMapping.request;
import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.FieldMapping.response;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.SdkRequest;

/**
 * Temporary solution - maps only DynamoDB attributes. Final solution should be generated from AWS
 * SDK automatically
 * (https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/2291).
 */
// We match the actual name in the AWS SDK for better consistency with it and possible future
// autogeneration.
@SuppressWarnings("MemberName")
enum AwsSdkRequest {
  // generic requests
  DynamoDbRequest(DYNAMODB, "DynamoDbRequest"),
  S3Request(S3, "S3Request"),
  SnsRequest(SNS, "SnsRequest"),
  SqsRequest(SQS, "SqsRequest"),
  KinesisRequest(KINESIS, "KinesisRequest"),
  // specific requests
  BatchGetItem(
      DYNAMODB,
      "BatchGetItemRequest",
      request("aws.dynamodb.table_names", "RequestItems"),
      response("aws.dynamodb.consumed_capacity", "ConsumedCapacity")),
  BatchWriteItem(
      DYNAMODB,
      "BatchWriteItemRequest",
      request("aws.dynamodb.table_names", "RequestItems"),
      response("aws.dynamodb.consumed_capacity", "ConsumedCapacity"),
      response("aws.dynamodb.item_collection_metrics", "ItemCollectionMetrics")),
  CreateTable(
      DYNAMODB,
      "CreateTableRequest",
      request("aws.dynamodb.global_secondary_indexes", "GlobalSecondaryIndexes"),
      request("aws.dynamodb.local_secondary_indexes", "LocalSecondaryIndexes"),
      request(
          "aws.dynamodb.provisioned_throughput.read_capacity_units",
          "ProvisionedThroughput.ReadCapacityUnits"),
      request(
          "aws.dynamodb.provisioned_throughput.write_capacity_units",
          "ProvisionedThroughput.WriteCapacityUnits")),
  DeleteItem(
      DYNAMODB,
      "DeleteItemRequest",
      response("aws.dynamodb.consumed_capacity", "ConsumedCapacity"),
      response("aws.dynamodb.item_collection_metrics", "ItemCollectionMetrics")),
  GetItem(
      DYNAMODB,
      "GetItemRequest",
      request("aws.dynamodb.projection_expression", "ProjectionExpression"),
      response("aws.dynamodb.consumed_capacity", "ConsumedCapacity"),
      request("aws.dynamodb.consistent_read", "ConsistentRead")),
  ListTables(
      DYNAMODB,
      "ListTablesRequest",
      request("aws.dynamodb.exclusive_start_table_name", "ExclusiveStartTableName"),
      response("aws.dynamodb.table_count", "TableNames"),
      request("aws.dynamodb.limit", "Limit")),
  PutItem(
      DYNAMODB,
      "PutItemRequest",
      response("aws.dynamodb.consumed_capacity", "ConsumedCapacity"),
      response("aws.dynamodb.item_collection_metrics", "ItemCollectionMetrics")),
  Query(
      DYNAMODB,
      "QueryRequest",
      request("aws.dynamodb.attributes_to_get", "AttributesToGet"),
      request("aws.dynamodb.consistent_read", "ConsistentRead"),
      request("aws.dynamodb.index_name", "IndexName"),
      request("aws.dynamodb.limit", "Limit"),
      request("aws.dynamodb.projection_expression", "ProjectionExpression"),
      request("aws.dynamodb.scan_index_forward", "ScanIndexForward"),
      request("aws.dynamodb.select", "Select"),
      response("aws.dynamodb.consumed_capacity", "ConsumedCapacity")),
  Scan(
      DYNAMODB,
      "ScanRequest",
      request("aws.dynamodb.attributes_to_get", "AttributesToGet"),
      request("aws.dynamodb.consistent_read", "ConsistentRead"),
      request("aws.dynamodb.index_name", "IndexName"),
      request("aws.dynamodb.limit", "Limit"),
      request("aws.dynamodb.projection_expression", "ProjectionExpression"),
      request("aws.dynamodb.segment", "Segment"),
      request("aws.dynamodb.select", "Select"),
      request("aws.dynamodb.total_segments", "TotalSegments"),
      response("aws.dynamodb.consumed_capacity", "ConsumedCapacity"),
      response("aws.dynamodb.count", "Count"),
      response("aws.dynamodb.scanned_count", "ScannedCount")),
  UpdateItem(
      DYNAMODB,
      "UpdateItemRequest",
      response("aws.dynamodb.consumed_capacity", "ConsumedCapacity"),
      response("aws.dynamodb.item_collection_metrics", "ItemCollectionMetrics")),
  UpdateTable(
      DYNAMODB,
      "UpdateTableRequest",
      request("aws.dynamodb.attribute_definitions", "AttributeDefinitions"),
      request("aws.dynamodb.global_secondary_index_updates", "GlobalSecondaryIndexUpdates"),
      request(
          "aws.dynamodb.provisioned_throughput.read_capacity_units",
          "ProvisionedThroughput.ReadCapacityUnits"),
      request(
          "aws.dynamodb.provisioned_throughput.write_capacity_units",
          "ProvisionedThroughput.WriteCapacityUnits")),
  ConverseRequest(BEDROCK_RUNTIME, "ConverseRequest", request("gen_ai.request.model", "modelId")),
  ;

  private final AwsSdkRequestType type;
  private final String requestClass;

  // Wrap in unmodifiableMap
  @SuppressWarnings("ImmutableEnumChecker")
  private final Map<FieldMapping.Type, List<FieldMapping>> fields;

  AwsSdkRequest(AwsSdkRequestType type, String requestClass, FieldMapping... fields) {
    this.type = type;
    this.requestClass = requestClass;
    this.fields = Collections.unmodifiableMap(FieldMapping.groupByType(fields));
  }

  @Nullable
  static AwsSdkRequest ofSdkRequest(SdkRequest request) {
    // try request type
    AwsSdkRequest result = ofType(request.getClass().getSimpleName());
    // try parent - generic
    if (result == null) {
      result = ofType(request.getClass().getSuperclass().getSimpleName());
    }
    return result;
  }

  private static AwsSdkRequest ofType(String typeName) {
    for (AwsSdkRequest type : values()) {
      if (type.requestClass.equals(typeName)) {
        return type;
      }
    }
    return null;
  }

  List<FieldMapping> fields(FieldMapping.Type type) {
    return fields.get(type);
  }

  AwsSdkRequestType type() {
    return type;
  }
}
