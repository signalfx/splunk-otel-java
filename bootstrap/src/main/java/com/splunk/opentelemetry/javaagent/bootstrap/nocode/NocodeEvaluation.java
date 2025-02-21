package com.splunk.opentelemetry.javaagent.bootstrap.nocode;

import java.util.concurrent.atomic.AtomicReference;

public class NocodeEvaluation {

  public interface Evaluator {
    String evaluate(String expression, Object thiz, Object[] params);
  }

  private static final AtomicReference<Evaluator> globalEvaluator = new AtomicReference<>();

  public static void internalSetEvaluator(Evaluator evaluator) {
    globalEvaluator.set(evaluator);
  }

  public static String evaluate(String expression, Object thiz, Object[] params) {
    Evaluator e = globalEvaluator.get();
    return e == null ? null : e.evaluate(expression, thiz, params);
  }

  private NocodeEvaluation() {}

}
