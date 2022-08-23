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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

class TomcatServiceNameDetector extends AppServerServiceNameDetector {
  private final ResourceLocator locator;
  private final Class<?> tomcatMainClass;

  TomcatServiceNameDetector(ResourceLocator locator) {
    this.locator = locator;
    tomcatMainClass = locator.findClass("org.apache.catalina.startup.Bootstrap");
  }

  @Override
  boolean isValidAppName(String name) {
    return !"docs".equals(name)
        && !"examples".equals(name)
        && !"host-manager".equals(name)
        && !"manager".equals(name);
  }

  @Override
  boolean isValidResult(String name, String result) {
    return !"ROOT".equals(name) || !"Welcome to Tomcat".equals(result);
  }

  @Override
  Path getDeploymentDir() throws URISyntaxException {
    if (tomcatMainClass == null) {
      return null;
    }

    URL bootstrapJarUrl = locator.getClassLocation(tomcatMainClass);
    Path bootstrapJarPath = Paths.get(bootstrapJarUrl.toURI());
    return bootstrapJarPath.getParent().getParent().resolve("webapps");
  }
}
