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
import java.nio.file.Path;
import java.nio.file.Paths;

class LibertyServiceNameDetector extends AppServerServiceNameDetector {
  private final ResourceLocator locator;
  private final Class<?> libertyMainClass;

  LibertyServiceNameDetector(ResourceLocator locator) {
    this.locator = locator;
    libertyMainClass = locator.findClass("com.ibm.ws.kernel.boot.cmdline.EnvCheck");
  }

  @Override
  boolean supportsEar() {
    return true;
  }

  @Override
  Path getDeploymentDir() throws URISyntaxException {
    if (libertyMainClass == null) {
      return null;
    }

    // we rely on liberty startup script switching directory to the root dir of the launched server
    // if the launched server is server1 then we expect to be in liberty_dir/usr/servers/server1
    return Paths.get("dropins").toAbsolutePath();
  }
}
