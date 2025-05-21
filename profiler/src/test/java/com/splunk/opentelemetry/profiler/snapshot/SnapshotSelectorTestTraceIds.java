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
 * {@link io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler}.
 *
 * <p>A trace ID percentile is defined as the range of values existing between {@link
 * Long#MAX_VALUE} * (percentile - 1 / 100) and {@link Long#MAX_VALUE} * (percentile / 100). A trace
 * ID is placed within a percentile by using the {@link Long} representation of the last 16
 * characters of the trace id. When that long value is negative, the absolute value is used to find
 * the percentile the trace id belongs to.
 *
 * <pre>
 * Ex. For percentile 5 the range is considered to be:
 * lower = {@link Long#MAX_VALUE} * 0.04 = 368934881474191040
 * upper = {@link Long#MAX_VALUE} * 0.05 = 461168601842738816
 * </pre>
 *
 * <p>A trace id is considered to be in the percentile range when the absolute value of second long
 * in the trace id characters (as calculated using {@link
 * io.opentelemetry.api.internal.OtelEncodingUtils#longFromBase16String(traceId, 16)}) is within the
 * range.
 *
 * @see io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler
 * @see io.opentelemetry.api.internal.OtelEncodingUtils#longFromBase16String(CharSequence, int)
 */
class SnapshotSelectorTestTraceIds {
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
          mapEntry(1, "00000000000000000143b39877726d4c", "0000000000000000febc4c67888d92b4"),
          mapEntry(2, "00000000000000000148aaf1b30b99cd", "0000000000000000feb7550e4cf46633"),
          mapEntry(3, "000000000000000002c619f84b6e3d6d", "0000000000000000fd39e607b491c293"),
          mapEntry(4, "000000000000000003e90e2397e85e31", "0000000000000000fc16f1dc6817a1cf"),
          mapEntry(5, "00000000000000000599e9374303b131", "0000000000000000fa6616c8bcfc4ecf"),
          mapEntry(6, "000000000000000006b0fccea239c2b9", "0000000000000000f94f03315dc63d47"),
          mapEntry(7, "000000000000000008ddd8dd0054e50f", "0000000000000000f7222722ffab1af1"),
          mapEntry(8, "00000000000000000914a87beefb8944", "0000000000000000f6eb5784110476bc"),
          mapEntry(9, "00000000000000000a81f147ae79b580", "0000000000000000f57e0eb851864a80"),
          mapEntry(10, "00000000000000000c7a85e0e3f6e67e", "0000000000000000f3857a1f1c091982"),
          mapEntry(11, "00000000000000000dd7c279626667d8", "0000000000000000f2283d869d999828"),
          mapEntry(12, "00000000000000000f57cc320b3e215f", "0000000000000000f0a833cdf4c1dea1"),
          mapEntry(13, "00000000000000000fe3a959597f1edf", "0000000000000000f01c56a6a680e121"),
          mapEntry(14, "0000000000000000118124dcd62c13cd", "0000000000000000ee7edb2329d3ec33"),
          mapEntry(15, "0000000000000000130ef512f84555b3", "0000000000000000ecf10aed07baaa4d"),
          mapEntry(16, "0000000000000000141b67a6ec1e7670", "0000000000000000ebe4985913e18990"),
          mapEntry(17, "00000000000000001585569f807b5f27", "0000000000000000ea7aa9607f84a0d9"),
          mapEntry(18, "000000000000000015d637d48c22d1a2", "0000000000000000ea29c82b73dd2e5e"),
          mapEntry(19, "0000000000000000179120b3146bc1dc", "0000000000000000e86edf4ceb943e24"),
          mapEntry(20, "00000000000000001882dbfd7abfa5fb", "0000000000000000e77d240285405a05"),
          mapEntry(21, "00000000000000001adaa7e91a829c90", "0000000000000000e5255816e57d6370"),
          mapEntry(22, "00000000000000001b3b4b75dcb2bfa3", "0000000000000000e4c4b48a234d405d"),
          mapEntry(23, "00000000000000001c4e3ee8733679ed", "0000000000000000e3b1c1178cc98613"),
          mapEntry(24, "00000000000000001e603c290e5e580a", "0000000000000000e19fc3d6f1a1a7f6"),
          mapEntry(25, "00000000000000001faa411f98ddc7cb", "0000000000000000e055bee067223835"),
          mapEntry(26, "000000000000000020a5afbcd97e4120", "0000000000000000df5a50432681bee0"),
          mapEntry(27, "00000000000000002209e46754395688", "0000000000000000ddf61b98abc6a978"),
          mapEntry(28, "0000000000000000233bd49d8de75c71", "0000000000000000dcc42b627218a38f"),
          mapEntry(29, "000000000000000023da35b710aee647", "0000000000000000dc25ca48ef5119b9"),
          mapEntry(30, "000000000000000025d6512432116c49", "0000000000000000da29aedbcdee93b7"),
          mapEntry(31, "00000000000000002676d70894ccaaf9", "0000000000000000d98928f76b335507"),
          mapEntry(32, "000000000000000027b9015b4eee4f09", "0000000000000000d846fea4b111b0f7"),
          mapEntry(33, "00000000000000002908c4007e983072", "0000000000000000d6f73bff8167cf8e"),
          mapEntry(34, "00000000000000002a4e4a10d0eef684", "0000000000000000d5b1b5ef2f11097c"),
          mapEntry(35, "00000000000000002caaa2529f376d41", "0000000000000000d3555dad60c892bf"),
          mapEntry(36, "00000000000000002d777a1c645899f5", "0000000000000000d28885e39ba7660b"),
          mapEntry(37, "00000000000000002ecf3ed7042fbbc9", "0000000000000000d130c128fbd04437"),
          mapEntry(38, "00000000000000002ff45e5ddc9d52d6", "0000000000000000d00ba1a22362ad2a"),
          mapEntry(39, "000000000000000030e16d4ebedf82f5", "0000000000000000cf1e92b141207d0b"),
          mapEntry(40, "000000000000000032a03147ce4cc60a", "0000000000000000cd5fceb831b339f6"),
          mapEntry(41, "0000000000000000345e2f0cbdce2196", "0000000000000000cba1d0f34231de6a"),
          mapEntry(42, "000000000000000034f65c111fb8e190", "0000000000000000cb09a3eee0471e70"),
          mapEntry(43, "00000000000000003679137f3515a89b", "0000000000000000c986ec80caea5765"),
          mapEntry(44, "00000000000000003723c78599aa8203", "0000000000000000c8dc387a66557dfd"),
          mapEntry(45, "0000000000000000396dd313445b8dea", "0000000000000000c6922cecbba47216"),
          mapEntry(46, "00000000000000003aa7691ef0edc188", "0000000000000000c55896e10f123e78"),
          mapEntry(47, "00000000000000003bb404785110eb8c", "0000000000000000c44bfb87aeef1474"),
          mapEntry(48, "00000000000000003ca55c229d93ce23", "0000000000000000c35aa3dd626c31dd"),
          mapEntry(49, "00000000000000003d82411baa8640cd", "0000000000000000c27dbee45579bf33"),
          mapEntry(50, "00000000000000003edb9eb17d582b29", "0000000000000000c124614e82a7d4d7"),
          mapEntry(51, "0000000000000000405bfc65c6b9fe1e", "0000000000000000bfa4039a394601e2"),
          mapEntry(52, "000000000000000041d75c3fa386fee6", "0000000000000000be28a3c05c79011a"),
          mapEntry(53, "0000000000000000437c32176ead3933", "0000000000000000bc83cde89152c6cd"),
          mapEntry(54, "0000000000000000441eae96cca9b1ab", "0000000000000000bbe1516933564e55"),
          mapEntry(55, "000000000000000045ad0089da3cad48", "0000000000000000ba52ff7625c352b8"),
          mapEntry(56, "0000000000000000469b0599b4b42b74", "0000000000000000b964fa664b4bd48c"),
          mapEntry(57, "0000000000000000483c973d7219ee64", "0000000000000000b7c368c28de6119c"),
          mapEntry(58, "00000000000000004a2414453cee6677", "0000000000000000b5dbebbac3119989"),
          mapEntry(59, "00000000000000004adb3965a6cbec2d", "0000000000000000b524c69a593413d3"),
          mapEntry(60, "00000000000000004c6f922c7949ede4", "0000000000000000b3906dd386b6121c"),
          mapEntry(61, "00000000000000004df5f13e05aa5613", "0000000000000000b20a0ec1fa55a9ed"),
          mapEntry(62, "00000000000000004f35fe02b3e7e461", "0000000000000000b0ca01fd4c181b9f"),
          mapEntry(63, "000000000000000050162d60064273fc", "0000000000000000afe9d29ff9bd8c04"),
          mapEntry(64, "000000000000000050b68a04f001e2d7", "0000000000000000af4975fb0ffe1d29"),
          mapEntry(65, "0000000000000000533189021ef11943", "0000000000000000acce76fde10ee6bd"),
          mapEntry(66, "0000000000000000534cecedf1b7d6a6", "0000000000000000acb313120e48295a"),
          mapEntry(67, "000000000000000055b2bd9c1c657001", "0000000000000000aa4d4263e39a8fff"),
          mapEntry(68, "0000000000000000567949e730f262b2", "0000000000000000a986b618cf0d9d4e"),
          mapEntry(69, "000000000000000057199fe8bc4ef161", "0000000000000000a8e6601743b10e9f"),
          mapEntry(70, "000000000000000058cc4d0b1d6b01b5", "0000000000000000a733b2f4e294fe4b"),
          mapEntry(71, "000000000000000059c19e1a3e2f6ca8", "0000000000000000a63e61e5c1d09358"),
          mapEntry(72, "00000000000000005af1551116b3dbff", "0000000000000000a50eaaeee94c2401"),
          mapEntry(73, "00000000000000005c7017b8f295b9b1", "0000000000000000a38fe8470d6a464f"),
          mapEntry(74, "00000000000000005e4d3841617b60dd", "0000000000000000a1b2c7be9e849f23"),
          mapEntry(75, "00000000000000005ee8f85e75a427d7", "0000000000000000a11707a18a5bd829"),
          mapEntry(76, "00000000000000006093d2d520cc6e90", "00000000000000009f6c2d2adf339170"),
          mapEntry(77, "00000000000000006203192ebaae234f", "00000000000000009dfce6d14551dcb1"),
          mapEntry(78, "0000000000000000630c2fda50a965e4", "00000000000000009cf3d025af569a1c"),
          mapEntry(79, "000000000000000064f5e269e2e24f20", "00000000000000009b0a1d961d1db0e0"),
          mapEntry(80, "000000000000000065c9fa9afb7fd35e", "00000000000000009a36056504802ca2"),
          mapEntry(81, "000000000000000066cb4ee73ce16f2a", "00000000000000009934b118c31e90d6"),
          mapEntry(82, "0000000000000000683fbf3468ca8a54", "000000000000000097c040cb973575ac"),
          mapEntry(83, "00000000000000006a3a8d3da082a9c7", "000000000000000095c572c25f7d5639"),
          mapEntry(84, "00000000000000006aab99065ac9ddcc", "0000000000000000955466f9a5362234"),
          mapEntry(85, "00000000000000006bde934c09acdcee", "000000000000000094216cb3f6532312"),
          mapEntry(86, "00000000000000006d6d8a23eead7196", "0000000000000000929275dc11528e6a"),
          mapEntry(87, "00000000000000006ee8e3f10829c598", "000000000000000091171c0ef7d63a68"),
          mapEntry(88, "00000000000000006ff553d6a3a7c64f", "0000000000000000900aac295c5839b1"),
          mapEntry(89, "000000000000000070ec301873be78ec", "00000000000000008f13cfe78c418714"),
          mapEntry(90, "00000000000000007300000232767805", "00000000000000008cfffffdcd8987fb"),
          mapEntry(91, "00000000000000007433d290acdedde0", "00000000000000008bcc2d6f53212220"),
          mapEntry(92, "0000000000000000758737145b360ecb", "00000000000000008a78c8eba4c9f135"),
          mapEntry(93, "0000000000000000770083adadec9c20", "000000000000000088ff7c52521363e0"),
          mapEntry(94, "000000000000000077bb30dfbf5a4ac2", "00000000000000008844cf2040a5b53e"),
          mapEntry(95, "0000000000000000797f17c4c31bdc02", "00000000000000008680e83b3ce423fe"),
          mapEntry(96, "00000000000000007acff0cd846071a3", "000000000000000085300f327b9f8e5d"),
          mapEntry(97, "00000000000000007c0554b910135b0d", "000000000000000083faab46efeca4f3"),
          mapEntry(98, "00000000000000007c61f93693043865", "0000000000000000839e06c96cfbc79b"),
          mapEntry(99, "00000000000000007d841f140bf3625b", "0000000000000000827be0ebf40c9da5"));

  private static Map.Entry<Integer, List<String>> mapEntry(int percentile, String... traceIds) {
    return Map.entry(percentile, List.of(traceIds));
  }

  /**
   * Generates a map of percentiles to trace ids that will pass the sampling check in {@link
   * io.opentelemetry.sdk.trace.samplers.TraceIdRatioBasedSampler} when the corresponding
   * probability is used (e.g., 5th percentile and 0.05).
   */
  public static void main(String[] args) {
    var map = new LinkedHashMap<Integer, List<String>>();
    for (int percentile = 1; percentile <= 99; percentile++) {
      long lower = (long) (Long.MAX_VALUE * ((double) (percentile - 1) / 100));
      long upper = (long) (Long.MAX_VALUE * ((double) (percentile) / 100));

      var random = new Random().nextLong(lower + 1, upper);
      // TraceIdRatioBasedSampler only considers the second number so OK to leave as 0
      map.put(percentile, List.of(TraceId.fromLongs(0, random), TraceId.fromLongs(0, -random)));
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
