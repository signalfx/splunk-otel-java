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

package com.splunk.opentelemetry.khttp;

import static com.splunk.opentelemetry.khttp.KHttpHeadersInjectAdapter.SETTER;

import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.tracer.HttpClientTracer;
import io.opentelemetry.instrumentation.api.tracer.net.NetPeerAttributes;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import khttp.responses.Response;

public class KHttpTracer extends HttpClientTracer<RequestWrapper, Map<String, String>, Response> {
  private static final KHttpTracer TRACER = new KHttpTracer();

  private KHttpTracer() {
    super(NetPeerAttributes.INSTANCE);
  }

  public static KHttpTracer tracer() {
    return TRACER;
  }

  @Override
  protected String method(RequestWrapper requestWrapper) {
    return requestWrapper.method;
  }

  @Override
  protected URI url(RequestWrapper requestWrapper) throws URISyntaxException {
    return new URI(requestWrapper.uri);
  }

  @Override
  protected Integer status(Response response) {
    return response.getStatusCode();
  }

  @Override
  protected String requestHeader(RequestWrapper requestWrapper, String name) {
    return requestWrapper.headers.get(name);
  }

  @Override
  protected String responseHeader(Response response, String name) {
    return response.getHeaders().get(name);
  }

  @Override
  protected TextMapSetter<Map<String, String>> getSetter() {
    return SETTER;
  }

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.javaagent.khttp-0.1";
  }
}
