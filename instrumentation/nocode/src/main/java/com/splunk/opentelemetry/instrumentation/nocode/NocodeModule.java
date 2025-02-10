
package com.splunk.opentelemetry.instrumentation.nocode;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class NocodeModule extends InstrumentationModule {

  public NocodeModule() {
    super("nocode");
    YamlParser yp = new YamlParser();
    NocodeRules.setGlobalRules(yp.getInstrumentationRules());
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    ArrayList<TypeInstrumentation> answer = new ArrayList<>();
    for(NocodeRules.Rule rule : NocodeRules.getGlobalRules()) {
      answer.add(new NocodeInstrumentation(rule));
    }
    return answer;
  }

  @Override
  public List<String> getAdditionalHelperClassNames() {
    return Arrays.asList(
        "com.splunk.opentelemetry.instrumentation.nocode.JSPS",
        "com.splunk.opentelemetry.instrumentation.nocode.NocodeSingletons",
        "com.splunk.opentelemetry.instrumentation.nocode.NocodeAttributesExtractor",
        "com.splunk.opentelemetry.instrumentation.nocode.NocodeMethodInvocation",
        "com.splunk.opentelemetry.instrumentation.nocode.NocodeSpanKindExtractor",
        "com.splunk.opentelemetry.instrumentation.nocode.NocodeSpanNameExtractor"
    );
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }
}
