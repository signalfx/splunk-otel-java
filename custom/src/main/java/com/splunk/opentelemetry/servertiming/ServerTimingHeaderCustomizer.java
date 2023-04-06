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

package com.splunk.opentelemetry.servertiming;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseCustomizer;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseMutator;

/**
 * Adds {@code Server-Timing} header (and {@code Access-Control-Expose-Headers}) to the HTTP
 * response. The {@code Server-Timing} header contains the traceId and spanId of the server span.
 */
@AutoService(HttpServerResponseCustomizer.class)
public class ServerTimingHeaderCustomizer implements HttpServerResponseCustomizer {
  static final String SERVER_TIMING = "Server-Timing";
  static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";

  static boolean enabled = false;

  @Override
  public <RESPONSE> void customize(
      Context context, RESPONSE response, HttpServerResponseMutator<RESPONSE> responseMutator) {
    if (!enabled || !Span.fromContext(context).getSpanContext().isValid()) {
      return;
    }

    responseMutator.appendHeader(response, SERVER_TIMING, toHeaderValue(context));
    responseMutator.appendHeader(response, EXPOSE_HEADERS, SERVER_TIMING);
  }

  private static String toHeaderValue(Context context) {
    TraceParentHolder traceParentHolder = new TraceParentHolder();
    W3CTraceContextPropagator.getInstance()
        .inject(context, traceParentHolder, TraceParentHolder::set);
    return "traceparent;desc=\"" + traceParentHolder.traceParent + "\"";
  }

  private static class TraceParentHolder {
    String traceParent;

    public void set(String key, String value) {
      if ("traceparent".equals(key)) {
        traceParent = value;
      }
    }
  }
}
