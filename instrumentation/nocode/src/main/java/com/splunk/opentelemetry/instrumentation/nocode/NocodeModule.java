
package com.splunk.opentelemetry.instrumentation.nocode;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.nocode.NocodeRules;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class NocodeModule extends InstrumentationModule {

  // FIXME style

  public NocodeModule() {
    super("nocode");
    YamlParser yp = new YamlParser();
    NocodeRules.setGlobalRules(yp.getInstrumentationRules());
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    ArrayList<TypeInstrumentation> answer = new ArrayList<>();
    // FIXME is doing this parsing in two places necessary for classloader stuff or can I just use the singleton?
    for(NocodeRules.Rule rule : NocodeRules.getGlobalRules()) {
      answer.add(new NocodeInstrumentation(rule));
      System.out.println("Added instrumentation for rule "+ rule);;
    }
    return answer;
  }

  @Override
  public List<String> getAdditionalHelperClassNames() {
    return Arrays.asList(
        "com.splunk.opentelemetry.instrumentation.nocode.JSPS",
        "com.splunk.opentelemetry.instrumentation.nocode.NocodeSingletons"
    );
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }
}
