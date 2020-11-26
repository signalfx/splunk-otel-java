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

package com.splunk.opentelemetry.javaagent.bootstrap;

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiddlewareHolder {

  private static final Logger log = LoggerFactory.getLogger(MiddlewareHolder.class);

  public static final AtomicReference<String> middlewareName = new AtomicReference<>();
  public static final AtomicReference<String> middlewareVersion = new AtomicReference<>();

  public static void trySetName(String name) {
    if (!middlewareName.compareAndSet(null, name)) {
      log.debug("Trying to re-set middleware name from {} to {}", middlewareName.get(), name);
    }
  }

  public static void trySetVersion(String version) {
    if (!middlewareVersion.compareAndSet(null, version)) {
      log.debug(
          "Trying to re-set middleware version from {} to {}", middlewareVersion.get(), version);
    }
  }
}
