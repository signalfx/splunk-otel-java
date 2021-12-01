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

package com.splunk.opentelemetry.profiler;

import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import java.util.function.Function;

/** Turns a linked stack into a new LogData instance */
public class LogDataCreator implements Function<StackToSpanLinkage, LogData> {

  static final String PROFILING_SOURCE = "otel.profiling";

  private final LogDataCommonAttributes commonAttributes;
  private final LogDataBuilder logDataBuilder;

  public LogDataCreator(LogDataCommonAttributes commonAttributes, LogDataBuilder logDataBuilder) {
    this.commonAttributes = commonAttributes;
    this.logDataBuilder = logDataBuilder;
  }

  @Override
  public LogData apply(StackToSpanLinkage linkedStack) {
    Attributes attributes = commonAttributes.build(linkedStack);

    logDataBuilder
        .setName(PROFILING_SOURCE)
        .setEpoch(linkedStack.getTime())
        .setBody(linkedStack.getRawStack())
        .setAttributes(attributes);
    if (linkedStack.hasSpanInfo()) {
      // TODO: Don't hop through this when upstream supports directly setting the span context on
      // the builder
      SpanContext spanContext = linkedStack.getSpanContext();
      Context context = Context.root().with(Span.wrap(spanContext));
      logDataBuilder.setContext(context);
    }
    return logDataBuilder.build();
  }
}
