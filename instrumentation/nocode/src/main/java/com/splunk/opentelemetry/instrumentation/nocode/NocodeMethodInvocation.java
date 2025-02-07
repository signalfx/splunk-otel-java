package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.nocode.NocodeRules;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;

public class NocodeMethodInvocation {
  private final NocodeRules.Rule rule;
  private final ClassAndMethod classAndMethod;
  private final Object thiz;
  private final Object[] parameters;

  public NocodeMethodInvocation(NocodeRules.Rule rule, ClassAndMethod cm, Object thiz, Object[] parameters) {
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

  public Object[] getParameters() {
    return parameters;
  }

  public ClassAndMethod getClassAndMethod() {
    return classAndMethod;
  }
}
