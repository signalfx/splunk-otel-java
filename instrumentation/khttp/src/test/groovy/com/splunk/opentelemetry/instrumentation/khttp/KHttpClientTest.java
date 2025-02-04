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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestOptions;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import khttp.KHttp;
import khttp.responses.Response;
import org.junit.jupiter.api.extension.RegisterExtension;

class KHttpClientTest extends AbstractHttpClientTest<Void> {

  @RegisterExtension
  static final InstrumentationExtension testing = HttpClientInstrumentationExtension.forAgent();

  @Override
  protected void configure(HttpClientTestOptions.Builder optionsBuilder) {
    super.configure(optionsBuilder);

    optionsBuilder.disableTestCircularRedirects();
    // these tests will pass, but they don't really test anything since REQUEST is Void
    optionsBuilder.disableTestReusedRequest();
    optionsBuilder.disableTestCallback();
    optionsBuilder.setHttpAttributes(
        uri -> {
          Set<AttributeKey<?>> attributes =
              new HashSet<>(HttpClientTestOptions.DEFAULT_HTTP_ATTRIBUTES);
          attributes.remove(AttributeKey.stringKey("network.protocol.version"));
          return attributes;
        });
    optionsBuilder.spanEndsAfterBody();
  }

  @Override
  public Void buildRequest(String method, URI uri, Map<String, String> headers) {
    return null;
  }

  @Override
  public int sendRequest(Void request, String method, URI uri, Map<String, String> headers) {
    // khttp applies the same timeout for both connect and read
    long timeoutSeconds = CONNECTION_TIMEOUT.toSeconds();
    Response response =
        KHttp.request(
            method,
            uri.toString(),
            headers,
            Collections.emptyMap(),
            null,
            null,
            null,
            null,
            timeoutSeconds);
    return response.getStatusCode();
  }
}
