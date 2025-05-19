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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

class SpecialTraceIds {
  /**
   * Get trace ID known to have a computed percentile equal to the requested value. Negative values
   * are accepted.
   *
   * @return trace ID with matching computed percentile
   * @throws IllegalArgumentException when provided percentile's absolute value of is less than 0 or
   *     more than 100
   */
  static List<String> forPercentileNew(int percentile) {
    if (!isValidPercentile(percentile)) {
      throw new IllegalArgumentException("Invalid percentile: " + percentile);
    }
    return TRACE_IDS.getOrDefault(percentile, Collections.emptyList());
  }

  private static boolean isValidPercentile(int percentile) {
    int abs = Math.abs(percentile);
    return abs >= 0 && abs <= 100;
  }

  private static final Map<Integer, List<String>> TRACE_IDS =
      Map.ofEntries(
          mapEntry(1, "0147ae147ae147b001073f1784a17046", "0000000000000000ffad76c12a1d565b"),
          mapEntry(2, "028f5c28f5c28f60023dbb7062d4d2c9", "feb851eb851eb850fdeba0a4bc354dd3"),
          mapEntry(3, "03d70a3d70a3d70002a40901032f75c4", "fd70a3d70a3d70a0fcb86674c016f02a"),
          mapEntry(4, "051eb851eb851ec0049e41e2f6a9cf6e", "fc28f5c28f5c2900fb4fc2b616faf303"),
          mapEntry(5, "066666666666668005ae0a9f53442a27", "fae147ae147ae140fa9bbb99a495e16d"),
          mapEntry(6, "07ae147ae147ae00079eb83afce007c1", "f999999999999980f8f4835f770a5351"),
          mapEntry(7, "08f5c28f5c28f60007ed8d8695d1221a", "f851eb851eb85200f75dfc1c1a791820"),
          mapEntry(8, "0a3d70a3d70a3d800a39a132342b2ae9", "f70a3d70a3d70a00f67f22ac61bd92d5"),
          mapEntry(9, "0b851eb851eb85000b3477896e7296a4", "f5c28f5c28f5c280f56e9f1b4c24544a"),
          mapEntry(10, "0ccccccccccccd000c183a0c29a7ab5d", "f47ae147ae147b00f41837c3fbbc84e9"),
          mapEntry(11, "0e147ae147ae14800def7c9d5c6b81a8", "f333333333333300f2888b89b9835c51"),
          mapEntry(12, "0f5c28f5c28f5c000e90c56db61b066c", "f1eb851eb851eb80f1b364d2550e999a"),
          mapEntry(13, "10a3d70a3d70a4000fbc24989cea3b00", "f0a3d70a3d70a400f02d707e24b5f5d0"),
          mapEntry(14, "11eb851eb851ec0011ca537c5e3b9699", "ef5c28f5c28f5c00ee8fc3f71c92e22f"),
          mapEntry(15, "133333333333330012ab7e0a68b2ba72", "ee147ae147ae1400ed20f7d931195293"),
          mapEntry(16, "147ae147ae147b0013fd64f3dea19f03", "eccccccccccccd00ec9648a668d13ab4"),
          mapEntry(17, "15c28f5c28f5c300151b60ac2436c477", "eb851eb851eb8500eb33e194ca339f5f"),
          mapEntry(18, "170a3d70a3d70a0016d310af8e6ec975", "ea3d70a3d70a3d00e9a319f6b317a24e"),
          mapEntry(19, "1851eb851eb8520017469470293eb916", "e8f5c28f5c28f600e8dc8ddc7b8c44ef"),
          mapEntry(20, "1999999999999a001975fc3324a3bff0", "e7ae147ae147ae00e79ba07cde3b66af"),
          mapEntry(21, "1ae147ae147ae1001a5762a82385d793", "e666666666666600e661fc2321b550a9"),
          mapEntry(22, "1c28f5c28f5c29001bb33755c359b900", "e51eb851eb851f00e49efd22802138d0"),
          mapEntry(23, "1d70a3d70a3d71001d2a698537c2d94b", "e3d70a3d70a3d700e39b75fe5c1f3e3b"),
          mapEntry(24, "1eb851eb851eb8001d977027e6b6a3fc", "e28f5c28f5c28f00e24929fd9eda7075"),
          mapEntry(25, "20000000000000001feda4fc2f3f5382", "e147ae147ae14800e04fc905fd502a5b"),
          mapEntry(26, "2147ae147ae1480020e597a220e6c79b", "e000000000000000df30436dfeb2a1fc"),
          mapEntry(27, "228f5c28f5c2900021a25da5f4ccc304", "deb851eb851eb800de6976711b5da5a7"),
          mapEntry(28, "23d70a3d70a3d8002388ca6fbd1d0b6b", "dd70a3d70a3d7000dc34828317df7a7e"),
          mapEntry(29, "251eb851eb851e0024823f09d48f3c26", "dc28f5c28f5c2800dc1761f78698d4e0"),
          mapEntry(30, "26666666666666002601f7ca960f1fe3", "dae147ae147ae200da4c2b217b7b5bb9"),
          mapEntry(31, "27ae147ae147ae002787036b89498c45", "d999999999999a00d8d09b94da7bfa5f"),
          mapEntry(32, "28f5c28f5c28f60028345957ce945fac", "d851eb851eb85200d74f4ab72768a55c"),
          mapEntry(33, "2a3d70a3d70a3e0029beca5253917719", "d70a3d70a3d70a00d675e959e36ad219"),
          mapEntry(34, "2b851eb851eb86002b20bbe569cc085b", "d5c28f5c28f5c200d5663a00a4443d29"),
          mapEntry(35, "2ccccccccccccc002c903a1d4d35eb4e", "d47ae147ae147a00d3c2e816871458bf"),
          mapEntry(36, "2e147ae147ae14002d5a232e541d58fc", "d333333333333400d20dc8e2909af0c1"),
          mapEntry(37, "2f5c28f5c28f5c002e4c3932b24afb28", "d1eb851eb851ec00d0f1ff85bfe60f54"),
          mapEntry(38, "30a3d70a3d70a4003042b2964986afa3", "d0a3d70a3d70a400cf90fef3469572e4"),
          mapEntry(39, "31eb851eb851ec0030a7ecf8f62d3fcd", "cf5c28f5c28f5c00cf57811d93627fd0"),
          mapEntry(40, "3333333333333400325f4d0c39f81f19", "ce147ae147ae1400cda72efb2dfa0fe9"),
          mapEntry(41, "347ae147ae147a0033498e449bb29a55", "cccccccccccccc00cc868a3c78e42cdb"),
          mapEntry(42, "35c28f5c28f5c20034a0cd9360684700", "cb851eb851eb8600ca6a078dd43e7d33"),
          mapEntry(43, "370a3d70a3d70a0035f4d4f502866c70", "ca3d70a3d70a3e00c9fb5ac145fef141"),
          mapEntry(44, "3851eb851eb85200381740f06acef619", "c8f5c28f5c28f600c8ac1fd38fd06b39"),
          mapEntry(45, "3999999999999a003866145448504c44", "c7ae147ae147ae00c6bb1bfce049b393"),
          mapEntry(46, "3ae147ae147ae2003ab2a674ae3c9682", "c666666666666600c63e06f3eaa6c72e"),
          mapEntry(47, "3c28f5c28f5c28003c11da57fa0448da", "c51eb851eb851e00c4169f19546773ca"),
          mapEntry(48, "3d70a3d70a3d70003d3b9cb08c5d8427", "c3d70a3d70a3d800c2ab767a8536e246"),
          mapEntry(49, "3eb851eb851eb8003e232c5be8ffa52d", "c28f5c28f5c29000c18c3d2d481d1666"),
          mapEntry(50, "40000000000000003fefcf6bb89f812c", "c147ae147ae14800c02d769ebbaa0c57"),
          mapEntry(51, "4147ae147ae1480040a815ec23b57087", "c000000000000000bf7bfe86a53bf919"),
          mapEntry(52, "428f5c28f5c29000416d474c77f3d8aa", "beb851eb851eb800be2b1b0abdfaf143"),
          mapEntry(53, "43d70a3d70a3d80042f8044fc4cf7d44", "bd70a3d70a3d7000bcdb92868c411754"),
          mapEntry(54, "451eb851eb85200043dbac397e6bdc65", "bc28f5c28f5c2800bc09c2fcbe73ed46"),
          mapEntry(55, "466666666666680045f5894addb9f481", "bae147ae147ae000ba00a079ce9fc4bd"),
          mapEntry(56, "47ae147ae147b0004790196ce6b68638", "b999999999999800b9927a1bcb6bfa8e"),
          mapEntry(57, "48f5c28f5c28f400486edc04edaff9db", "b851eb851eb85000b841259bb9e5eb80"),
          mapEntry(58, "4a3d70a3d70a3c0049c78b49802c9541", "b70a3d70a3d70c00b5f1cfd7509fb6b3"),
          mapEntry(59, "4b851eb851eb84004a549e1a466a0d44", "b5c28f5c28f5c400b5a385e163555fd3"),
          mapEntry(60, "4ccccccccccccc004bfac37ea32d4579", "b47ae147ae147c00b353de2c7ebd0663"),
          mapEntry(61, "4e147ae147ae14004cd803ece6fce1ad", "b333333333333400b206a56633e7eb9a"),
          mapEntry(62, "4f5c28f5c28f5c004ea455bed79267ba", "b1eb851eb851ec00b0e282c4f1a5a5b0"),
          mapEntry(63, "50a3d70a3d70a40050a1e83c083dc803", "b0a3d70a3d70a400afedc1b92d9d083c"),
          mapEntry(64, "51eb851eb851ec0051a2fc1002cd5368", "af5c28f5c28f5c00ae59dc30481c7003"),
          mapEntry(65, "5333333333333400525242aada0c47ec", "ae147ae147ae1400ad372d58dba4dee4"),
          mapEntry(66, "547ae147ae147c0053354d371f298481", "accccccccccccc00aba4b115b606c9ab"),
          mapEntry(67, "55c28f5c28f5c40055a193132114f87b", "ab851eb851eb8400ab2518abe3df887c"),
          mapEntry(68, "570a3d70a3d70c0056f3a8db1a61e035", "aa3d70a3d70a3c00aa3a014f10ed5c22"),
          mapEntry(69, "5851eb851eb85000571d9051f43e08b9", "a8f5c28f5c28f400a8397404438b1e54"),
          mapEntry(70, "59999999999998005890dabed9ee1de0", "a7ae147ae147b000a695ca896a36c565"),
          mapEntry(71, "5ae147ae147ae0005abb4923768c9a2e", "a666666666666800a635c663a89665d0"),
          mapEntry(72, "5c28f5c28f5c28005bb772ca3bdf5cfe", "a51eb851eb852000a4e2893d12807d15"),
          mapEntry(73, "5d70a3d70a3d70005cdaf6e7cdf847b6", "a3d70a3d70a3d800a3d28a1612362825"),
          mapEntry(74, "5eb851eb851eb8005e8590907c5807c0", "a28f5c28f5c29000a170f07d0118cc59"),
          mapEntry(75, "60000000000000005ffd2356e0209edd", "a147ae147ae14800a014a69cb52f309c"),
          mapEntry(76, "6147ae147ae1480060b2582a793e4e5b", "a0000000000000009f8957b5a2095b5c"),
          mapEntry(77, "628f5c28f5c2900061f8ecb7ccb0d928", "9eb851eb851eb8009d81cd527c2cbb46"),
          mapEntry(78, "63d70a3d70a3d800633ab50c7ad3bc33", "9d70a3d70a3d70009d028283993f247f"),
          mapEntry(79, "651eb851eb852000649227f9c7af6e5e", "9c28f5c28f5c28009b65e8a459ef926f"),
          mapEntry(80, "66666666666668006565ec1db4ca0d55", "9ae147ae147ae00099b8c550afc70e06"),
          mapEntry(81, "67ae147ae147b000667d87aba97697c6", "999999999999980098a782c02e422c26"),
          mapEntry(82, "68f5c28f5c28f400687d389a7e683596", "9851eb851eb85000977e9df9a1674399"),
          mapEntry(83, "6a3d70a3d70a3c006932f6b3d1b296b2", "970a3d70a3d70c0096eaa88837434f59"),
          mapEntry(84, "6b851eb851eb84006aaf238a5e96059e", "95c28f5c28f5c40095aec458603dba39"),
          mapEntry(85, "6ccccccccccccc006ca07fb7ecfaada7", "947ae147ae147c009342a9dd8f77dc60"),
          mapEntry(86, "6e147ae147ae14006df6ab9da2960e30", "9333333333333400923d447fc8b10fd5"),
          mapEntry(87, "6f5c28f5c28f5c006f2d4d597a6fc3d7", "91eb851eb851ec0090fa7eff0fb82c57"),
          mapEntry(88, "70a3d70a3d70a4006fc7e9a6f4845d4c", "90a3d70a3d70a4008f819f78ef9bc423"),
          mapEntry(89, "71eb851eb851ec0071b8098256025417", "8f5c28f5c28f5c008e72c4a4db735441"),
          mapEntry(90, "733333333333340071f57a33efdc6da8", "8e147ae147ae14008df7fb9bef3bf8ce"),
          mapEntry(91, "747ae147ae147c00740ec06e42dea4fe", "8ccccccccccccc008b99be522e684a28"),
          mapEntry(92, "75c28f5c28f5c40074d5c2f1d2e4936e", "8b851eb851eb84008aa7035a138f3337"),
          mapEntry(93, "770a3d70a3d70c0075e1366af3812a76", "8a3d70a3d70a3c00898302ad06cac6d5"),
          mapEntry(94, "7851eb851eb8500077d0876e4b2354e9", "88f5c28f5c28f4008811849ccec92749"),
          mapEntry(95, "7999999999999800798aaa2f8e2ff15f", "87ae147ae147b000866bcaf0a87b8931"),
          mapEntry(96, "7ae147ae147ae00079a5a248e6e3f743", "866666666666680085617b507a531ed7"),
          mapEntry(97, "7c28f5c28f5c28007b2e4cc6ef77008c", "851eb851eb85200083e4bbf269f228d6"),
          mapEntry(98, "7d70a3d70a3d70007cedb23eee80aa1d", "83d70a3d70a3d80082c335f22168fedf"),
          mapEntry(99, "7eb851eb851eb8007e927fc16f5b143d", "828f5c28f5c29000823d6b632e0bd12f"));

