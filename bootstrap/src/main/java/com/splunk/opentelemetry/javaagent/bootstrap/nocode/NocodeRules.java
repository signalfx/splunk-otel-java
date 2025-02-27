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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NocodeRules {

  public static final class Rule {
    public final String className;
    public final String methodName;
    public final String spanName; // may be null - use default of "class.method"
    public final String spanKind; // matches the SpanKind enum, null means default to INTERNAL
    public final Map<String, String> attributes; // key name to jexl expression

    public Rule(
        String className,
        String methodName,
        String spanName,
        String spanKind,
        Map<String, String> attributes) {
      this.className = className;
      this.methodName = methodName;
      this.spanName = spanName;
      this.spanKind = spanKind;
      this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public String toString() {
      return "Nocode rule: "
          + className
          + "."
          + methodName
          + ":spanName="
          + spanName
          + ",attrs="
          + attributes;
    }
  }

  private NocodeRules() {}

  // Using className.methodName as the key
  private static final HashMap<String, Rule> name2Rule = new HashMap<>();

  // Called by the NocodeInitializer
  public static void setGlobalRules(List<Rule> rules) {
    for (Rule r : rules) {
      name2Rule.put(r.className + "." + r.methodName, r);
    }
  }

  public static Iterable<Rule> getGlobalRules() {
    return name2Rule.values();
  }

  public static Rule find(String className, String methodName) {
    return name2Rule.get(className + "." + methodName);
  }
}
