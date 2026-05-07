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

package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.TraceId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Maintains a collection of trace ids mapped to percentiles matching the probability calculation in
 * {@link com.splunk.opentelemetry.profiler.snapshot.TraceIdBasedSnapshotSelector}.
 *
 * <p>A trace ID percentile is defined as the range of values existing between SELECTION_RANGE *
 * (percentile - 1 / 100) and SELECTION_RANGE * (percentile / 100). A trace ID is placed within a
 * percentile by using the {@link Long} representation of the last 14 characters of the trace id.
 *
 * <pre>
 * Ex. For percentile 5 the range is considered to be:
 * lower = SELECTION_RANGE * 0.04 = 10737418
 * upper = SELECTION_RANGE * 0.05 = 13421772
 * </pre>
 *
 * <p>A trace id is considered to be in the percentile range when the 28 lower bits of the second
 * long in the trace id characters is within the range.
 */
class SnapshotSelectorTestTraceIds {
  // TraceIdBasedSnapshotSelector looks only at the lower 28 bits of the 128-bit trace-id value
  private static final int SELECTION_RANGE = 1 << 28;

  /**
   * Get trace ids known to be within the requested percentile. At least two trace ids will be
   * returned for each percentile representing the second long in the trace id being both positive
   * and negative.
   *
   * @return trace ids with matching computed percentile
   * @throws IllegalArgumentException when provided percentile is less than 1 or more than 99
   */
  static List<String> forPercentile(int percentile) {
    if (percentile < 1 || percentile > 99) {
      throw new IllegalArgumentException("Invalid percentile: " + percentile);
    }
    return TRACE_IDS.getOrDefault(percentile, Collections.emptyList());
  }

