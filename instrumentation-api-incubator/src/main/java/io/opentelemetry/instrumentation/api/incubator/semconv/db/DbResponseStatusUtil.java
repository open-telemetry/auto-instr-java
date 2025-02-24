package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import javax.annotation.Nullable;

/** Utility class for database response status. */
public class DbResponseStatusUtil {
  private DbResponseStatusUtil() {
  }

  @Nullable
  public static String httpStatusToResponseStatus(int httpStatus) {
    return httpStatus >= 400 && httpStatus< 600 ? Integer.toString(httpStatus) : null;
  }
}
