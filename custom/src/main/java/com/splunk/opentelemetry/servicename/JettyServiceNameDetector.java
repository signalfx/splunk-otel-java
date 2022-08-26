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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class JettyServiceNameDetector extends AppServerServiceNameDetector {

  JettyServiceNameDetector(ResourceLocator locator) {
    super(locator, "org.eclipse.jetty.start.Main", false);
  }

  @Override
  boolean isValidAppName(Path path) {
    // jetty deployer ignores directories ending with ".d"
    if (Files.isDirectory(path)) {
      return !path.getFileName().toString().endsWith(".d");
    }
    return true;
  }

  @Override
  Path getDeploymentDir() {
    // Jetty expects the webapps directory to be in the directory where jetty was started from.
    // Alternatively the location of webapps directory can be specified by providing jetty base
    // directory as an argument to jetty e.g. java -jar start.jar jetty.base=/dir where webapps
    // would be located in /dir/webapps. We don't handle jetty.base yet.
    return Paths.get("webapps").toAbsolutePath();
  }
}
