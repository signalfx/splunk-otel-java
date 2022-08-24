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

class WildflyServiceNameDetector extends AppServerServiceNameDetector {
  private final ResourceLocator locator;
  private final Class<?> serverClass;

  WildflyServiceNameDetector(ResourceLocator locator) {
    this.locator = locator;
    serverClass = locator.findClass("org.jboss.modules.Main");
  }

  @Override
  boolean supportsEar() {
    return true;
  }

  @Override
  Path getDeploymentDir() throws URISyntaxException {
    if (serverClass == null || System.getProperty("[Standalone]") == null) {
      return null;
    }

    URL jarUrl = locator.getClassLocation(serverClass);
    Path jarPath = Paths.get(jarUrl.toURI());
    return jarPath.getParent().resolve("standalone/deployments");
  }
}
