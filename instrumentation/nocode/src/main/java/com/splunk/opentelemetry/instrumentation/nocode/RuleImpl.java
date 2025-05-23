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
import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.api.trace.SpanKind;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public final class RuleImpl implements NocodeRules.Rule {
  private static final AtomicInteger counter = new AtomicInteger();

  private final int id = counter.incrementAndGet();
  private final ElementMatcher<TypeDescription> classMatcher;
  private final ElementMatcher<MethodDescription> methodMatcher;
  private final NocodeExpression spanName; // may be null - use default of "class.method"
  private final SpanKind spanKind; // may be null
  private final NocodeExpression spanStatus; // may be null, should return string from StatusCodes

  private final Map<String, NocodeExpression> attributes; // key name to jexl expression

  public RuleImpl(
      ElementMatcher<TypeDescription> classMatcher, // ElementMatcher
      ElementMatcher<MethodDescription> methodMatcher, // ElementMatcher
      NocodeExpression spanName,
      SpanKind spanKind,
      NocodeExpression spanStatus,
      Map<String, NocodeExpression> attributes) {
    this.classMatcher = classMatcher;
    this.methodMatcher = methodMatcher;
    this.spanName = spanName;
    this.spanKind = spanKind;
    this.spanStatus = spanStatus;
    this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
  }

  public String toString() {
    return "Nocode rule: "
        + classMatcher
        + "."
        + methodMatcher
        + ":span_name="
        + spanName
        + ":span_kind="
        + spanKind
        + ":span_status="
        + spanStatus
        + ",attrs="
        + attributes;
  }

  @Override
  public int getId() {
    return id;
  }

  public ElementMatcher<TypeDescription> getClassMatcher() {
    return classMatcher;
  }

  public ElementMatcher<MethodDescription> getMethodMatcher() {
    return methodMatcher;
  }

  @Override
  public NocodeExpression getSpanName() {
    return spanName;
  }

  @Override
  public SpanKind getSpanKind() {
    return spanKind;
  }

  @Override
  public NocodeExpression getSpanStatus() {
    return spanStatus;
  }

  @Override
  public Map<String, NocodeExpression> getAttributes() {
    return attributes;
  }
}
