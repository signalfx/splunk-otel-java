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

package com.splunk.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JaegerAndB3SpringBootSmokeTest extends SpringBootSmokeTest {
  protected Map<String, String> getExtraEnv() {
    Map<String, String> extraEnv = new HashMap<>(super.getExtraEnv());
    extraEnv.put("OTEL_TRACES_EXPORTER", "jaeger-thrift-splunk");
    extraEnv.put("OTEL_EXPORTER_JAEGER_ENDPOINT", "http://collector:14268/api/traces");
    extraEnv.put("OTEL_PROPAGATORS", "b3multi");
    return extraEnv;
  }

  protected void assertTraces(TraceInspector traces) throws IOException {
    // verify spans are exported
    assertEquals(1, traces.countSpansByName("/greeting"));
    assertEquals(1, traces.countSpansByName("WebController.greeting"));
    assertEquals(1, traces.countSpansByName("WebController.withSpan"));

    // verify that correct agent version is set in the resource
    String currentAgentVersion = getOtelInstrumentationVersion();
    assertEquals(3, traces.countFilteredAttributes("otel.library.version", currentAgentVersion));

    // verify that correct service name is set in the resource
    assertTrue(traces.resourceExists("service.name", "smoke-test-app"));
  }
}
