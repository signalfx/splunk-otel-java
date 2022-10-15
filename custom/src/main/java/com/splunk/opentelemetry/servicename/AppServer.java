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
import javax.annotation.Nullable;

/**
 * An interface that represents a single kind of application server and its specific configuration.
 */
public interface AppServer {

  /** Path to directory to be scanned for deployments. */
  Path getDeploymentDir() throws Exception;

  /**
   * Returns a single class that, when present, determines that the given application server is
   * active/running.
   */
  Class<?> getServerClass();

  /**
   * Implementations for app servers that do not support ear files should override this method and
   * return false;
   */
  default boolean supportsEar() {
    return true;
  }
  ;

  /** Use to ignore default applications that are bundled with the app server. */
  default boolean isValidAppName(Path path) {
    return true;
  }

  /** Use to ignore default applications that are bundled with the app server. */
  default boolean isValidResult(Path path, @Nullable String result) {
    return true;
  }
}
