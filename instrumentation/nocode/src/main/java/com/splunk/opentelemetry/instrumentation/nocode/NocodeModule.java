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

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.ArrayList;
import java.util.List;

@AutoService(InstrumentationModule.class)
public final class NocodeModule extends InstrumentationModule {

  public NocodeModule() {
    super("nocode");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    List<TypeInstrumentation> answer = new ArrayList<>();
    for (NocodeRules.Rule rule : NocodeRules.getGlobalRules()) {
      answer.add(new NocodeInstrumentation((RuleImpl) rule));
    }
    // ensure that there is at least one instrumentation so that muzzle reference collection could
    // work
    if (answer.isEmpty()) {
      answer.add(new NocodeInstrumentation(null));
    }
    return answer;
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.splunk.opentelemetry.instrumentation");
  }

  // If nocode instrumentation is added to something with existing auto-instrumentation,
  // it would generally be better to run the nocode bits after the "regular" bits.
  // E.g., if we want to add nocode to a servlet call, then we want to make sure that
  // the otel-standard servlet instrumentation runs first to handle context propagation, etc.
  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }
}
