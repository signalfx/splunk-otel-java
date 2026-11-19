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
  private static final AttributeSetter<AttributesBuilder> attributesBuilderSetter =
      new AttributeSetter<AttributesBuilder>() {
        @Override
        public void set(AttributesBuilder attributesBuilder, String key, long value) {
          attributesBuilder.put(key, value);
        }

        @Override
        public void set(AttributesBuilder attributesBuilder, String key, double value) {
          attributesBuilder.put(key, value);
        }

        @Override
        public void set(AttributesBuilder attributesBuilder, String key, boolean value) {
          attributesBuilder.put(key, value);
        }

        @Override
        public void set(AttributesBuilder attributesBuilder, String key, String value) {
          attributesBuilder.put(key, value);
        }
      };

  private static final AttributeSetter<Span> spanAttributeSetter =
      new AttributeSetter<Span>() {
        @Override
        public void set(Span span, String key, long value) {
          span.setAttribute(key, value);
        }

        @Override
        public void set(Span span, String key, double value) {
          span.setAttribute(key, value);
        }

        @Override
        public void set(Span span, String key, boolean value) {
          span.setAttribute(key, value);
        }

        @Override
        public void set(Span span, String key, String value) {
          span.setAttribute(key, value);
        }
      };

  private final AttributesExtractor<ClassAndMethod, Object> codeExtractor;

  public NocodeAttributesExtractor() {
    codeExtractor = CodeAttributesExtractor.create(ClassAndMethod.codeAttributesGetter());
  }

  @Override
  public void onStart(
      AttributesBuilder attributesBuilder, Context context, NocodeMethodInvocation invocation) {
    codeExtractor.onStart(attributesBuilder, context, invocation.getClassAndMethod());

    applyRuleAttributes(
        invocation,
        (key, value) -> setAttribute(attributesBuilderSetter, attributesBuilder, key, value));
  }

  static void applyToSpan(Span span, NocodeMethodInvocation invocation) {
    applyRuleAttributes(
        invocation, (key, value) -> setAttribute(spanAttributeSetter, span, key, value));
  }

  private static void applyRuleAttributes(
      NocodeMethodInvocation invocation, BiConsumer<String, Object> attributeConsumer) {
    Map<String, NocodeExpression> attributes = invocation.getRuleAttributes();
    for (Map.Entry<String, NocodeExpression> entry : attributes.entrySet()) {
      Object value = invocation.evaluate(entry.getValue());
      if (value != null) {
        attributeConsumer.accept(entry.getKey(), value);
      }
    }
  }

  private static <T> void setAttribute(
      AttributeSetter<T> setter, T target, String key, Object value) {
    if (value instanceof Long
        || value instanceof Integer
        || value instanceof Short
        || value instanceof Byte) {
      setter.set(target, key, ((Number) value).longValue());
    } else if (value instanceof Float || value instanceof Double) {
      setter.set(target, key, ((Number) value).doubleValue());
    } else if (value instanceof Boolean) {
      setter.set(target, key, (Boolean) value);
    } else {
      setter.set(target, key, value.toString());
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

  private interface AttributeSetter<T> {
    void set(T target, String key, long value);

    void set(T target, String key, double value);

    void set(T target, String key, boolean value);

    void set(T target, String key, String value);
  }
}
