/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.recording;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

public final class ResponseHeaderScrubber extends ResponseTransformer {
  @Override
  public String getName() {
    return "scrub-response-header";
  }

  @Override
  public Response transform(
      Request request, Response response, FileSource fileSource, Parameters parameters) {
    HttpHeaders scrubbed = HttpHeaders.noHeaders();
    for (HttpHeader header : response.getHeaders().all()) {
      switch (header.key()) {
        case "Set-Cookie":
          scrubbed = scrubbed.plus(HttpHeader.httpHeader("Set-Cookie", "test_set_cookie"));
          break;
        default:
          scrubbed = scrubbed.plus(header);
          break;
      }
    }
    return Response.Builder.like(response).but().headers(scrubbed).build();
  }
}
