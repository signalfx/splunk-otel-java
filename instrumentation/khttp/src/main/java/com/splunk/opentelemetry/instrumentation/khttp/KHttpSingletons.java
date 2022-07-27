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

package com.splunk.opentelemetry.instrumentation.khttp;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;
import khttp.responses.Response;

public final class KHttpSingletons {
  private static final String INSTRUMENTATION_NAME = "com.splunk.javaagent.khttp-0.1";

  private static final Instrumenter<RequestWrapper, Response> INSTRUMENTER;

  static {
    KHttpHttpClientNetAttributesGetter netAttributesGetter =
        new KHttpHttpClientNetAttributesGetter();
    HttpClientAttributesGetter<RequestWrapper, Response> httpAttributesGetter =
        new KHttpHttpClientHttpAttributesGetter();

    INSTRUMENTER =
        Instrumenter.<RequestWrapper, Response>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(httpAttributesGetter))
            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(httpAttributesGetter))
            .addAttributesExtractor(HttpClientAttributesExtractor.create(httpAttributesGetter))
            .addAttributesExtractor(NetClientAttributesExtractor.create(netAttributesGetter))
            .addAttributesExtractor(
                PeerServiceAttributesExtractor.create(
                    netAttributesGetter, CommonConfig.get().getPeerServiceMapping()))
            .addOperationMetrics(HttpClientMetrics.get())
            .buildClientInstrumenter(KHttpHttpHeaderSetter.INSTANCE);
  }

  public static Instrumenter<RequestWrapper, Response> instrumenter() {
    return INSTRUMENTER;
  }

  private KHttpSingletons() {}
}
