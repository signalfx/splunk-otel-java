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
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.Map;
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

    Map<String, NocodeExpression> attributes = mi.getRuleAttributes();
    for (String key : attributes.keySet()) {
      NocodeExpression expression = attributes.get(key);
      Object value = mi.evaluate(expression);
      if (value instanceof Long
          || value instanceof Integer
          || value instanceof Short
          || value instanceof Byte) {
        attributesBuilder.put(key, ((Number) value).longValue());
      } else if (value instanceof Float || value instanceof Double) {
        attributesBuilder.put(key, ((Number) value).doubleValue());
      } else if (value instanceof Boolean) {
        attributesBuilder.put(key, (Boolean) value);
      } else if (value != null) {
        attributesBuilder.put(key, value.toString());
      }
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
