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

import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import khttp.responses.Response;
import org.jetbrains.annotations.Nullable;

final class KHttpHttpClientNetAttributesGetter
    implements NetClientAttributesGetter<RequestWrapper, Response> {
  @Override
  public String transport(RequestWrapper requestWrapper, @Nullable Response response) {
    return SemanticAttributes.NetTransportValues.IP_TCP;
  }

  @Nullable
  @Override
  public String peerName(RequestWrapper requestWrapper, @Nullable Response response) {
    if (requestWrapper.parsedUri != null) {
      return requestWrapper.parsedUri.getHost();
    }
    return null;
  }

  @Nullable
  @Override
  public Integer peerPort(RequestWrapper requestWrapper, @Nullable Response response) {
    if (requestWrapper.parsedUri != null && requestWrapper.parsedUri.getPort() > 0) {
      return requestWrapper.parsedUri.getPort();
    }
    return null;
  }
}
