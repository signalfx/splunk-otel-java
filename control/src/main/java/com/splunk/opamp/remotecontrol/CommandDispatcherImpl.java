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

package com.splunk.opamp.remotecontrol;

import static java.util.logging.Level.WARNING;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CommandDispatcherImpl implements CommandDispatcher {

  public static final Logger logger = Logger.getLogger(CommandDispatcherImpl.class.getName());
  private static final int MAX_THREAD_DUMP_COUNT = 100;
  private static final int MAX_THREAD_DUMP_INTERVAL_MILLIS = 300_000;

  private final BigDumper threadDumper;

  public CommandDispatcherImpl(BigDumper threadDumper) {
    this.threadDumper = threadDumper;
  }

  @Override
  public void dispatch(String contentType, String body) {
    String[] parts = body.split("\n");
    if (parts.length == 0) {
      logger.warning("Missing useful command body.");
      return;
    }
    List<String> lines = Arrays.stream(parts).map(String::trim).collect(Collectors.toList());
    String command = lines.get(0);
    List<String> params = Collections.emptyList();
    if (lines.size() > 1) {
      params = lines.subList(1, lines.size());
    }
    switch (command) {
      case "thread.dump":
        startThreadDump(params);
        break;
      default:
        logger.warning("Unknown command: " + command);
    }
  }

  /** params look like this: - job id - count (default: 1) - interval (default: 1000) */
  private boolean startThreadDump(List<String> params) {
    // The first param is the job id, and it's required
    if (params.isEmpty() || params.get(0).isEmpty()) {
      logger.warning("Missing thread dump job ID.");
      return false;
    }

    String jobId = params.get(0);
    try {
      int count = getParamOrDefault(params, 1, 1);
      int intervalMillis = getParamOrDefault(params, 2, 1000);
      if (count < 1 || count > MAX_THREAD_DUMP_COUNT) {
        logger.warning("Thread dump count must be between 1 and 100.");
        return false;
      }
      if (intervalMillis < 1 || intervalMillis > MAX_THREAD_DUMP_INTERVAL_MILLIS) {
        logger.warning("Thread dump interval must be between 1 and 300000 milliseconds.");
        return false;
      }
      return threadDumper.startPeriodicDumper(jobId, count, Duration.ofMillis(intervalMillis));
    } catch (NumberFormatException exception) {
      logger.warning("Thread dump count and interval must be valid 32-bit integers.");
      return false;
    } catch (RuntimeException exception) {
      logger.log(WARNING, "Thread dump command failed.", exception);
      return false;
    }
  }

  private static int getParamOrDefault(List<String> params, int index, int defaultValue) {
    return params.size() > index ? Integer.parseInt(params.get(index)) : defaultValue;
  }
}
