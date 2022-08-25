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

package com.splunk.opentelemetry.servicename;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServiceNameDetector {
  private static final Logger log = LoggerFactory.getLogger(ServiceNameDetector.class);

  abstract String detect() throws Exception;

  public static boolean serviceNameNotConfigured(ConfigProperties config) {
    return ServiceNameChecker.serviceNameNotConfigured(config);
  }

  public static String detectServiceName() {
    for (ServiceNameDetector detector : detectors()) {
      try {
        String name = detector.detect();
        if (name != null) {
          return name;
        }
      } catch (Exception exception) {
        log.warn(
            "Service name detector '{}' failed with",
            detector.getClass().getSimpleName(),
            exception);
      }
    }

    return null;
  }

  private static List<ServiceNameDetector> detectors() {
    ResourceLocator locator = new ResourceLocatorImpl();

    List<ServiceNameDetector> detectors = new ArrayList<>();
    detectors.add(new TomeeServiceNameDetector(locator));
    detectors.add(new TomcatServiceNameDetector(locator));
    detectors.add(new JettyServiceNameDetector(locator));
    detectors.add(new LibertyServiceNameDetector(locator));
    detectors.add(new WildflyServiceNameDetector(locator));
    detectors.add(new GlassfishServiceNameDetector(locator));
    detectors.add(new WebSphereServiceNameDetector(locator));

    return detectors;
  }

  private static class ResourceLocatorImpl implements ResourceLocator {

    @Override
    public Class<?> findClass(String className) {
      try {
        return Class.forName(className, false, ClassLoader.getSystemClassLoader());
      } catch (ClassNotFoundException | LinkageError exception) {
        return null;
      }
    }

    @Override
    public URL getClassLocation(Class<?> clazz) {
      return clazz.getProtectionDomain().getCodeSource().getLocation();
    }
  }
}
