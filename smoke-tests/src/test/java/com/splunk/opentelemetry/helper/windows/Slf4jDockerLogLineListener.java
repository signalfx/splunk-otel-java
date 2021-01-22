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

package com.splunk.opentelemetry.helper.windows;

import org.slf4j.Logger;

public class Slf4jDockerLogLineListener implements ContainerLogHandler.Listener {
  private final Logger logger;

  public Slf4jDockerLogLineListener(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void accept(ContainerLogHandler.LineType type, String text) {
    String normalizedText = text.replaceAll("((\\r?\\n)|(\\r))$", "");

    switch (type) {
      case STDOUT:
        this.logger.info("STDOUT: {}", normalizedText);
        break;
      case STDERR:
        this.logger.error("STDERR: {}", normalizedText);
        break;
    }
  }
}
