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
import java.util.logging.Logger;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;

public class JexlEvaluator implements NocodeEvaluation.Evaluator {
  private static final Logger logger = Logger.getLogger(JexlEvaluator.class.getName());

  private final JexlEngine jexl;

  public JexlEvaluator() {
    JexlFeatures features =
        new JexlFeatures()
            .register(false) // don't support #register syntax
            .comparatorNames(false); // don't support 'lt' as an alternative to '<'
    this.jexl =
        new JexlBuilder()
            .features(features)
            // "unrestricted" means "can introspect on custom classes"
            .permissions(JexlPermissions.UNRESTRICTED)
            // don't support ant syntax
            .antish(false)
            // This api is terribly named but false means "null deref throws exception rather than
            // log warning"
            .safe(false)
            // We will catch our own exceptions
            .silent(false)
            // Don't assume unknown methods/variables mean "null"
            .strict(true)
            .create();
  }

  private static void setBeginningVariables(JexlContext context, Object thiz, Object[] params) {
    context.set("this", thiz);
    for (int i = 0; i < params.length; i++) {
      context.set("param" + i, params[i]);
    }
  }

  private Object evaluateExpression(String expression, JexlContext context) {
    try {
      // could cache the Expression in the Rule if desired
      return jexl.createExpression(expression).evaluate(context);
    } catch (Throwable t) {
      logger.warning("Can't evaluate {" + expression + "}: " + t);
      return null;
    }
  }

  private void setEndingVariables(JexlContext context, Object returnValue, Throwable error) {
    context.set("returnValue", returnValue);
    context.set("error", error);
  }

  @Override
  public Object evaluate(String expression, Object thiz, Object[] params) {
    JexlContext context = new MapContext();
    setBeginningVariables(context, thiz, params);
    return evaluateExpression(expression, context);
  }

  @Override
  public Object evaluateAtEnd(
      String expression, Object thiz, Object[] params, Object returnValue, Throwable error) {
    JexlContext context = new MapContext();
    setBeginningVariables(context, thiz, params);
    setEndingVariables(context, returnValue, error);
    return evaluateExpression(expression, context);
  }
}
