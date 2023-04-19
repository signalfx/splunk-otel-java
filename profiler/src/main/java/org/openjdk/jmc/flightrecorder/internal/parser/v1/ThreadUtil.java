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

package org.openjdk.jmc.flightrecorder.internal.parser.v1;

import org.openjdk.jmc.common.IMCThread;

// helper class for accessing package private StructTypes.JfrThread
public class ThreadUtil {
  public static Long getOsThreadId(IMCThread thread) {
    if (thread instanceof StructTypes.JfrThread) {
      Object value = ((StructTypes.JfrThread) thread).osThreadId;
      if (value instanceof Number) {
        return ((Number) value).longValue();
      }
    }
    return null;
  }
}
