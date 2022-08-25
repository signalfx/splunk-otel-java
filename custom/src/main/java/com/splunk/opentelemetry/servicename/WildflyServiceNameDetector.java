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
    log.info("called getDeploymentDir");
    // only standalone mode is supported
    if (System.getProperty("[Standalone]") == null) {
      return null;
    }
    log.info("is standalone");

    String jbossBaseDir = System.getenv("JBOSS_BASE_DIR");
    log.info("JBOSS_BASE_DIR {}", jbossBaseDir);
    if (jbossBaseDir != null) {
      log.debug("Using JBOSS_BASE_DIR '{}'.");
      log.info("result {}", Paths.get(jbossBaseDir, "standalone", "deployments"));
      return Paths.get(jbossBaseDir, "standalone", "deployments");
    }

    URL jarUrl = locator.getClassLocation(serverClass);
    Path jarPath = Paths.get(jarUrl.toURI());
    log.info("result {}", jarPath.getParent().resolve("standalone/deployments"));
    return jarPath.getParent().resolve("standalone/deployments");
  }
}
