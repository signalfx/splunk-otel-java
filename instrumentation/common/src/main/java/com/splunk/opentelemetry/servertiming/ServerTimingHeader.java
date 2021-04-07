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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.config.Config;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds {@code Server-Timing} header (and {@code Access-Control-Expose-Headers}) to the HTTP
 * response. The {@code Server-Timing} header contains the traceId and spanId of the server span.
 */
public final class ServerTimingHeader {
  private static final Logger log = LoggerFactory.getLogger(ServerTimingHeader.class);

  public static final String SERVER_TIMING = "Server-Timing";
  public static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";

  private static final String EMIT_RESPONSE_HEADERS_OLD = "splunk.context.server-timing.enabled";
  private static final String EMIT_RESPONSE_HEADERS = "splunk.trace-response-header.enabled";

  public static boolean shouldEmitServerTimingHeader() {
    return ConfigHolder.SHOULD_EMIT_RESPONSE_HEADERS;
  }

  public static <RS> void setHeaders(Context context, RS response, TextMapSetter<RS> headerSetter) {
    if (!Span.fromContext(context).getSpanContext().isValid()) {
      return;
    }

    headerSetter.set(response, SERVER_TIMING, toHeaderValue(context));
    headerSetter.set(response, EXPOSE_HEADERS, SERVER_TIMING);
  }

  private static String toHeaderValue(Context context) {
    Map<String, String> traceContextHeaders = new HashMap<>();
    W3CTraceContextPropagator.getInstance().inject(context, traceContextHeaders, Map::put);
    return "traceparent;desc=\"" + traceContextHeaders.get("traceparent") + "\"";
  }

  private ServerTimingHeader() {}

  // tests didn't like using Config in ServerTimingHeader.<clinit>
  static class ConfigHolder {
    private static final boolean SHOULD_EMIT_RESPONSE_HEADERS;

    static {
      Config config = Config.get();
      if (config.getProperty(EMIT_RESPONSE_HEADERS_OLD) != null) {
        log.warn(
            "Deprecated property '{}' was set; please use '{}' instead. Support for the deprecated property will be removed in future versions.",
            EMIT_RESPONSE_HEADERS_OLD,
            EMIT_RESPONSE_HEADERS);
      }
      SHOULD_EMIT_RESPONSE_HEADERS =
          config.getBooleanProperty(
              EMIT_RESPONSE_HEADERS, config.getBooleanProperty(EMIT_RESPONSE_HEADERS_OLD, true));
    }
  }
}
