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

package com.splunk.opentelemetry.javaagent.bootstrap.nocode;

import io.opentelemetry.api.trace.SpanKind;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class NocodeRules {

  public static final class Rule {
    private static final AtomicInteger counter = new AtomicInteger();

    private final int id = counter.incrementAndGet();
    private final Object classMatcher;
    private final Object methodMatcher;
    private final NocodeExpression spanName; // may be null - use default of "class.method"
    private final SpanKind spanKind; // may be null
    private final NocodeExpression spanStatus; // may be null, should return string from StatusCodes

    private final Map<String, NocodeExpression> attributes; // key name to jexl expression

    public Rule(
        Object classMatcher, // ElementMatcher
        Object methodMatcher, // ElementMatcher
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
          + ":spanName="
          + spanName
          + ":spanKind="
          + spanKind
          + ":spanStatus="
          + spanStatus
          + ",attrs="
          + attributes;
    }

    public int getId() {
      return id;
    }

    public Object getClassMatcher() {
      return classMatcher;
    }

    public Object getMethodMatcher() {
      return methodMatcher;
    }

    public NocodeExpression getSpanName() {
      return spanName;
    }

    public SpanKind getSpanKind() {
      return spanKind;
    }

    public NocodeExpression getSpanStatus() {
      return spanStatus;
    }

    public Map<String, NocodeExpression> getAttributes() {
      return attributes;
    }
  }

  private NocodeRules() {}

  private static final HashMap<Integer, Rule> ruleMap = new HashMap<>();

  // Called by the NocodeInitializer
  public static void setGlobalRules(List<Rule> rules) {
    for (Rule r : rules) {
      ruleMap.put(r.id, r);
    }
  }

  public static Iterable<Rule> getGlobalRules() {
    return ruleMap.values();
  }

  public static Rule find(int id) {
    return ruleMap.get(id);
  }
}
