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

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import java.util.Collections;
import java.util.Map;

public final class NocodeMethodInvocation {
  private final NocodeRules.Rule rule;
  private final ClassAndMethod classAndMethod;
  private final Object thiz;
  private final Object[] parameters;

  public NocodeMethodInvocation(
      NocodeRules.Rule rule, ClassAndMethod cm, Object thiz, Object[] parameters) {
    this.rule = rule;
    this.classAndMethod = cm;
    this.thiz = thiz;
    this.parameters = parameters;
  }

  public NocodeRules.Rule getRule() {
    return rule;
  }

  public Object getThiz() {
    return thiz;
  }

  /**
   * Please be careful with this, it's directly tied to @Advice.AllArguments.
   *
   * @return @Advice.AllArguments - please be careful
   */
  public Object[] getParameters() {
    return parameters;
  }

  public ClassAndMethod getClassAndMethod() {
    return classAndMethod;
  }

  public Map<String, String> getRuleAttributes() {
    return rule == null ? Collections.emptyMap() : rule.attributes;
  }
}
