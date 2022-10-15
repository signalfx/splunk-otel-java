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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

final class DelegatingServiceNameDetector implements ServiceNameDetector {

  private static final Logger logger =
      Logger.getLogger(DelegatingServiceNameDetector.class.getName());

  private final List<ServiceNameDetector> delegates;

  DelegatingServiceNameDetector(List<ServiceNameDetector> delegates) {
    this.delegates = Collections.unmodifiableList(new ArrayList<>(delegates));
  }

  @Override
  public String detect() throws Exception {
    for (ServiceNameDetector detector : delegates) {
      try {
        String name = detector.detect();
        if (name != null) {
          return name;
        }
      } catch (Exception exception) {
        if (logger.isLoggable(FINE)) {
          logger.log(
              FINE,
              "Service name detector '" + detector.getClass().getSimpleName() + "' failed with",
              exception);
        }
      }
    }

    return null;
  }
}
