/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.instrumentation.nocode;

import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeExpression;
import com.splunk.opentelemetry.javaagent.bootstrap.nocode.NocodeRules;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.jexl3.JexlExpression;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

class YamlParser {
  private static final Logger logger = Logger.getLogger(YamlParser.class.getName());
  // FUTURE support method override selection - e.g., with classfile method signature or something
  private static final String NOCODE_YMLFILE = "splunk.otel.instrumentation.nocode.yml.file";

  private final List<NocodeRules.Rule> instrumentationRules;
  private JexlEvaluator evaluator;

  YamlParser(ConfigProperties config) {
    instrumentationRules = Collections.unmodifiableList(new ArrayList<>(load(config)));
  }

  public List<NocodeRules.Rule> getInstrumentationRules() {
    return instrumentationRules;
  }

  private List<NocodeRules.Rule> load(ConfigProperties config) {
    String yamlFileName = config.getString(NOCODE_YMLFILE);
    if (yamlFileName == null || yamlFileName.trim().isEmpty()) {
      return Collections.emptyList();
    }

    try {
      return loadUnsafe(yamlFileName);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Can't load configured nocode yaml.", e);
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  private List<NocodeRules.Rule> loadUnsafe(String yamlFileName) throws Exception {
    List<NocodeRules.Rule> answer = new ArrayList<>();
    try (InputStream inputStream = Files.newInputStream(Paths.get(yamlFileName.trim()))) {
      Load load = new Load(LoadSettings.builder().build());
      Iterable<Object> parsedYaml = load.loadAllFromInputStream(inputStream);
      for (Object yamlBit : parsedYaml) {
        List<Map<String, Object>> rulesMap = (List<Map<String, Object>>) yamlBit;
        for (Map<String, Object> yamlRule : rulesMap) {
          // FUTURE support more complex class selection (inherits-from, etc.)
          ElementMatcher<TypeDescription> classMatcher = classMatcher(yamlRule.get("class"));
          ElementMatcher<MethodDescription> methodMatcher = methodMatcher(yamlRule.get("method"));
          NocodeExpression spanName = toExpression(yamlRule.get("spanName"));
          SpanKind spanKind = null;
          if (yamlRule.get("spanKind") != null) {
            String spanKindString = yamlRule.get("spanKind").toString();
            try {
              spanKind = SpanKind.valueOf(spanKindString.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
              logger.warning("Invalid span kind " + spanKindString);
            }
          }
          NocodeExpression spanStatus = toExpression(yamlRule.get("spanStatus"));

          Map<String, NocodeExpression> ruleAttributes = new HashMap<>();
          List<Map<String, Object>> attrs = (List<Map<String, Object>>) yamlRule.get("attributes");
          if (attrs != null) {
            for (Map<String, Object> attr : attrs) {
              ruleAttributes.put(attr.get("key").toString(), toExpression(attr.get("value")));
            }
          }
          answer.add(
              new NocodeRules.Rule(
                  classMatcher, methodMatcher, spanName, spanKind, spanStatus, ruleAttributes));
        }
      }
    }

    return answer;
  }

  private ElementMatcher<TypeDescription> classMatcher(Object yaml) {
    if (yaml instanceof String) {
      return ElementMatchers.named(yaml.toString());
    }
    return matcherFromYaml(yaml, true);
  }

  private ElementMatcher<MethodDescription> methodMatcher(Object yaml) {
    if (yaml instanceof String) {
      return ElementMatchers.named(yaml.toString());
    }
    return matcherFromYaml(yaml, false);
  }

  private List<ElementMatcher> matcherListFromYaml(Object yamlObject, boolean isClass) {
    if (!(yamlObject instanceof Map)) {
      throw new IllegalArgumentException("Bare yaml value not expected: " + yamlObject);
    }
    Map<String, Object> yaml = (Map<String, Object>) yamlObject;
    ArrayList<ElementMatcher> answer = new ArrayList<>();
    for (String key : yaml.keySet()) {
      answer.add(matcherFromKeyAndYamlValue(key, yaml.get(key), isClass));
    }
    return answer;
  }

  private ElementMatcher matcherFromYaml(Object yamlObject, boolean isClass) {
    if (!(yamlObject instanceof Map)) {
      throw new IllegalArgumentException("Bare yaml value not expected: " + yamlObject);
    }
    Map<String, Object> yaml = (Map<String, Object>) yamlObject;
    if (yaml.size() != 1) {
      throw new IllegalArgumentException("Multiple yaml elements not allowed without and:/or:");
    }
    String key = yaml.keySet().iterator().next();
    Object value = yaml.get(key);
    return matcherFromKeyAndYamlValue(key, value, isClass);
  }

  private ElementMatcher matcherFromKeyAndYamlValue(String key, Object value, boolean isClass) {
    if (key.equals("and")) {
      ElementMatcher.Junction matcher = ElementMatchers.any();
      for (ElementMatcher sub : matcherListFromYaml(value, isClass)) {
        matcher = matcher.and(sub);
      }
      return matcher;
    } else if (key.equals("or")) {
      ElementMatcher.Junction matcher = ElementMatchers.none();
      for (ElementMatcher sub : matcherListFromYaml(value, isClass)) {
        matcher = matcher.or(sub);
      }
      return matcher;
    } else if (key.equals("not")) {
      return ElementMatchers.not(matcherFromYaml(value, isClass));
    } else if (key.equals("named")) {
      return ElementMatchers.named(value.toString());
    } else if (key.equals("nameMatches")) {
      return ElementMatchers.nameMatches(value.toString());
    }
    if (isClass) {
      if (key.equals("hasSuper")) {
        return ElementMatchers.hasSuperType(ElementMatchers.named(value.toString()));
      }
    } else { // isMethod
      if (key.equals("hasParameterCount")) {
        return ElementMatchers.takesArguments(Integer.parseInt(value.toString()));
      } else if (key.equals("hasParameterOfType")) {
        String[] args = ((String) value).split("\\s+");
        if (args.length != 2) {
          throw new IllegalArgumentException(
              "Expected <index> <typename> as fields of hasParameterOfType");
        }
        return ElementMatchers.takesArgument(
            Integer.parseInt(args[0]), ElementMatchers.named(args[1]));
      }
    }
    throw new IllegalArgumentException("Unknown yaml element: " + key);
  }

  private NocodeExpression toExpression(Object ruleNode) {
    if (ruleNode == null) {
      return null;
    }

    String expressionText = ruleNode.toString();
    if (expressionText == null) {
      return null;
    }

    if (evaluator == null) {
      evaluator = new JexlEvaluator();
    }

    JexlExpression jexlExpression = evaluator.createExpression(expressionText);
    if (jexlExpression == null) {
      return null;
    }

    return new NocodeExpression() {
      @Override
      public Object evaluate(Object thiz, Object[] params) {
        return evaluator.evaluate(jexlExpression, thiz, params);
      }

      @Override
      public Object evaluateAtEnd(
          Object thiz, Object[] params, Object returnValue, Throwable error) {
        return evaluator.evaluateAtEnd(jexlExpression, thiz, params, returnValue, error);
      }

      @Override
      public String toString() {
        return expressionText;
      }
    };
  }
}
