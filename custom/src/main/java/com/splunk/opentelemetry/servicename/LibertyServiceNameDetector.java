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

import java.nio.file.Path;
import java.nio.file.Paths;

class LibertyServiceNameDetector extends AppServerServiceNameDetector {
  private final ResourceLocator locator;
  private final Class<?> serverClass;

  LibertyServiceNameDetector(ResourceLocator locator) {
    this.locator = locator;
    serverClass = locator.findClass("com.ibm.ws.kernel.boot.cmdline.EnvCheck");
  }

  @Override
  boolean supportsEar() {
    return true;
  }

  @Override
  Path getDeploymentDir() {
    if (serverClass == null) {
      return null;
    }

    // default installation has
    // WLP_OUTPUT_DIR - libertyDir/usr/servers
    // WLP_USER_DIR - libertyDir/usr
    // docker image has
    // WLP_USER_DIR - /opt/ol/wlp/usr
    // WLP_OUTPUT_DIR - /opt/ol/wlp/output

    // liberty server sets current directory to $WLP_OUTPUT_DIR/serverName we need
    // $WLP_USER_DIR/servers/serverName
    // in default installation we already have the right directory and don't need to do anything
    Path serverDir = Paths.get("").toAbsolutePath();
    String wlpUserDir = System.getenv("WLP_USER_DIR");
    String wlpOutputDir = System.getenv("WLP_OUTPUT_DIR");
    if (wlpUserDir != null
        && wlpOutputDir != null
        && !Paths.get(wlpOutputDir).equals(Paths.get(wlpUserDir, "servers"))) {
      Path serverName = Paths.get(wlpOutputDir).getFileName();
      serverDir = Paths.get(wlpUserDir).resolve(serverName);
    }

    return serverDir.resolve("dropins");
  }
}
