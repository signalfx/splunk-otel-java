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

package com.splunk.opentelemetry.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class StringListShortener {
  static List<String> truncate(List<String> list, int maxLength) {
    // Ensure that String representation of the List does not exceed max allowed length. List is
    // translated to String as a JSON array (it is surrounded in [], each element is surrounded in
    // double quotes, elements are separated with a comma).
    maxLength = maxLength - 4;
    List<String> result = new ArrayList<String>();
    int totalLength = 0;
    for (Iterator<String> i = list.iterator(); i.hasNext(); ) {
      String s = i.next();
      int length = s.length();
      if (i.hasNext()) {
        // we assume that list elements are joined with ","
        length += 3;
      }
      if (totalLength + length <= maxLength) {
        result.add(s);
      } else {
        // if there is room for less than 3 chars we'll need to truncate the previous element
        if (maxLength - totalLength >= 3) {
          s = s.substring(0, Math.min(s.length(), maxLength - totalLength - 3)) + "...";
        } else {
          s = result.remove(result.size() - 1);
          // we just truncate the last 3 chars, this can make the end result slightly shorter
          // than the max length
          s = s.substring(0, Math.max(0, s.length() - 3)) + "...";
        }
        result.add(s);
        return result;
      }
      totalLength += length;
    }

    return null;
  }
}
