/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.couchbase.v2_0;

import com.couchbase.client.core.CouchbaseException;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import javax.annotation.Nullable;

final class CouchbaseAttributesGetter
    implements DbClientAttributesGetter<CouchbaseRequestInfo, Void> {

  @SuppressWarnings("deprecation") // using deprecated DbSystemIncubatingValues
  @Override
  public String getDbSystem(CouchbaseRequestInfo couchbaseRequest) {
    return DbIncubatingAttributes.DbSystemIncubatingValues.COUCHBASE;
  }

  @Deprecated
  @Override
  @Nullable
  public String getUser(CouchbaseRequestInfo couchbaseRequest) {
    return null;
  }

  @Override
  @Nullable
  public String getDbNamespace(CouchbaseRequestInfo couchbaseRequest) {
    return couchbaseRequest.bucket();
  }

  @Deprecated
  @Override
  @Nullable
  public String getConnectionString(CouchbaseRequestInfo couchbaseRequest) {
    return null;
  }

  @Override
  @Nullable
  public String getDbQueryText(CouchbaseRequestInfo couchbaseRequest) {
    return couchbaseRequest.statement();
  }

  @Override
  @Nullable
  public String getDbOperationName(CouchbaseRequestInfo couchbaseRequest) {
    return couchbaseRequest.operation();
  }

  @Nullable
  @Override
  public String getResponseStatusFromException(Throwable throwable) {
    if (throwable instanceof CouchbaseException) {
      //      ResponseStatusDetails details = ((CouchbaseException) throwable).details();
      // in newer versions of the SDK, this is available as a method on the exception
      // check if it's available and contains the status
      //      @InterfaceAudience.Public
      //       @InterfaceStability.Experimental
      //       public ResponseStatusDetails details() {
      //           return this.responseStatusDetails;
      //       }
      //      throwable.
      //      return throwable.getClass().getSimpleName();
    }
    return null;
  }
}
