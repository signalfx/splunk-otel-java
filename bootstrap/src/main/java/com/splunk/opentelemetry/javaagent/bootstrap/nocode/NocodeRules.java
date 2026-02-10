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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NocodeRules {
  public interface Rule {

    int getId();

    NocodeExpression getSpanName();

    SpanKind getSpanKind();

    NocodeExpression getSpanStatus();

    Map<String, NocodeExpression> getAttributes();
  }

  private NocodeRules() {}

  private static final HashMap<Integer, Rule> ruleMap = new HashMap<>();

  // Called by the NocodeInitializer
  public static void setGlobalRules(List<Rule> rules) {
    for (Rule r : rules) {
      ruleMap.put(r.getId(), r);
    }
  }

  public static Iterable<Rule> getGlobalRules() {
    return ruleMap.values();
  }

  public static Rule find(int id) {
    return ruleMap.get(id);
  }

  public static void clearGlobalRules() {
    ruleMap.clear();
  }
}
