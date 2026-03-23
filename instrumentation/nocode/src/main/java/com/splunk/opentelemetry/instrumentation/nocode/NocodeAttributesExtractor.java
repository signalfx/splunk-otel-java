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

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeExpression;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

class NocodeAttributesExtractor implements AttributesExtractor<NocodeMethodInvocation, Object> {
  private final AttributesExtractor<ClassAndMethod, Object> codeExtractor;

  public NocodeAttributesExtractor() {
    codeExtractor = CodeAttributesExtractor.create(ClassAndMethod.codeAttributesGetter());
  }

  @Override
  public void onStart(
      AttributesBuilder attributesBuilder, Context context, NocodeMethodInvocation mi) {
    codeExtractor.onStart(attributesBuilder, context, mi.getClassAndMethod());

    applyRuleAttributes(mi, (key, value) -> putAttribute(attributesBuilder, key, value));
  }

  static void applyToSpan(Span span, NocodeMethodInvocation mi) {
    applyRuleAttributes(mi, (key, value) -> setAttribute(span, key, value));
  }

  private static void applyRuleAttributes(
      NocodeMethodInvocation mi, BiConsumer<String, Object> attributeConsumer) {
    Map<String, NocodeExpression> attributes = mi.getRuleAttributes();
    for (Map.Entry<String, NocodeExpression> entry : attributes.entrySet()) {
      Object value = mi.evaluate(entry.getValue());
      if (value != null) {
        attributeConsumer.accept(entry.getKey(), value);
      }
    }
  }

  // The duplication between these two methods is unfortunate but kinda unavodable without
  // introducing an abstracting interface over the two ways the otel apis work
  private static void putAttribute(AttributesBuilder attributesBuilder, String key, Object value) {
    if (value instanceof Long
        || value instanceof Integer
        || value instanceof Short
        || value instanceof Byte) {
      attributesBuilder.put(key, ((Number) value).longValue());
    } else if (value instanceof Float || value instanceof Double) {
      attributesBuilder.put(key, ((Number) value).doubleValue());
    } else if (value instanceof Boolean) {
      attributesBuilder.put(key, (Boolean) value);
    } else {
      attributesBuilder.put(key, value.toString());
    }
  }

  private static void setAttribute(Span span, String key, Object value) {
    if (value instanceof Long
        || value instanceof Integer
        || value instanceof Short
        || value instanceof Byte) {
      span.setAttribute(key, ((Number) value).longValue());
    } else if (value instanceof Float || value instanceof Double) {
      span.setAttribute(key, ((Number) value).doubleValue());
    } else if (value instanceof Boolean) {
      span.setAttribute(key, (Boolean) value);
    } else {
      span.setAttribute(key, value.toString());
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributesBuilder,
      Context context,
      NocodeMethodInvocation nocodeMethodInvocation,
      @Nullable Object unused,
      @Nullable Throwable throwable) {
    codeExtractor.onEnd(
        attributesBuilder, context, nocodeMethodInvocation.getClassAndMethod(), unused, throwable);
  }
}
