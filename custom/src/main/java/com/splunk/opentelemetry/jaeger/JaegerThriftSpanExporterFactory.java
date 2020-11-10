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

package com.splunk.opentelemetry.jaeger;

import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.javaagent.spi.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

// TODO remove when https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1609
// is implemented
public class JaegerThriftSpanExporterFactory implements SpanExporterFactory {
  @Override
  public SpanExporter fromConfig(Properties config) {
    return JaegerThriftSpanExporter.builder().readProperties(config).build();
  }

  @Override
  public Set<String> getNames() {
    return Collections.singleton("jaeger-thrift");
  }
}
