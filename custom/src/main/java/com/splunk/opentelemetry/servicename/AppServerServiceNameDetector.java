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

import static java.util.logging.Level.FINE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

final class AppServerServiceNameDetector implements ServiceNameDetector {

  private static final Logger logger =
      Logger.getLogger(AppServerServiceNameDetector.class.getName());

  final AppServer appServer;

  AppServerServiceNameDetector(AppServer appServer) {
    this.appServer = appServer;
  }

  @Override
  public @Nullable String detect() throws Exception {
    if (appServer.getServerClass() == null) {
      return null;
    }

    Path deploymentDir = appServer.getDeploymentDir();
    if (deploymentDir == null) {
      return null;
    }

    if (Files.isDirectory(deploymentDir)) {
      logger.log(FINE, "Looking for deployments in '{0}'.", deploymentDir);
      try (Stream<Path> stream = Files.list(deploymentDir)) {
        for (Path path : stream.collect(Collectors.toList())) {
          String name = detectName(path);
          if (name != null) {
            return name;
          }
        }
      }
    } else {
      logger.log(FINE, "Deployment dir '{0}' doesn't exist.", deploymentDir);
    }

    return null;
  }

  private String detectName(Path path) {
    if (!appServer.isValidAppName(path)) {
      logger.log(FINE, "Skipping '{0}'.", path);
      return null;
    }

    logger.log(FINE, "Attempting service name detection in '{0}'.", path);
    String name = path.getFileName().toString();
    ParseBuddy parseBuddy = new ParseBuddy(appServer);
    if (Files.isDirectory(path)) {
      return parseBuddy.handleExplodedApp(path);
    } else if (name.endsWith(".war")) {
      return parseBuddy.handlePackagedWar(path);
    } else if (appServer.supportsEar() && name.endsWith(".ear")) {
      return parseBuddy.handlePackagedEar(path);
    }

    return null;
  }
}
