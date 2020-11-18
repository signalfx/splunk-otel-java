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

import static com.splunk.opentelemetry.DefaultConfig.setDefaultConfig;
import static java.lang.String.valueOf;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;

public class SplunkAgent {
  public static void premain(final String agentArgs, final Instrumentation inst) {
    agentmain(agentArgs, inst);
  }

  public static void agentmain(final String agentArgs, final Instrumentation inst) {
    setDefaultConfig("otel.exporter", "jaeger-thrift");
    // http://localhost:9080/v1/trace is the default endpoint for SmartAgent
    // http://localhost:14268/api/traces is the default endpoint for otel-collector
    setDefaultConfig("otel.exporter.jaeger.endpoint", "http://localhost:9080/v1/trace");
    setDefaultConfig("otel.propagators", "b3multi");

    String max = valueOf(Integer.MAX_VALUE);
    setDefaultConfig("otel.config.max.attrs", max);
    setDefaultConfig("otel.config.max.event.attrs", max);
    setDefaultConfig("otel.config.max.link.attrs", max);

    // events and links create collections with provided sizes, so we shouldn't set them too high
    setDefaultConfig("otel.config.max.events", "256");
    setDefaultConfig("otel.config.max.links", "256");
    // -1 here means no attribute length limit
    setDefaultConfig("otel.config.max.attr.length", "-1");

    OpenTelemetryAgent.agentmain(agentArgs, inst);
  }
}
