package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class YamlParser {
  private static final Logger logger = Logger.getLogger(YamlParser.class.getName());
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
      logger.severe("Can't load configured yaml: "+e);
      return Collections.emptyList();
    }
  }

  private static List<NocodeRules.Rule> loadUnsafe() throws Exception {
    String yamlFileName = System.getenv(NOCODE_YMLFILE_ENV_KEY);
    if (yamlFileName == null || yamlFileName.trim().isEmpty()) {
      return Collections.emptyList();
    }
    Reader yamlReader = new FileReader(yamlFileName.trim());

    Load load = new Load(LoadSettings.builder().build());
    Iterable<Object> parsedYaml = load.loadAllFromReader(yamlReader);
    ArrayList<NocodeRules.Rule> answer = new ArrayList<>();
    for(Object yamlBit : parsedYaml) {
      List l = (List) yamlBit;
      for(Object yamlRule : l) {
        LinkedHashMap lhm = (LinkedHashMap) yamlRule;
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
