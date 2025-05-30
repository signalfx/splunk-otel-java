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
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.jexl3.JexlExpression;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

class YamlParser {
  private static final Logger logger = Logger.getLogger(YamlParser.class.getName());
  private final List<NocodeRules.Rule> instrumentationRules;
  private JexlEvaluator evaluator;

  public static List<NocodeRules.Rule> parseFromFile(String yamlFileName) {
    if (yamlFileName == null || yamlFileName.trim().isEmpty()) {
      return Collections.emptyList();
    }
    try {
      try (Reader reader = Files.newBufferedReader(Paths.get(yamlFileName.trim()))) {
        return new YamlParser(reader).getInstrumentationRules();
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Can't load configured nocode yaml.", e);
      return Collections.emptyList();
    }
  }

  // For unit testing purposes
  static List<NocodeRules.Rule> parseFromString(String yaml) throws Exception {
    return new YamlParser(new StringReader(yaml)).getInstrumentationRules();
  }

  private YamlParser(Reader reader) throws Exception {
    instrumentationRules = Collections.unmodifiableList(new ArrayList<>(loadUnsafe(reader)));
  }

  public List<NocodeRules.Rule> getInstrumentationRules() {
    return instrumentationRules;
  }

  @SuppressWarnings("unchecked")
  private List<NocodeRules.Rule> loadUnsafe(Reader reader) throws Exception {
    List<NocodeRules.Rule> answer = new ArrayList<>();
    Load load = new Load(LoadSettings.builder().build());
    Iterable<Object> parsedYaml = load.loadAllFromReader(reader);
    for (Object yamlBit : parsedYaml) {
      List<Map<String, Object>> rulesMap = (List<Map<String, Object>>) yamlBit;
      for (Map<String, Object> yamlRule : rulesMap) {
        ElementMatcher<TypeDescription> classMatcher = classMatcher(yamlRule.get("class"));
        ElementMatcher<MethodDescription> methodMatcher = methodMatcher(yamlRule.get("method"));
        NocodeExpression spanName = toExpression(yamlRule.get("span_name"));
        SpanKind spanKind = null;
        if (yamlRule.get("span_kind") != null) {
          String spanKindString = yamlRule.get("span_kind").toString();
          try {
            spanKind = SpanKind.valueOf(spanKindString.toUpperCase(Locale.ROOT));
          } catch (IllegalArgumentException exception) {
            logger.warning("Invalid span kind " + spanKindString);
          }
        }
        NocodeExpression spanStatus = toExpression(yamlRule.get("span_status"));

        Map<String, NocodeExpression> ruleAttributes = new HashMap<>();
        List<Map<String, Object>> attrs = (List<Map<String, Object>>) yamlRule.get("attributes");
        if (attrs != null) {
          for (Map<String, Object> attr : attrs) {
            ruleAttributes.put(attr.get("key").toString(), toExpression(attr.get("value")));
          }
        }
        answer.add(
            new RuleImpl(
                classMatcher, methodMatcher, spanName, spanKind, spanStatus, ruleAttributes));
      }
    }

    return answer;
  }

  private ElementMatcher<TypeDescription> classMatcher(Object yaml) {
    if (yaml instanceof String) {
      return ElementMatchers.named(yaml.toString());
    }
    return matcherFromYaml(yaml, makeParsers(MatcherParser.super_type));
  }

  private ElementMatcher<MethodDescription> methodMatcher(Object yaml) {
    if (yaml instanceof String) {
      return ElementMatchers.named(yaml.toString());
    }
    return matcherFromYaml(
        yaml, makeParsers(MatcherParser.parameter_count, MatcherParser.parameter));
  }

  private static Map<String, MatcherParser> makeParsers(MatcherParser... extras) {
    Map<String, MatcherParser> answer = new HashMap<>();
    List<MatcherParser> parsers =
        new ArrayList<>(
            Arrays.asList(
                MatcherParser.and,
                MatcherParser.or,
                MatcherParser.not,
                MatcherParser.name,
                MatcherParser.name_regex));
    parsers.addAll(Arrays.asList(extras));
    for (MatcherParser mp : parsers) {
      answer.put(mp.name(), mp);
    }
    return answer;
  }

  private static <E extends NamedElement> List<ElementMatcher<E>> matcherListFromYaml(
      Object yamlObject, Map<String, MatcherParser> parsers) {
    if (!(yamlObject instanceof List)) {
      throw new IllegalArgumentException("Yaml list value expected: " + yamlObject);
    }
    List<Object> yaml = (List<Object>) yamlObject;
    ArrayList<ElementMatcher<E>> answer = new ArrayList<>();
    for (Object sub : yaml) {
      answer.add(matcherFromYaml(sub, parsers));
    }
    return answer;
  }

  private static <E extends NamedElement> ElementMatcher<E> matcherFromYaml(
      Object yamlObject, Map<String, MatcherParser> parsers) {
    if (!(yamlObject instanceof Map)) {
      throw new IllegalArgumentException("Bare yaml value not expected: " + yamlObject);
    }
    Map<String, Object> yaml = (Map<String, Object>) yamlObject;
    if (yaml.size() != 1) {
      throw new IllegalArgumentException("Multiple yaml elements not allowed without and:/or:");
    }
    String key = yaml.keySet().iterator().next();
    Object value = yaml.get(key);
    return matcherFromKeyAndYamlValue(key, value, parsers);
  }

  private static <E extends NamedElement> ElementMatcher<E> matcherFromKeyAndYamlValue(
      String key, Object value, Map<String, MatcherParser> parsers) {
    MatcherParser parser = parsers.get(key);
    if (parser == null) {
      throw new IllegalArgumentException("Unknown yaml element: " + key);
    }
    return parser.parse(value, parsers);
  }

  enum MatcherParser {
    not {
      <E extends NamedElement> ElementMatcher<E> parse(
          Object value, Map<String, MatcherParser> parsers) {
        return ElementMatchers.not(matcherFromYaml(value, parsers));
      }
    },
    and {
      <E extends NamedElement> ElementMatcher<E> parse(
          Object value, Map<String, MatcherParser> parsers) {
        ElementMatcher.Junction<E> matcher = ElementMatchers.any();
        for (ElementMatcher<NamedElement> sub : matcherListFromYaml(value, parsers)) {
          matcher = matcher.and(sub);
        }
        return matcher;
      }
    },
    or {
      <E extends NamedElement> ElementMatcher<E> parse(
          Object value, Map<String, MatcherParser> parsers) {
        ElementMatcher.Junction<E> matcher = ElementMatchers.none();
        for (ElementMatcher<NamedElement> sub : matcherListFromYaml(value, parsers)) {
          matcher = matcher.or(sub);
        }
        return matcher;
      }
    },
    name {
      <E extends NamedElement> ElementMatcher<E> parse(
          Object value, Map<String, MatcherParser> parsers) {
        return ElementMatchers.named(value.toString());
      }
    },
    name_regex {
      <E extends NamedElement> ElementMatcher<E> parse(
          Object value, Map<String, MatcherParser> parsers) {
        return ElementMatchers.nameMatches(value.toString());
      }
    },
    super_type {
      <E extends NamedElement> ElementMatcher<E> parse(
          Object value, Map<String, MatcherParser> parsers) {
        return (ElementMatcher<E>)
            AgentElementMatchers.hasSuperType(ElementMatchers.named(value.toString()));
      }
    },
    parameter_count {
      <E extends NamedElement> ElementMatcher<E> parse(
          Object value, Map<String, MatcherParser> parsers) {
        return (ElementMatcher<E>)
            ElementMatchers.takesArguments(Integer.parseInt(value.toString()));
      }
    },
    parameter {
      <E extends NamedElement> ElementMatcher<E> parse(
          Object value, Map<String, MatcherParser> parsers) {
        if (!(value instanceof Map)) {
          throw new IllegalArgumentException("Expected index: and type: for parameter:");
        }
        Map<String, Object> yamlVal = (Map<String, Object>) value;
        if (yamlVal.size() != 2 || !yamlVal.containsKey("index") || !yamlVal.containsKey("type")) {
          throw new IllegalArgumentException("Expected index: and type: for parameter:");
        }
        return (ElementMatcher<E>)
            ElementMatchers.takesArgument(
                Integer.parseInt(yamlVal.get("index").toString()),
                ElementMatchers.named(yamlVal.get("type").toString()));
      }
    };

    abstract <E extends NamedElement> ElementMatcher<E> parse(
        Object value, Map<String, MatcherParser> parsers);
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
