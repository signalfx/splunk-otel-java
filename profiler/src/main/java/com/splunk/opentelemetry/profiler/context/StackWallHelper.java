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

package com.splunk.opentelemetry.profiler.context;

class StackWallHelper {
  /**
   * Helper method to perform an indexOf that is limited to a specific stack trace in a string that
   * may contain many. The expectation is that it is only used to find something which should under
   * normal circumstances always be present, therefore the performance penalty of the underlying
   * call to {@link String#indexOf(int, int)} also checking further in the string (in case it was
   * out of bounds) would be insignificant.
   */
  static int indexOfWithinStack(String wallOfStacks, int character, int fromIndex, int endIndex) {
    int result = wallOfStacks.indexOf(character, fromIndex);
    if (result >= endIndex) {
      return -1;
    }
    return result;
  }
}