  private static Map.Entry<Integer, List<String>> mapEntry(int percentile, String... traceIds) {
    return Map.entry(percentile, List.of(traceIds));
  }

  public static void main(String[] args) {
    var ranges = createRanges();
    var traceIds = generateTraceIds(ranges);
    System.out.println(asJavaCode(traceIds));
  }

  private static Deque<PercentileRange> createRanges() {
    var ranges = new ArrayDeque<PercentileRange>();
    for (int percentile = 1; percentile <= 99; percentile++) {
      ranges.add(new PercentileRange(percentile, false));
      ranges.add(new PercentileRange(percentile, true));
    }
    return ranges;
  }

  private static Map<Integer, List<String>> generateTraceIds(Deque<PercentileRange> ranges) {
    var map = new LinkedHashMap<Integer, List<String>>();
    while (!ranges.isEmpty()) {
      var range = ranges.remove();
      boolean foundTraceIdInRange = false;
      while (!foundTraceIdInRange) {
        var lower = new Random().nextLong(range.lower + 1, range.upper);
        var traceId = TraceId.fromLongs(range.upper, lower);
        map.compute(
            range.value,
            (percentile, traceIds) -> {
              if (traceIds == null) {
                traceIds = new ArrayList<>();
              }
              traceIds.add(traceId);
              return traceIds;
            });
        foundTraceIdInRange = true;
      }
    }
    return map;
  }

  private static StringBuilder asJavaCode(Map<Integer, List<String>> map) {
    var string = new StringBuilder();
    string.append("private static final Map<Integer, List<String>> TRACE_IDS_NEW = Map.ofEntries(");
    for (var entry : map.entrySet()) {
      string.append("\n    mapEntry(").append(entry.getKey()).append(", ");
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

  private static class PercentileRange {
    private final int value;
    private final long lower;
    private final long upper;

    private PercentileRange(int value, boolean negative) {
      this.value = value;
      if (!negative) {
        this.lower = (long) (Long.MAX_VALUE * ((double) (value - 1) / 100));
        this.upper = (long) (Long.MAX_VALUE * ((double) (value) / 100));
      } else {
        this.upper = (long) (Long.MIN_VALUE * ((double) (value - 1) / 100));
        this.lower = (long) (Long.MIN_VALUE * ((double) (value) / 100));
      }
    }

    boolean contains(long value) {
      return value > lower && value < upper;
    }

    @Override
    public String toString() {
      return lower + " - " + upper;
    }
  }

  private SpecialTraceIds() {}
}
