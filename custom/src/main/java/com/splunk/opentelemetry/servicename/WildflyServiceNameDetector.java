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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WildflyServiceNameDetector extends AppServerServiceNameDetector {
  private static final Logger log = LoggerFactory.getLogger(WildflyServiceNameDetector.class);

  WildflyServiceNameDetector(ResourceLocator locator) {
    super(locator, "org.jboss.modules.Main", true);
  }

  @Override
  Path getDeploymentDir() throws URISyntaxException {
    String programArguments = System.getProperty("sun.java.command");
    log.debug("Started with arguments '{}'.", programArguments);
    if (programArguments == null) {
      return null;
    }
    if (!programArguments.contains("org.jboss.as.standalone")) {
      // only standalone mode is supported
      return null;
    }

    // base dir is also passed as program argument (not vm argument) -Djboss.server.base.dir we use
    // environment variable JBOSS_BASE_DIR to avoid parsing program arguments
    String jbossBaseDir = System.getenv("JBOSS_BASE_DIR");
    if (jbossBaseDir != null) {
      log.debug("Using JBOSS_BASE_DIR '{}'.", jbossBaseDir);
      return Paths.get(jbossBaseDir, "deployments");
    }

    URL jarUrl = locator.getClassLocation(serverClass);
    Path jarPath = Paths.get(jarUrl.toURI());
    return jarPath.getParent().resolve("standalone/deployments");
  }
}
