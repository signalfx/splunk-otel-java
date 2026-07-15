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

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    List<String> lines = Arrays.stream(parts)
        .map(String::trim)
        .collect(Collectors.toList());
    String command = lines.get(0);
    switch (command) {
      case "thread.dump":
        startThreadDump(lines);
        break;
      default:
        LOGGER.warning("Unknown command: " + command);
    }
  }

  private boolean startThreadDump(List<String> lines) {
    int count = 1;
    if(lines.size() > 1){
      count = Integer.parseInt(lines.get(1));
    }
    Duration interval = Duration.ofSeconds(1);
    if(lines.size() > 2){
      interval = Duration.ofMillis(Integer.parseInt(lines.get(2)));
    }
    return threadDumper.startPeriodicDumper(count, interval);
  }
}
