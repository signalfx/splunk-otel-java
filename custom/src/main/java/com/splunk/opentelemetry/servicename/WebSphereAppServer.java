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
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.logging.Level.FINE;

class WebSphereAppServer implements AppServer {

  private final static String SERVER_CLASS_NAME = "com.ibm.wsspi.bootstrap.WSPreLauncher";
  private final ResourceLocator locator;

  WebSphereAppServer(ResourceLocator locator) {
    this.locator = locator;
  }

  @Override
  public boolean isValidAppName(Path path) {
    // query.ear is bundled with websphere
    String name = path.getFileName().toString();
    return !"query.ear".equals(name);
  }

  @Override
  public Path getDeploymentDir() {
    // not used
    return null;
  }

  @Override
  public Class<?> getServerClass() {
    return locator.findClass(SERVER_CLASS_NAME);
  }
}
