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

import java.util.concurrent.atomic.AtomicReference;

public class NocodeEvaluation {

  public interface Evaluator {
    Object evaluate(String expression, Object thiz, Object[] params);
  }

  private static final AtomicReference<Evaluator> globalEvaluator = new AtomicReference<>();

  public static void internalSetEvaluator(Evaluator evaluator) {
    globalEvaluator.set(evaluator);
  }

  public static Object evaluate(String expression, Object thiz, Object[] params) {
    Evaluator e = globalEvaluator.get();
    return e == null ? null : e.evaluate(expression, thiz, params);
  }

  private NocodeEvaluation() {}
}
