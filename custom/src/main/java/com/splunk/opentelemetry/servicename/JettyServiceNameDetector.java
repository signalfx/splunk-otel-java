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

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JettyServiceNameDetector extends AppServerServiceNameDetector {
  private static final Logger log = LoggerFactory.getLogger(JettyServiceNameDetector.class);

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

  @VisibleForTesting
  static Path parseJettyBase(String programArguments) {
    if (programArguments == null) {
      return null;
    }
    int start = programArguments.indexOf("jetty.base=");
    if (start == -1) {
      return null;
    }
    start += "jetty.base=".length();
    if (start == programArguments.length()) {
      return null;
    }
    // Take the path until the first space. If the path doesn't exist extend it up to the next
    // space. Repeat until a path that exists is found or input runs out.
    int next = start;
    while (true) {
      int nextSpace = programArguments.indexOf(' ', next);
      if (nextSpace == -1) {
        Path candidate = Paths.get(programArguments.substring(start));
        return Files.exists(candidate) ? candidate : null;
      }
      Path candidate = Paths.get(programArguments.substring(start, nextSpace));
      next = nextSpace + 1;
      if (Files.exists(candidate)) {
        return candidate;
      }
    }
  }

  @Override
  Path getDeploymentDir() {
    // Jetty expects the webapps directory to be in the directory where jetty was started from.
    // Alternatively the location of webapps directory can be specified by providing jetty base
    // directory as an argument to jetty e.g. java -jar start.jar jetty.base=/dir where webapps
    // would be located in /dir/webapps.

    String programArguments = System.getProperty("sun.java.command");
    log.debug("Started with arguments '{}'.", programArguments);
    if (programArguments != null) {
      Path jettyBase = parseJettyBase(programArguments);
      if (jettyBase != null) {
        log.debug("Using jetty.base '{}'.", jettyBase);
        return jettyBase.resolve("webapps");
      }
    }

    return Paths.get("webapps").toAbsolutePath();
  }
}
