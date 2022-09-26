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

import static com.splunk.opentelemetry.SplunkConfiguration.OTEL_EXPORTER_JAEGER_ENDPOINT;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_ACCESS_TOKEN;

import com.google.auto.service.AutoService;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import org.apache.thrift.transport.TTransportException;

@AutoService(ConfigurableSpanExporterProvider.class)
@Deprecated
public class JaegerThriftSpanExporterFactory implements ConfigurableSpanExporterProvider {
  private static final Logger logger =
      Logger.getLogger(JaegerThriftSpanExporterFactory.class.getName());

  @Override
  public SpanExporter createExporter(ConfigProperties config) {
    logDeprecationWarning();
    JaegerThriftSpanExporterBuilder builder = JaegerThriftSpanExporter.builder();

    String endpoint = config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT);
    String token = config.getString(SPLUNK_ACCESS_TOKEN);
    if (token != null && !token.isEmpty()) {
      logger.fine("Using authenticated jaeger-thrift exporter");
      builder.setThriftSender(createHttpSender(endpoint, token));
    } else {
      logger.fine("Using jaeger-thrift exporter without authentication");
      builder.setEndpoint(endpoint);
    }

    return builder.build();
  }

  private void logDeprecationWarning() {
    logger.warning(
    "jaeger-thrift-splunk trace exporter is deprecated and may be removed in a future\n" +
         "major release. Use the default OTLP exporter instead, or set the SPLUNK_REALM\n" +
         "and SPLUNK_ACCESS_TOKEN environment variables to send telemetry directly to \n" +
         "Splunk Observability Cloud."
    );
  }

  private HttpSender createHttpSender(String endpoint, String token) {
    try {
      return new HttpSender.Builder(endpoint)
          .withClient(
              new OkHttpClient.Builder().addInterceptor(new AuthTokenInterceptor(token)).build())
          .build();
    } catch (TTransportException e) {
      throw new IllegalStateException("Could not create jaeger-thrift HttpSender", e);
    }
  }

  @Override
  public String getName() {
    return "jaeger-thrift-splunk";
  }
}
