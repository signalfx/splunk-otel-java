package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeEvaluation;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import java.util.logging.Logger;

public class JexlEvaluator implements NocodeEvaluation.Evaluator {
  private static final Logger logger = Logger.getLogger(JexlEvaluator.class.getName());

  private final JexlEngine jexl;

  public JexlEvaluator() {
    JexlFeatures features = new JexlFeatures()
        .register(false)
        .comparatorNames(false);
    // The "unrestricted" permissions allows jexl to introspect on custom/non-stdlib classes
    // for calling methods
    this.jexl = new JexlBuilder().features(features).permissions(JexlPermissions.UNRESTRICTED).antish(false).create();
  }


  @Override
  public String evaluate(String expression, Object thiz, Object[] params) {
    JexlContext context = new MapContext();
    context.set("this", thiz);
    for(int i=0; i < params.length; i++) {
      context.set("param"+i, params[i]);
    }
    try {
      // could cache the Expression in the Rule if desired
      Object result = jexl.createExpression(expression).evaluate(context);
      return result == null ? null : result.toString();
    } catch (Throwable t) {
      logger.warning("Can't evaluate {" + expression + "}: " + t);
      return null;
    }
  }
}
