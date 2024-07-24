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

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import io.opentelemetry.javaagent.bootstrap.internal.JavaagentHttpClientInstrumenters;
import khttp.responses.Response;

public final class KHttpSingletons {
  private static final String INSTRUMENTATION_NAME = "com.splunk.khttp";

  private static final Instrumenter<RequestWrapper, Response> INSTRUMENTER;

  static {
    HttpClientAttributesGetter<RequestWrapper, Response> httpAttributesGetter =
        new KHttpHttpClientHttpAttributesGetter();

    INSTRUMENTER =
        JavaagentHttpClientInstrumenters.create(
            INSTRUMENTATION_NAME,
            new KHttpHttpClientHttpAttributesGetter(),
            KHttpHttpHeaderSetter.INSTANCE);
  }

  public static Instrumenter<RequestWrapper, Response> instrumenter() {
    return INSTRUMENTER;
  }

  private KHttpSingletons() {}
}
