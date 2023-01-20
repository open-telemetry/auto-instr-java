package io.opentelemetry.instrumentation.testing.junit.http;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

@AutoValue
public abstract class Options {
  public static final Set<AttributeKey<?>> DEFAULT_HTTP_ATTRIBUTES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  SemanticAttributes.NET_PEER_NAME,
                  SemanticAttributes.NET_PEER_PORT,
                  SemanticAttributes.HTTP_URL,
                  SemanticAttributes.HTTP_METHOD,
                  SemanticAttributes.HTTP_FLAVOR,
                  SemanticAttributes.HTTP_USER_AGENT)));

  public static final BiFunction<URI, String, String> DEFAULT_EXPECTED_CLIENT_SPAN_NAME_MAPPER =
      (uri, method) -> method != null ? "HTTP " + method : "HTTP request";
  public abstract Function<URI, Set<AttributeKey<?>>> getHttpAttributes();
  public abstract Integer getResponseCodeOnRedirectError();
  public abstract String getUserAgent();
  public abstract BiFunction<URI, Throwable, Throwable> getClientSpanErrorMapper();
  public abstract BiFunction<String, Integer, SingleConnection> getSingleConnectionFactory();
  public abstract BiFunction<URI, String, String> getExpectedClientSpanNameMapper();
  public abstract boolean getTestWithClientParent();
  public abstract boolean getTestRedirects();
  public abstract boolean getTestCircularRedirects();
  public abstract int getMaxRedirects();
  public abstract boolean getTestReusedRequest();
  public abstract boolean getTestConnectionFailure();
  public abstract boolean getTestReadTimeout();
  public abstract boolean getTestRemoteConnection();
  public abstract boolean getTestHttps();
  public abstract boolean getTestCallback();
  public abstract boolean getTestCallbackWithParent();
  public abstract boolean getTestCallbackWithImplicitParent();
  public abstract boolean getTestErrorWithCallback();
  static Builder builder() {
    return new AutoValue_Options.Builder().withDefaults();
  }

  @AutoValue.Builder
  public interface Builder {

    default Builder withDefaults(){
      return setHttpAttributes(x -> DEFAULT_HTTP_ATTRIBUTES)
          .setResponseCodeOnRedirectError(null)
          .setUserAgent(null)
          .setClientSpanErrorMapper((uri, exception) -> exception)
          .setSingleConnectionFactory((host, port) -> null)
          .setExpectedClientSpanNameMapper(DEFAULT_EXPECTED_CLIENT_SPAN_NAME_MAPPER)
          .setTestWithClientParent(true)
          .setTestRedirects(true)
          .setTestCircularRedirects(true)
          .setMaxRedirects(2)
          .setTestReusedRequest(true)
          .setTestConnectionFailure(true)
          .setTestReadTimeout(false)
          .setTestRemoteConnection(true)
          .setTestHttps(true)
          .setTestCallback(true)
          .setTestCallbackWithParent(true)
          .setTestCallbackWithImplicitParent(false)
          .setTestErrorWithCallback(true)
          ;
    }

    Builder setHttpAttributes(Function<URI, Set<AttributeKey<?>>> value);

    Builder setResponseCodeOnRedirectError(Integer value);

    Builder setUserAgent(String value);

    Builder setClientSpanErrorMapper(BiFunction<URI,Throwable,Throwable> value);

    Builder setSingleConnectionFactory(BiFunction<String,Integer,SingleConnection> value);

    Builder setExpectedClientSpanNameMapper(BiFunction<URI,String,String> value);

    Builder setTestWithClientParent(boolean value);

    Builder setTestRedirects(boolean value);

    Builder setTestCircularRedirects(boolean value);
    Builder setMaxRedirects(int value);
    Builder setTestReusedRequest(boolean value);
    Builder setTestConnectionFailure(boolean value);
    Builder setTestReadTimeout(boolean value);
    Builder setTestRemoteConnection(boolean value);
    Builder setTestHttps(boolean value);
    Builder setTestCallback(boolean value);
    Builder setTestCallbackWithParent(boolean value);
    Builder setTestCallbackWithImplicitParent(boolean value);
    Builder setTestErrorWithCallback(boolean value);

    Options build();
  }
}