  private static final Map<Integer, List<String>> TRACE_IDS =
      Map.ofEntries(
          mapEntry(1, "00000000000000000000000000209d27", "0000000000000000000000000027a91d"),
          mapEntry(2, "000000000000000000000000003a269a", "000000000000000000000000002c3ecc"),
          mapEntry(3, "00000000000000000000000000587789", "000000000000000000000000005b87fe"),
          mapEntry(4, "00000000000000000000000000982d38", "00000000000000000000000000918baa"),
          mapEntry(5, "00000000000000000000000000c04436", "00000000000000000000000000be2039"),
          mapEntry(6, "00000000000000000000000000e6fbd2", "00000000000000000000000000d39f04"),
          mapEntry(7, "0000000000000000000000000100222c", "000000000000000000000000011d04b6"),
          mapEntry(8, "000000000000000000000000013a8c30", "00000000000000000000000001361b6d"),
          mapEntry(9, "000000000000000000000000015f9f64", "0000000000000000000000000151646a"),
          mapEntry(10, "00000000000000000000000001752030", "000000000000000000000000018b5787"),
          mapEntry(11, "00000000000000000000000001b750c6", "00000000000000000000000001c059d3"),
          mapEntry(12, "00000000000000000000000001d11c72", "00000000000000000000000001e376de"),
          mapEntry(13, "00000000000000000000000001f08d5a", "0000000000000000000000000204c9fb"),
          mapEntry(14, "00000000000000000000000002347a35", "0000000000000000000000000214a27e"),
          mapEntry(15, "000000000000000000000000024d2306", "0000000000000000000000000259ec24"),
          mapEntry(16, "00000000000000000000000002716a58", "000000000000000000000000027e25c7"),
          mapEntry(17, "00000000000000000000000002aac8f5", "0000000000000000000000000291b571"),
          mapEntry(18, "00000000000000000000000002c24e43", "00000000000000000000000002bc4e2d"),
          mapEntry(19, "00000000000000000000000002fcbf2a", "00000000000000000000000002f4e764"),
          mapEntry(20, "0000000000000000000000000324ce3a", "000000000000000000000000033293e4"),
          mapEntry(21, "0000000000000000000000000335ae6b", "0000000000000000000000000358cb7f"),
          mapEntry(22, "000000000000000000000000037cd58b", "000000000000000000000000036bdbfc"),
          mapEntry(23, "000000000000000000000000038d056c", "000000000000000000000000039b559c"),
          mapEntry(24, "00000000000000000000000003bee8f2", "00000000000000000000000003c7f0e3"),
          mapEntry(25, "00000000000000000000000003e77be6", "00000000000000000000000003f249b1"),
          mapEntry(26, "00000000000000000000000004220e60", "000000000000000000000000042389b1"),
          mapEntry(27, "000000000000000000000000043ddf33", "0000000000000000000000000441f2da"),
          mapEntry(28, "00000000000000000000000004700385", "000000000000000000000000046b4412"),
          mapEntry(29, "0000000000000000000000000487853d", "000000000000000000000000048008d7"),
          mapEntry(30, "00000000000000000000000004ac4ece", "00000000000000000000000004aaad0b"),
          mapEntry(31, "00000000000000000000000004df8325", "00000000000000000000000004e98cbe"),
          mapEntry(32, "00000000000000000000000005173694", "000000000000000000000000050def4d"),
          mapEntry(33, "0000000000000000000000000540c99a", "000000000000000000000000054418d2"),
          mapEntry(34, "0000000000000000000000000558bf3b", "000000000000000000000000054e30d0"),
          mapEntry(35, "00000000000000000000000005809404", "00000000000000000000000005956530"),
          mapEntry(36, "00000000000000000000000005c0e2f6", "00000000000000000000000005a5f331"),
          mapEntry(37, "00000000000000000000000005d4f283", "00000000000000000000000005dcd0ab"),
          mapEntry(38, "00000000000000000000000006030db4", "00000000000000000000000005f1f580"),
          mapEntry(39, "0000000000000000000000000628a56f", "000000000000000000000000061ce55b"),
          mapEntry(40, "0000000000000000000000000659af9d", "0000000000000000000000000642e003"),
          mapEntry(41, "00000000000000000000000006739770", "0000000000000000000000000674eca0"),
          mapEntry(42, "000000000000000000000000069a041b", "00000000000000000000000006b6a03a"),
          mapEntry(43, "00000000000000000000000006d5a52c", "00000000000000000000000006c5a423"),
          mapEntry(44, "00000000000000000000000006e3d905", "00000000000000000000000007078393"),
          mapEntry(45, "000000000000000000000000071f4bce", "00000000000000000000000007228cf3"),
          mapEntry(46, "000000000000000000000000074e0b07", "00000000000000000000000007581aba"),
          mapEntry(47, "000000000000000000000000076db61f", "000000000000000000000000076bb44e"),
          mapEntry(48, "00000000000000000000000007897a80", "000000000000000000000000079e61a0"),
          mapEntry(49, "00000000000000000000000007bdc92c", "00000000000000000000000007cf73eb"),
          mapEntry(50, "00000000000000000000000007d721eb", "00000000000000000000000007e27553"),
          mapEntry(51, "0000000000000000000000000807f57c", "000000000000000000000000080e5e54"),
          mapEntry(52, "00000000000000000000000008448b48", "000000000000000000000000084c3b38"),
          mapEntry(53, "00000000000000000000000008560c03", "00000000000000000000000008698617"),
          mapEntry(54, "000000000000000000000000089c219e", "0000000000000000000000000884faef"),
          mapEntry(55, "00000000000000000000000008a91de0", "00000000000000000000000008b0ea40"),
          mapEntry(56, "00000000000000000000000008f57f98", "00000000000000000000000008d976c0"),
          mapEntry(57, "00000000000000000000000008fcb88b", "0000000000000000000000000902ca7b"),
          mapEntry(58, "00000000000000000000000009444897", "00000000000000000000000009387b84"),
          mapEntry(59, "00000000000000000000000009547645", "000000000000000000000000096d9006"),
          mapEntry(60, "00000000000000000000000009934905", "000000000000000000000000098e0e28"),
          mapEntry(61, "00000000000000000000000009b814fd", "00000000000000000000000009c04915"),
          mapEntry(62, "00000000000000000000000009d33446", "00000000000000000000000009c46df9"),
          mapEntry(63, "0000000000000000000000000a102bc6", "00000000000000000000000009f1eb48"),
          mapEntry(64, "0000000000000000000000000a22c9af", "0000000000000000000000000a3424b5"),
          mapEntry(65, "0000000000000000000000000a591261", "0000000000000000000000000a4fe3d7"),
          mapEntry(66, "0000000000000000000000000a78d37d", "0000000000000000000000000a6f1a96"),
          mapEntry(67, "0000000000000000000000000a9c1b2e", "0000000000000000000000000ab30f4e"),
          mapEntry(68, "0000000000000000000000000acb1dd7", "0000000000000000000000000adacc63"),
          mapEntry(69, "0000000000000000000000000aee5d2d", "0000000000000000000000000b075d61"),
          mapEntry(70, "0000000000000000000000000b245fe6", "0000000000000000000000000b21bb2c"),
          mapEntry(71, "0000000000000000000000000b4fa6e5", "0000000000000000000000000b53022a"),
          mapEntry(72, "0000000000000000000000000b61ceed", "0000000000000000000000000b7c97c6"),
          mapEntry(73, "0000000000000000000000000b8c94c7", "0000000000000000000000000b9399d6"),
          mapEntry(74, "0000000000000000000000000bced4f0", "0000000000000000000000000bc0c8fd"),
          mapEntry(75, "0000000000000000000000000bdc3cf3", "0000000000000000000000000bf5659c"),
          mapEntry(76, "0000000000000000000000000c1fb9fb", "0000000000000000000000000c0e2a7c"),
          mapEntry(77, "0000000000000000000000000c4c2b6f", "0000000000000000000000000c2b2c62"),
          mapEntry(78, "0000000000000000000000000c59724c", "0000000000000000000000000c79fbd3"),
          mapEntry(79, "0000000000000000000000000c820326", "0000000000000000000000000c9aa100"),
          mapEntry(80, "0000000000000000000000000ca4daef", "0000000000000000000000000cabca09"),
          mapEntry(81, "0000000000000000000000000cce8c41", "0000000000000000000000000cefba44"),
          mapEntry(82, "0000000000000000000000000d1e1bd3", "0000000000000000000000000d155488"),
          mapEntry(83, "0000000000000000000000000d1f9610", "0000000000000000000000000d378273"),
          mapEntry(84, "0000000000000000000000000d5c1735", "0000000000000000000000000d6a2dbb"),
          mapEntry(85, "0000000000000000000000000d7c3863", "0000000000000000000000000d866d9d"),
          mapEntry(86, "0000000000000000000000000db23d8c", "0000000000000000000000000da49dc5"),
          mapEntry(87, "0000000000000000000000000deb6b81", "0000000000000000000000000de4e54a"),
          mapEntry(88, "0000000000000000000000000e0021c7", "0000000000000000000000000e13d1ef"),
          mapEntry(89, "0000000000000000000000000e385cd2", "0000000000000000000000000e1ff154"),
          mapEntry(90, "0000000000000000000000000e4eea14", "0000000000000000000000000e510a37"),
          mapEntry(91, "0000000000000000000000000e8bdd8c", "0000000000000000000000000e8c190b"),
          mapEntry(92, "0000000000000000000000000e9c58e0", "0000000000000000000000000e9c0501"),
          mapEntry(93, "0000000000000000000000000edcea53", "0000000000000000000000000ed2fb19"),
          mapEntry(94, "0000000000000000000000000ef3d070", "0000000000000000000000000ee90679"),
          mapEntry(95, "0000000000000000000000000f29177a", "0000000000000000000000000f2f72b0"),
          mapEntry(96, "0000000000000000000000000f39cc43", "0000000000000000000000000f4a5ad1"),
          mapEntry(97, "0000000000000000000000000f5ce953", "0000000000000000000000000f6272ae"),
          mapEntry(98, "0000000000000000000000000fa7290d", "0000000000000000000000000fad85af"),
          mapEntry(99, "0000000000000000000000000fb567d2", "0000000000000000000000000fd17054"));

