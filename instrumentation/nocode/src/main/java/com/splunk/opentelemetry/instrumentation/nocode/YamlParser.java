package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.nocode.NocodeRules;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlParser {

  public static final String NOCODE_YMLFILE_ENV_KEY = "SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE";

  private final List<NocodeRules.Rule> InstrumentationRules;

  public YamlParser() {
    InstrumentationRules = Collections.unmodifiableList(new ArrayList<>(load()));
  }

  public List<NocodeRules.Rule> getInstrumentationRules() {
    return InstrumentationRules;
  }

  // FIXME surely there is a utility for this somewhere in the agent's scope (or just use Reader in snakeyaml)
  private static String readYamlFile(String yamlFileName) throws Exception {
    File f = new File(yamlFileName);
    BufferedReader br = new BufferedReader(new FileReader(f));
    String line;
    StringBuffer buf = new StringBuffer();
    while((line = br.readLine()) != null) {
      buf.append(line);
      buf.append(System.lineSeparator());
    }
    br.close();
    return buf.toString();
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
    String yamlString = readYamlFile(yamlFileName.trim());


    // FIXME why can't I figure out how to reference the snakeyaml that's already inside the agent jar?
    // This nonsense is here to do a reflective load of the yaml parser that's already there
    // and parse the config file
    Class loadSettingsC = Class.forName("org.snakeyaml.engine.v2.api.LoadSettings");
    Class loadC = Class.forName("org.snakeyaml.engine.v2.api.Load");
    Object lsb = loadSettingsC.getMethod("builder").invoke(null);
    Object loadSettings = lsb.getClass().getMethod("build").invoke(lsb);
    Object load = loadC.getConstructor(loadSettingsC).newInstance(loadSettings);
    Iterable<Object> parsedYaml = (Iterable<Object>) load.getClass().getMethod("loadAllFromString", String.class).invoke(load, yamlString);
    ArrayList<NocodeRules.Rule> answer = new ArrayList<>();
    for(Object yamlBit : parsedYaml) {
      List l = (List) yamlBit;
      for(Object sub : l) {
        LinkedHashMap lhm = (LinkedHashMap) sub;
        String className = lhm.get("class").toString();
        String methodName = lhm.get("method").toString();
        List attrs = (List) lhm.get("attributes");
        Map<String, String> ruleAttributes = new HashMap<>();
        for(Object attr : attrs) {
          LinkedHashMap attrMap = (LinkedHashMap) attr;
          ruleAttributes.put(attrMap.get("key").toString(), attrMap.get("value").toString());
        }
        answer.add(new NocodeRules.Rule(className, methodName, ruleAttributes));
      }
    }
    return answer;
  }



}
