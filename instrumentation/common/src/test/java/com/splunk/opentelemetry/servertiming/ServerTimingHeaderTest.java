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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.test.utils.TraceUtils;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ServerTimingHeaderTest {
  @BeforeAll
  static void setUpOpenTelemetry() {
    OpenTelemetrySdk.builder().buildAndRegisterGlobal();
  }

  @Test
  void shouldNotSetAnyHeadersWithoutValidCurrentSpan() {
    // given
    var headers = new HashMap<String, String>();

    // when
    ServerTimingHeader.setHeaders(Context.current(), headers, Map::put);

    // then
    assertTrue(headers.isEmpty());
  }

  @Test
  void shouldSetHeaders() {
    // given
    var headers = new HashMap<String, String>();

    // when
    var spanContext =
        TraceUtils.runUnderTrace(
            "server",
            () -> {
              ServerTimingHeader.setHeaders(Context.current(), headers, Map::put);
              return Span.current().getSpanContext();
            });

    // then
    assertEquals(2, headers.size());

    var serverTimingHeaderValue =
        "traceparent;desc=\"00-"
            + spanContext.getTraceIdAsHexString()
            + "-"
            + spanContext.getSpanIdAsHexString()
            + "-01\"";
    assertEquals(serverTimingHeaderValue, headers.get(ServerTimingHeader.SERVER_TIMING));
    assertEquals(ServerTimingHeader.SERVER_TIMING, headers.get(ServerTimingHeader.EXPOSE_HEADERS));
  }
}
