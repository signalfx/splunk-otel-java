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

package com.splunk.opentelemetry.profiler.threaddump;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DeadlockDataExtractor {
  private static final Pattern OWNABLE_SYNCHRONIZER_OWNER =
      Pattern.compile(
          "(?m)^[ \\t]*waiting for ownable synchronizer[ \\t]+"
              + "(0x[0-9a-fA-F]+),[ \\t]+\\(a ([^)]+)\\),\\R"
              + "[ \\t]*which is held by \"([^\\r\\n]*)\"[ \\t]*$");

  private DeadlockDataExtractor() {}

  static Map<String, String> extractLockOwners(String threadDump) {
    Map<String, String> lockOwners = new HashMap<>();
    Matcher matcher = OWNABLE_SYNCHRONIZER_OWNER.matcher(threadDump);
    while (matcher.find()) {
      String lock = StackTraceParser.formatLock(matcher.group(1), matcher.group(2));
      lockOwners.put(lock, matcher.group(3));
    }
    return lockOwners;
  }
}
