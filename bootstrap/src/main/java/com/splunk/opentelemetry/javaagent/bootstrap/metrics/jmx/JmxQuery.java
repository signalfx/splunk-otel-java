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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx;

import static java.util.Collections.singletonMap;

import com.google.auto.value.AutoValue;
import java.util.Map;
import java.util.stream.Collectors;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

@AutoValue
public abstract class JmxQuery {

  public static JmxQuery create(String domain, String propertyKey, String propertyValue) {
    return create(domain, singletonMap(propertyKey, propertyValue));
  }

  public static JmxQuery create(String domain, Map<String, String> properties) {
    return new AutoValue_JmxQuery(domain, properties);
  }

  abstract String domain();

  abstract Map<String, String> properties();

  ObjectName toObjectNameQuery() throws MalformedObjectNameException {
    String propertiesStr =
        properties().entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(","));
    if (!propertiesStr.isEmpty()) {
      propertiesStr += ",";
    }
    return new ObjectName(domain() + ":" + propertiesStr + "*");
  }

  boolean matches(ObjectName objectName) {
    if (!objectName.getDomain().equals(domain())) {
      return false;
    }
    for (Map.Entry<String, String> e : properties().entrySet()) {
      if (!objectName.getKeyProperty(e.getKey()).equals(e.getValue())) {
        return false;
      }
    }
    return true;
  }
}
