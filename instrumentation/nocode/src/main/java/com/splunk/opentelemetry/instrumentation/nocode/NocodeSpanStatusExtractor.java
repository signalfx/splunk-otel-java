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

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import java.util.Locale;
import java.util.logging.Logger;
import javax.annotation.Nullable;

class NocodeSpanStatusExtractor
    implements SpanStatusExtractor<NocodeMethodInvocation, Object> {
  private static final Logger logger = Logger.getLogger(NocodeSpanStatusExtractor.class.getName());

  @Override
  public void extract(
      SpanStatusBuilder spanStatusBuilder,
      NocodeMethodInvocation mi,
      @Nullable Object returnValue,
      @Nullable Throwable error) {

    if (mi.getRule() == null || mi.getRule().getSpanStatus() == null) {
      SpanStatusExtractor.getDefault().extract(spanStatusBuilder, mi, returnValue, error);
      return;
    }
    Object status = mi.evaluateAtEnd(mi.getRule().getSpanStatus(), returnValue, error);
    if (status != null) {
      try {
        StatusCode code = StatusCode.valueOf(status.toString().toUpperCase(Locale.ROOT));
        spanStatusBuilder.setStatus(code);
      } catch (IllegalArgumentException noMatchingValue) {
        // nop, should remain UNSET
        logger.fine("Invalid span status ignored: " + status);
      }
    }
  }
}
