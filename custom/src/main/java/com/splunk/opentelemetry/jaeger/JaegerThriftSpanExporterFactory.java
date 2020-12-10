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

import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.javaagent.spi.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaegerThriftSpanExporterFactory implements SpanExporterFactory {
  private static final Logger log = LoggerFactory.getLogger(JaegerThriftSpanExporterFactory.class);

  static final String SIGNALFX_AUTH_TOKEN = "signalfx.auth.token";
  static final String OTEL_EXPORTER_JAEGER_ENDPOINT = "otel.exporter.jaeger.endpoint";

  @Override
  public SpanExporter fromConfig(Properties config) {
    JaegerThriftSpanExporter.Builder builder =
        JaegerThriftSpanExporter.builder().readProperties(config);

    String token = config.getProperty(SIGNALFX_AUTH_TOKEN, "");
    if (!token.isEmpty()) {
      log.debug("Using authenticated jaeger-thrift exporter");
      builder.setThriftSender(
          new HttpSender.Builder(config.getProperty(OTEL_EXPORTER_JAEGER_ENDPOINT))
              .withClient(
                  new OkHttpClient.Builder()
                      .addInterceptor(new AuthTokenInterceptor(token))
                      .build())
              .build());
    } else {
      log.debug("Using jaeger-thrift exporter without authentication");
    }

    return builder.build();
  }

  @Override
  public Set<String> getNames() {
    return Collections.singleton("jaeger-thrift-splunk");
  }
}
