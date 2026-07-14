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

package com.splunk.hackity.hack.control;

import java.util.logging.Logger;

public class CommandDispatcherImpl implements CommandDispatcher {

  public static final Logger LOGGER = Logger.getLogger(CommandDispatcherImpl.class.getName());

  private final BigDumper threadDumper;

  public CommandDispatcherImpl(BigDumper threadDumper) {
    this.threadDumper = threadDumper;
  }

  @Override
  public void dispatch(String contentType, String body) {
    String[] parts = body.split("\n");
    if (parts.length < 1) {
      LOGGER.warning("Missing useful command body.");
      return;
    }
    String command = parts[0].trim();
    switch (command) {
      case "thread.dump":
        threadDumper.dump();
        break;
      default:
        LOGGER.warning("Unknown command: " + command);
    }
  }
}
