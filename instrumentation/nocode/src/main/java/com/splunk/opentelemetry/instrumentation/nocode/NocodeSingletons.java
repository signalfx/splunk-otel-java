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

package com.splunk.opentelemetry.instrumentation.nocode;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

public class NocodeSingletons {
  private static final Instrumenter<NocodeMethodInvocation, Void> INSTRUMENTER;

  static {
    INSTRUMENTER =
        Instrumenter.<NocodeMethodInvocation, Void>builder(
                GlobalOpenTelemetry.get(), "com.splunk.nocode", new NocodeSpanNameExtractor())
            .addAttributesExtractor(new NocodeAttributesExtractor())
            .buildInstrumenter(new NocodeSpanKindExtractor());
  }

  public static Instrumenter<NocodeMethodInvocation, Void> instrumenter() {
    return INSTRUMENTER;
  }
}
