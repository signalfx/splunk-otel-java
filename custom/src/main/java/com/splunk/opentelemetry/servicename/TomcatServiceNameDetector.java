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

  TomcatServiceNameDetector(ResourceLocator locator) {
    super(locator, "org.apache.catalina.startup.Bootstrap", false);
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
    String catalinaBase = System.getProperty("catalina.base");
    if (catalinaBase != null) {
      return Paths.get(catalinaBase, "webapps");
    }

    String catalinaHome = System.getProperty("catalina.home");
    if (catalinaHome != null) {
      return Paths.get(catalinaHome, "webapps");
    }

    // if neither catalina.base nor catalina.home is set try to deduce the location of webapps based
    // on the loaded server class.
    URL jarUrl = locator.getClassLocation(serverClass);
    Path jarPath = Paths.get(jarUrl.toURI());
    // jar is in bin/. First call to getParent strips jar name and the second bin/. We'll end up
    // with a path to server root to which we append the autodeploy directory.
    return jarPath.getParent().getParent().resolve("webapps");
  }
}
