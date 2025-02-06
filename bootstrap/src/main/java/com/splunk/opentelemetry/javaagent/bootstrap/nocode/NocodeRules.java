package com.splunk.opentelemetry.javaagent.nocode;

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
    public final Map<String, String> attributes; // key name to jsps
    public Rule(String className, String methodName, Map<String,String> attributes) {
      this.className = className;
      this.methodName = methodName;
      this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }
    public Map<String, String> getAttributes() {
      return attributes;
    }
    public String toString() {
      return className+"."+methodName+"=attrs:"+attributes;
    }
  }

  // FIXME this is awkward atomicity due to unique nature of initialization and access patterns.
  // revisit if this placement actually works
  private static final AtomicReference<List<Rule>> GlobalRules = new AtomicReference<>(Collections.EMPTY_LIST);
  private static final AtomicReference<Map<String,Map<String,Rule>>> c2m2r = new AtomicReference<>(Collections.emptyMap());

  public static void setGlobalRules(List<Rule> rules) {
    GlobalRules.set(Collections.unmodifiableList(new ArrayList<>(rules)));
    Map<String, Map<String, Rule>> lookups = new HashMap<>();
    for(Rule r : rules) {
      Map<String, Rule> method2rule = lookups.computeIfAbsent(r.className, k -> new HashMap<>());
      method2rule.put(r.methodName, r);
    }
    // FIXME awkward structure, particularly since subfields aren't easily marked unmodifiable
    c2m2r.set(Collections.unmodifiableMap(lookups));
  }

  public static List<Rule> getGlobalRules() {
    return GlobalRules.get();
  }

  public static Rule findRuleByClassAndMethod(String className, String methodName) {
    Map<String,Map<String,Rule>> lookups = c2m2r.get();
    Map<String, Rule> m2r = lookups.get(className);
    if (m2r == null) {
      System.out.println("JBLEY NO M2R");
      return null;
    }
    return m2r.get(methodName);
  }


}
