/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.instrumentation.khttp;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import khttp.responses.Response;
import org.jetbrains.annotations.Nullable;

final class KHttpHttpClientHttpAttributesExtractor
    extends HttpClientAttributesExtractor<RequestWrapper, Response> {
  @Nullable
  @Override
  protected String url(RequestWrapper requestWrapper) {
    return requestWrapper.uri;
  }

  @Nullable
  @Override
  protected String flavor(RequestWrapper requestWrapper, @Nullable Response response) {
    return SemanticAttributes.HttpFlavorValues.HTTP_1_1;
  }

  @Nullable
  @Override
  protected String method(RequestWrapper requestWrapper) {
    return requestWrapper.method;
  }

  @Override
  protected List<String> requestHeader(RequestWrapper requestWrapper, String name) {
    String header = requestWrapper.headers.get(name);
    return header != null ? singletonList(header) : emptyList();
  }

  @Nullable
  @Override
  protected Long requestContentLength(RequestWrapper requestWrapper, @Nullable Response response) {
    return null;
  }

  @Nullable
  @Override
  protected Long requestContentLengthUncompressed(
      RequestWrapper requestWrapper, @Nullable Response response) {
    return null;
  }

  @Override
  protected Integer statusCode(RequestWrapper requestWrapper, Response response) {
    return response.getStatusCode();
  }

  @Nullable
  @Override
  protected Long responseContentLength(RequestWrapper requestWrapper, Response response) {
    return null;
  }

  @Nullable
  @Override
  protected Long responseContentLengthUncompressed(
      RequestWrapper requestWrapper, Response response) {
    return null;
  }

  @Override
  protected List<String> responseHeader(
      RequestWrapper requestWrapper, Response response, String name) {
    String header = response.getHeaders().get(name);
    return header != null ? singletonList(header) : emptyList();
  }
}
