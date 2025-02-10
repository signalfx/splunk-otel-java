package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlParser {
  // FIXME support method override selection - e.g., with classfile method signature or something
  public static final String NOCODE_YMLFILE_ENV_KEY = "SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE";

  private final List<NocodeRules.Rule> InstrumentationRules;

  public YamlParser() {
    InstrumentationRules = Collections.unmodifiableList(new ArrayList<>(load()));
  }

  public List<NocodeRules.Rule> getInstrumentationRules() {
    return InstrumentationRules;
  }

  private static List<NocodeRules.Rule> load() {
    try {
      return loadUnsafe();
    } catch (Exception e) {
      // FIXME error handling
      System.out.println("JBLEY can't load yaml: "+e);
      return Collections.emptyList();
    }
  }

  private static List<NocodeRules.Rule> loadUnsafe() throws Exception {
    String yamlFileName = System.getenv(NOCODE_YMLFILE_ENV_KEY);
    if (yamlFileName == null || yamlFileName.trim().isEmpty()) {
      return Collections.emptyList();
    }
    Reader yamlReader = new FileReader(yamlFileName.trim());


    // FIXME why can't I figure out how to reference the snakeyaml that's already inside the agent jar?
    // This nonsense is here to do a reflective load of the yaml parser that's already there
    // and parse the config file
    Class loadSettingsC = Class.forName("org.snakeyaml.engine.v2.api.LoadSettings");
    Class loadC = Class.forName("org.snakeyaml.engine.v2.api.Load");
    Object lsb = loadSettingsC.getMethod("builder").invoke(null);
    Object loadSettings = lsb.getClass().getMethod("build").invoke(lsb);
    Object load = loadC.getConstructor(loadSettingsC).newInstance(loadSettings);
    Iterable<Object> parsedYaml = (Iterable<Object>) load.getClass().getMethod("loadAllFromReader", Reader.class).invoke(load, yamlReader);
    ArrayList<NocodeRules.Rule> answer = new ArrayList<>();
    for(Object yamlBit : parsedYaml) {
      List l = (List) yamlBit;
      for(Object sub : l) {
        LinkedHashMap lhm = (LinkedHashMap) sub;
        String className = lhm.get("class").toString();
        String methodName = lhm.get("method").toString();
        String spanName = lhm.get("spanName") == null ? null : lhm.get("spanName").toString();
        String spanKind = lhm.get("spanKind") == null ? null : lhm.get("spanKind").toString();
        List attrs = (List) lhm.get("attributes");
        Map<String, String> ruleAttributes = new HashMap<>();
        for(Object attr : attrs) {
          LinkedHashMap attrMap = (LinkedHashMap) attr;
          ruleAttributes.put(attrMap.get("key").toString(), attrMap.get("value").toString());
        }
        answer.add(new NocodeRules.Rule(className, methodName, spanName, spanKind, ruleAttributes));
      }
    }
    yamlReader.close();
    return answer;
  }



}
