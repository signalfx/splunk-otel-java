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

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeEvaluation;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import java.util.Locale;
import javax.annotation.Nullable;

public class NocodeSpanStatusExtractor
    implements SpanStatusExtractor<NocodeMethodInvocation, Object> {

  @Override
  public void extract(
      SpanStatusBuilder spanStatusBuilder,
      NocodeMethodInvocation mi,
      @Nullable Object returnValue,
      @Nullable Throwable error) {

    if (mi.getRule() == null || mi.getRule().spanStatus == null) {

      // FIXME would love to use a DefaultSpanStatusExtractor as a fallback but it is not public
      // so here is a copy of its (admittedly simple) guts
      if (error != null) {
        spanStatusBuilder.setStatus(StatusCode.ERROR);
      }
      return;
    }
    Object status =
        NocodeEvaluation.evaluateAtEnd(
            mi.getRule().spanStatus, mi.getThiz(), mi.getParameters(), returnValue, error);
    if (status != null) {
      try {
        StatusCode code = StatusCode.valueOf(status.toString().toUpperCase(Locale.ROOT));
        spanStatusBuilder.setStatus(code);
      } catch (IllegalArgumentException noMatchingValue) {
        // nop, should remain UNSET
      }
    }
  }
}
