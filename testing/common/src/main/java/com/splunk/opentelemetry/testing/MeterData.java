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

package com.splunk.opentelemetry.testing;

import java.util.Map;
import java.util.Objects;

public final class MeterData {
  private final String type;
  private final String unit;
  private final Map<String, String> tags;

  public MeterData(String type, String unit, Map<String, String> tags) {
    this.type = type;
    this.unit = unit;
    this.tags = tags;
  }

  @SuppressWarnings("unchecked")
  public static MeterData fromMap(Map<String, Object> raw) {
    return new MeterData(
        (String) raw.get("type"), (String) raw.get("unit"), (Map<String, String>) raw.get("tags"));
  }

  public String getType() {
    return type;
  }

  public String getUnit() {
    return unit;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MeterData meterData = (MeterData) o;
    return type.equals(meterData.type)
        && unit.equals(meterData.unit)
        && tags.equals(meterData.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, unit, tags);
  }
}
