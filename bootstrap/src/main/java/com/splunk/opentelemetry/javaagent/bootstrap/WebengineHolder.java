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

import static java.util.logging.Level.FINE;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class WebengineHolder {

  private static final Logger logger = Logger.getLogger(WebengineHolder.class.getName());

  public static final AtomicReference<String> webengineName = new AtomicReference<>();
  public static final AtomicReference<String> webengineVersion = new AtomicReference<>();

  public static void trySetName(String name) {
    if (!webengineName.compareAndSet(null, name) && logger.isLoggable(FINE)) {
      logger.log(
          FINE,
          "Trying to re-set webengine name from {0} to {1}",
          new Object[] {webengineName.get(), name});
    }
  }

  public static void trySetVersion(String version) {
    if (!webengineVersion.compareAndSet(null, version) && logger.isLoggable(FINE)) {
      logger.log(
          FINE,
          "Trying to re-set webengine version from {0} to {1}",
          new Object[] {webengineVersion.get(), version});
    }
  }
}
