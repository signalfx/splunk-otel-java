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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class TomeeAppServer implements AppServer {

  private static final String SERVER_CLASS_NAME = "org.apache.catalina.startup.Bootstrap";
  private final ResourceLocator locator;

  TomeeAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Override
  public Path getDeploymentDir() throws URISyntaxException {
    Path rootDir = getRootDir();

    // check for presence of tomee configuration file, if it doesn't exist then we have tomcat not
    // tomee
    if (!Files.isRegularFile(rootDir.resolve("conf/tomee.xml"))) {
      return null;
    }

    // tomee deployment directory is configurable, we'll only look at the default 'apps' directory
    // to get the actual deployment directory (or see whether it is enabled at all) we would need to
    // parse conf/tomee.xml
    // tomee also deploys applications from webapps directory, detecting them is handled by
    // TomcatServiceNameDetector
    return rootDir.resolve("apps");
  }

  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVER_CLASS_NAME);
  }

  private Path getRootDir() throws URISyntaxException {
    String catalinaBase = System.getProperty("catalina.base");
    if (catalinaBase != null) {
      return Paths.get(catalinaBase);
    }

    String catalinaHome = System.getProperty("catalina.home");
    if (catalinaHome != null) {
      return Paths.get(catalinaHome);
    }

    // if neither catalina.base nor catalina.home is set try to deduce the location of based on the
    // loaded server class.
    URL jarUrl = locator.getClassLocation(getServerClass());
    Path jarPath = Paths.get(jarUrl.toURI());
    // jar is in bin/. First call to getParent strips jar name and the second bin/. We'll end up
    // with a path to server root.
    return jarPath.getParent().getParent();
  }
}
