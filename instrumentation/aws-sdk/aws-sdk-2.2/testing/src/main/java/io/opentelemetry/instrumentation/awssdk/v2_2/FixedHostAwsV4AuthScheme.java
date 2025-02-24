/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.scheme.DefaultAwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.internal.signer.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;

final class FixedHostAwsV4AuthScheme implements AwsV4AuthScheme {

  private final FixedHostAwsV4HttpSigner signer;

  public FixedHostAwsV4AuthScheme(String apiUrl) {
    this.signer = new FixedHostAwsV4HttpSigner(apiUrl);
  }

  @Override
  public String schemeId() {
    return AwsV4AuthScheme.SCHEME_ID;
  }

  @Override
  public IdentityProvider<AwsCredentialsIdentity> identityProvider(
      IdentityProviders identityProviders) {
    return DefaultAwsV4AuthScheme.create().identityProvider(identityProviders);
  }

  @Override
  public AwsV4HttpSigner signer() {
    return signer;
  }

  private static class FixedHostAwsV4HttpSigner implements AwsV4HttpSigner {
    private static final AwsV4HttpSigner DEFAULT = new DefaultAwsV4HttpSigner();

    private final String apiUrl;

    FixedHostAwsV4HttpSigner(String apiUrl) {
      this.apiUrl = apiUrl;
    }

    @Override
    public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
      SdkHttpRequest original = request.request();
      SignRequest<? extends AwsCredentialsIdentity> override =
          request.toBuilder()
              .request(
                  request.request().toBuilder().port(443).protocol("https").host(apiUrl).build())
              .build();
      SignedRequest signed = DEFAULT.sign(override);
      return signed.toBuilder()
          .request(
              signed.request().toBuilder()
                  .protocol(original.protocol())
                  .host(original.host())
                  .port(original.port())
                  .build())
          .build();
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(
        AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
      // TODO: Implement
      return null;
    }
  }
}
