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

package com.splunk.opentelemetry.profiler.pprof;

import com.google.perftools.profiles.ProfileProto.Profile;
import com.google.perftools.profiles.ProfileProto.Sample;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class PprofUtils {
  public static byte[] deserialize(LogRecordData logRecord) throws IOException {
    var bytes = new ByteArrayInputStream(decode(logRecord));
    var inputStream = new GZIPInputStream(bytes);
    return inputStream.readAllBytes();
  }

  public static byte[] decode(LogRecordData logRecord) {
    Value<?> body = logRecord.getBodyValue();
    if (body == null) {
      throw new RuntimeException("Log record body is null");
    }
    return Base64.getDecoder().decode(body.asString());
  }

  public static Map<String, Object> toLabelString(Sample sample, Profile profile) {
    var labels = new HashMap<String, Object>();
    for (var label : sample.getLabelList()) {
      var stringTableIndex = label.getKey();
      var key = profile.getStringTable((int) stringTableIndex);
      if (label.getStr() > 0) {
        labels.put(key, profile.getStringTable((int) label.getStr()));
      } else {
        labels.put(key, label.getNum());
      }
    }
    return labels;
  }

  private PprofUtils() {}
}
