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

import static com.splunk.opentelemetry.servertiming.ServerTimingHeaderCustomizer.EXPOSE_HEADERS;
import static com.splunk.opentelemetry.servertiming.ServerTimingHeaderCustomizer.SERVER_TIMING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ServerTimingHeaderTest {

  @RegisterExtension InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  private final ServerTimingHeaderCustomizer serverTiming = new ServerTimingHeaderCustomizer();

  @BeforeAll
  static void setUp() {
    ServerTimingHeaderCustomizer.enabled = true;
  }

  @Test
  void shouldNotSetAnyHeadersWithoutValidCurrentSpan() {
    // given
    var headers = new HashMap<String, String>();

    // when
    serverTiming.customize(Context.root(), headers, Map::put);

    // then
    assertTrue(headers.isEmpty());
  }

  @Test
  void shouldSetHeaders() {
    // given
    var headers = new HashMap<String, String>();

    // when
    var spanContext =
        testing.runWithSpan(
            "server",
            () -> {
              serverTiming.customize(Context.current(), headers, Map::put);
              return Span.current().getSpanContext();
            });

    // then
    assertEquals(2, headers.size());

    var serverTimingHeaderValue =
        "traceparent;desc=\"00-"
            + spanContext.getTraceId()
            + "-"
            + spanContext.getSpanId()
            + "-01\"";
    assertEquals(serverTimingHeaderValue, headers.get(SERVER_TIMING));
    assertEquals(SERVER_TIMING, headers.get(EXPOSE_HEADERS));
  }
}
