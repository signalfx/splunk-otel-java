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

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import khttp.responses.Response;
import org.jetbrains.annotations.Nullable;

final class KHttpHttpClientHttpAttributesGetter
    implements HttpClientAttributesGetter<RequestWrapper, Response> {

  @Nullable
  @Override
  public String getUrl(RequestWrapper requestWrapper) {
    return requestWrapper.uri;
  }

  @Nullable
  @Override
  public String getMethod(RequestWrapper requestWrapper) {
    return requestWrapper.method;
  }

  @Override
  public List<String> getRequestHeader(RequestWrapper requestWrapper, String name) {
    return requestWrapper.headers.entrySet().stream()
        .filter(e -> e.getKey().equalsIgnoreCase(name))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  @Override
  public Integer getStatusCode(
      RequestWrapper requestWrapper, Response response, @Nullable Throwable error) {
    return response.getStatusCode();
  }

  @Override
  public List<String> getResponseHeader(
      RequestWrapper requestWrapper, Response response, String name) {
    String header = response.getHeaders().get(name);
    return header != null ? singletonList(header) : emptyList();
  }
}
