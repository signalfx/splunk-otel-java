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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import java.util.Locale;

public class NocodeSpanKindExtractor implements SpanKindExtractor<NocodeMethodInvocation> {
  @Override
  public SpanKind extract(NocodeMethodInvocation mi) {
    if (mi.getRule() == null || mi.getRule().spanKind == null) {
      return SpanKind.INTERNAL;
    }
    try {
      return SpanKind.valueOf(mi.getRule().spanKind.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException noMatchingValue) {
      return SpanKind.INTERNAL;
    }
  }
}