  private static Map.Entry<Integer, List<String>> mapEntry(int percentile, String... traceIds) {
    return Map.entry(percentile, List.of(traceIds));
  }

  /**
   * Generates a map of percentiles to trace ids that will pass the selection check when the
   * corresponding probability is used (e.g., 5th percentile and 0.05).
   */
  public static void main(String[] args) {
    var map = new LinkedHashMap<Integer, List<String>>();
    for (int percentile = 1; percentile <= 99; percentile++) {
      long lower = (long) (SELECTION_RANGE * ((double) (percentile - 1) / 100));
      long upper = (long) (SELECTION_RANGE * ((double) (percentile) / 100));

      var random1 = new Random().nextLong(lower + 1, upper);
      var random2 = new Random().nextLong(lower + 1, upper);
      map.put(percentile, List.of(TraceId.fromLongs(0, random1), TraceId.fromLongs(0, random2)));
    }
    System.out.println(asJavaCode(map));
  }

  private static StringBuilder asJavaCode(Map<Integer, List<String>> map) {
    var string = new StringBuilder();
    string
        .append("private static final Map<Integer, List<String>> TRACE_IDS =")
        .append("\n\tMap.ofEntries(");
    for (var entry : map.entrySet()) {
      string.append("\n\t\tmapEntry(").append(entry.getKey()).append(", ");
      for (var traceId : entry.getValue()) {
        string.append("\"").append(traceId).append("\", ");
      }
      string.deleteCharAt(string.length() - 1);
      string.deleteCharAt(string.length() - 1);
      string.append("),");
    }
    string.deleteCharAt(string.length() - 1);
    string.append(");");
    return string;
  }

  private SnapshotSelectorTestTraceIds() {}
}
