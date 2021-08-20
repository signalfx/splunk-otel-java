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

import java.util.function.ToDoubleFunction;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public final class JmxAttributesHelper {

  public static ToDoubleFunction<MBeanServer> getNumberAttribute(
      ObjectName objectName, String attributeName) {
    return mBeanServer -> {
      try {
        Object value = mBeanServer.getAttribute(objectName, attributeName);
        return value instanceof Number ? ((Number) value).doubleValue() : Double.NaN;
      } catch (MBeanException
          | AttributeNotFoundException
          | InstanceNotFoundException
          | ReflectionException e) {
        return Double.NaN;
      }
    };
  }

  private JmxAttributesHelper() {}
}
