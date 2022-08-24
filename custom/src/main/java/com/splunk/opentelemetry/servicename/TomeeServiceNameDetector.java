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

class TomeeServiceNameDetector extends AppServerServiceNameDetector {
  private final ResourceLocator locator;
  private final String appsDir;
  private final Class<?> serverClass;
  private final boolean isTomee;

  TomeeServiceNameDetector(ResourceLocator locator) {
    // tomee deployment directory is configurable, we'll only look at the default 'apps' directory
    // tomee also deploys applications from webapps directory, detecting them is handled by
    // TomcatServiceNameDetector
    this(locator, "apps");
  }

  TomeeServiceNameDetector(ResourceLocator locator, String appsDir) {
    this.locator = locator;
    this.appsDir = appsDir;
    serverClass = locator.findClass("org.apache.catalina.startup.Bootstrap");
    isTomee = locator.findClass("org.apache.tomee.catalina.ServerListener") != null;
  }

  @Override
  boolean supportsEar() {
    return true;
  }

  @Override
  Path getDeploymentDir() throws URISyntaxException {
    if (serverClass == null || !isTomee) {
      return null;
    }

    URL jarUrl = locator.getClassLocation(serverClass);
    Path jarPath = Paths.get(jarUrl.toURI());
    // jar is in bin/. First call to getParent strips jar name and the second bin/. We'll end up
    // with a path to server root to which we append the autodeploy directory.
    return jarPath.getParent().getParent().resolve(appsDir);
  }
}
