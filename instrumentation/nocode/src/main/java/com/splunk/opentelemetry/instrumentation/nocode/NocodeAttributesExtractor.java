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
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.Map;
import javax.annotation.Nullable;

public final class NocodeAttributesExtractor
    implements AttributesExtractor<NocodeMethodInvocation, Void> {
  private final AttributesExtractor<ClassAndMethod, Void> codeExtractor;

  public NocodeAttributesExtractor() {
    codeExtractor = CodeAttributesExtractor.create(ClassAndMethod.codeAttributesGetter());
  }

  @Override
  public void onStart(
      AttributesBuilder attributesBuilder, Context context, NocodeMethodInvocation mi) {
    codeExtractor.onStart(attributesBuilder, context, mi.getClassAndMethod());

    Map<String, String> attributes = mi.getRuleAttributes();
    for (String key : attributes.keySet()) {
      String expression = attributes.get(key);
      String value = NocodeEvaluation.evaluate(expression, mi.getThiz(), mi.getParameters());
      if (value != null) {
        attributesBuilder.put(key, value);
      }
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributesBuilder,
      Context context,
      NocodeMethodInvocation nocodeMethodInvocation,
      @Nullable Void unused,
      @Nullable Throwable throwable) {
    codeExtractor.onEnd(
        attributesBuilder, context, nocodeMethodInvocation.getClassAndMethod(), unused, throwable);
  }
}
