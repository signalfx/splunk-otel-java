package com.splunk.opentelemetry.javaagent.bootstrap.nocode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class NocodeRules {

  public static class Rule {
    public final String className;
    public final String methodName;
    public final String spanName; // may be null - use default of "class.method"
    public final String spanKind; // matches the SpanKind enum, null means default to INTERNAL
    public final Map<String, String> attributes; // key name to jsps
    public Rule(String className, String methodName, String spanName, String spanKind, Map<String,String> attributes) {
      this.className = className;
      this.methodName = methodName;
      this.spanName = spanName;
      this.spanKind = spanKind;
      this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }
    public String toString() {
      return className+"."+methodName+":spanName="+spanName+",attrs="+attributes;
    }
  }

  // Unfortunately the particular sequence of who needs access to this and who can create (based on
  // having the yaml parser library loaded) is awkward.  Would prefer this to be a simple
  // static final immutable computed at startup...
  private static final AtomicReference<List<Rule>> GlobalRules = new AtomicReference<>(Collections.EMPTY_LIST);
  // Using className.methodName as the key
  private static final AtomicReference<Map<String,Rule>> Name2Rule = new AtomicReference<>(Collections.emptyMap());

  public static void setGlobalRules(List<Rule> rules) {
    GlobalRules.set(Collections.unmodifiableList(new ArrayList<>(rules)));
    Map<String, Rule> n2r = new HashMap<>();
    for(Rule r : rules) {
      n2r.put(r.className + "." + r.methodName, r);
    }
    Name2Rule.set(Collections.unmodifiableMap(n2r));
  }

  public static List<Rule> getGlobalRules() {
    return GlobalRules.get();
  }

  public static Rule findRuleByClassAndMethod(String className, String methodName) {
    Map<String,Rule> n2r = Name2Rule.get();
    return n2r == null ? null : n2r.get(className + "." + methodName);
  }


}
