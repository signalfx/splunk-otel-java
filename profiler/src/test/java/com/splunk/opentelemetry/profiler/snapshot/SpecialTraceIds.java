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
    return TRACE_IDS_NEW.getOrDefault(percentile, Collections.emptyList());
  }

  private static boolean isValidPercentile(int percentile) {
    int abs = Math.abs(percentile);
    return abs >= 0 && abs <= 100;
  }

  private static final Map<Integer, List<String>> TRACE_IDS_NEW =
      Map.ofEntries(
          Map.entry(
              1, List.of("0147ae147ae147b0003b7e1e927f22f5", "0000000000000000ffcd4cec40381dce")),
          Map.entry(
              2, List.of("028f5c28f5c28f60020ba864a1b06381", "feb851eb851eb850fdc9981cce25b120")),
          Map.entry(
              3, List.of("03d70a3d70a3d70002f20c52efda2b40", "fd70a3d70a3d70a0fcc9a397a3c30957")),
          Map.entry(
              4, List.of("051eb851eb851ec00403465dc24833d5", "fc28f5c28f5c2900fba5ec6b01a40d5e")),
          Map.entry(
              5, List.of("066666666666668005519dbf44c6c9d4", "fae147ae147ae140f9f909c473669b26")),
          Map.entry(
              6, List.of("07ae147ae147ae00066b7610b2ae53f2", "f999999999999980f8d5538f97e3fb6f")),
          Map.entry(
              7, List.of("08f5c28f5c28f60007b2d88f8d171204", "f851eb851eb85200f814888929ddb6a4")),
          Map.entry(
              8, List.of("0a3d70a3d70a3d8009a992975e8d92d4", "f70a3d70a3d70a00f67225e22f43128d")),
          Map.entry(
              9, List.of("0b851eb851eb85000aa701839196a891", "f5c28f5c28f5c280f4be566c56df2f73")),
          Map.entry(
              10, List.of("0ccccccccccccd000be93f426fa5788b", "f47ae147ae147b00f39f750a28141acb")),
          Map.entry(
              11, List.of("0e147ae147ae14800e0d2de2117863ad", "f333333333333300f2bd7b8ef31a6505")),
          Map.entry(
              12, List.of("0f5c28f5c28f5c000ed0cf6d4d2abcac", "f1eb851eb851eb80f19ebb234d0c5259")),
          Map.entry(
              13, List.of("10a3d70a3d70a4000febba15d8d0ba4c", "f0a3d70a3d70a400ef7874fc7db78930")),
          Map.entry(
              14, List.of("11eb851eb851ec0011d25f337ba01faf", "ef5c28f5c28f5c00ef1d46e2b968d4c8")),
          Map.entry(
              15, List.of("1333333333333300123789dca559503c", "ee147ae147ae1400edd4e74af0c1639f")),
          Map.entry(
              16, List.of("147ae147ae147b001393c38998fd516b", "eccccccccccccd00ec027c9a60386f54")),
          Map.entry(
              17, List.of("15c28f5c28f5c3001485d09e3d47b437", "eb851eb851eb8500eaebfe4b179488ff")),
          Map.entry(
              18, List.of("170a3d70a3d70a00160a62acd4671ada", "ea3d70a3d70a3d00e936fd6075a9805f")),
          Map.entry(
              19, List.of("1851eb851eb8520018384f670c325a97", "e8f5c28f5c28f600e7e0670c5b364d00")),
          Map.entry(
              20, List.of("1999999999999a001877eb5cc8e31a1f", "e7ae147ae147ae00e7643489f46d2698")),
          Map.entry(
              21, List.of("1ae147ae147ae1001a7b9905be439a7e", "e666666666666600e5a89cc2c9aa3b2e")),
          Map.entry(
              22, List.of("1c28f5c28f5c29001bfc85cd2691cbf4", "e51eb851eb851f00e51d0b4a0510d197")),
          Map.entry(
              23, List.of("1d70a3d70a3d71001d539c3eb44e9c0b", "e3d70a3d70a3d700e3892f2b3a2d334f")),
          Map.entry(
              24, List.of("1eb851eb851eb8001e3210b9c79b2a3e", "e28f5c28f5c28f00e1c4c0d9d233e738")),
          Map.entry(
              25, List.of("20000000000000001edf8473a05a8f73", "e147ae147ae14800e0ebe15fef97d65b")),
          Map.entry(
              26, List.of("2147ae147ae1480020b26bc8fd6fff51", "e000000000000000dfe83ec5340947fb")),
          Map.entry(
              27, List.of("228f5c28f5c2900021614c4c1dcfe69d", "deb851eb851eb800dea5ee95e3c298d3")),
          Map.entry(
              28, List.of("23d70a3d70a3d800236374815b32b0ce", "dd70a3d70a3d7000dd5d7c2a8db46ddd")),
          Map.entry(
              29, List.of("251eb851eb851e00241e6841aeb010b2", "dc28f5c28f5c2800db5f0011f95ad69f")),
          Map.entry(
              30, List.of("2666666666666600261f670671be80aa", "dae147ae147ae200d9d50b196c1201de")),
          Map.entry(
              31, List.of("27ae147ae147ae0026eb70d8d3a01f3d", "d999999999999a00d9544ebaf7fd8317")),
          Map.entry(
              32, List.of("28f5c28f5c28f60027f1963aff7bab6c", "d851eb851eb85200d797d026bb3da47e")),
          Map.entry(
              33, List.of("2a3d70a3d70a3e00296384c0f8c50f1b", "d70a3d70a3d70a00d660d90e42d6a8e6")),
          Map.entry(
              34, List.of("2b851eb851eb86002b3eaef62a263b0b", "d5c28f5c28f5c200d4fa8f45afddcc72")),
          Map.entry(
              35, List.of("2ccccccccccccc002c346c32f9694000", "d47ae147ae147a00d3c4b8338e798a44")),
          Map.entry(
              36, List.of("2e147ae147ae14002d864623f89ce9bc", "d333333333333400d2988ce3b820ba14")),
          Map.entry(
              37, List.of("2f5c28f5c28f5c002e7bd1a4315cbb42", "d1eb851eb851ec00d0a88806d91142a2")),
          Map.entry(
              38, List.of("30a3d70a3d70a4002fcdc2345d2e7a2b", "d0a3d70a3d70a400d0455075a22fbfb9")),
          Map.entry(
              39, List.of("31eb851eb851ec00319ceedd681d881b", "cf5c28f5c28f5c00ce9ec5d0fcd23429")),
          Map.entry(
              40, List.of("3333333333333400326ab200f91ad37d", "ce147ae147ae1400cdbf7e4293c3c644")),
          Map.entry(
              41, List.of("347ae147ae147a00346cc33fa30bbd41", "cccccccccccccc00cbd056af97178d7f")),
          Map.entry(
              42, List.of("35c28f5c28f5c2003594afd1726b9a45", "cb851eb851eb8600ca47e77e2a08a560")),
          Map.entry(
              43, List.of("370a3d70a3d70a00367952b56b1e534c", "ca3d70a3d70a3e00c96f901f3cc28007")),
          Map.entry(
              44, List.of("3851eb851eb8520037f26f75cb42f450", "c8f5c28f5c28f600c867209217c9055e")),
          Map.entry(
              45, List.of("3999999999999a0039870860342b9574", "c7ae147ae147ae00c6afd894f8d9ecbd")),
          Map.entry(
              46, List.of("3ae147ae147ae20039dc0820398e13b7", "c666666666666600c52d627b33f28369")),
          Map.entry(
              47, List.of("3c28f5c28f5c28003b63593032d9908c", "c51eb851eb851e00c49ce76ba861ec95")),
          Map.entry(
              48, List.of("3d70a3d70a3d70003c9c6798ed8d2d57", "c3d70a3d70a3d800c32f6fb649491c2e")),
          Map.entry(
              49, List.of("3eb851eb851eb8003dd5fea6d1c89527", "c28f5c28f5c29000c272bac5615a8c98")),
          Map.entry(
              50, List.of("40000000000000003f07bbb14b22ef4d", "c147ae147ae14800c06a28b1f1397522")),
          Map.entry(
              51, List.of("4147ae147ae1480040df64752d58c8be", "c000000000000000bfdbbfe64d793e6a")),
          Map.entry(
              52, List.of("428f5c28f5c29000418c27c7e905726a", "beb851eb851eb800bd88bf96ad2caf97")),
          Map.entry(
              53, List.of("43d70a3d70a3d8004305005dffc7146f", "bd70a3d70a3d7000bd2697dca73e419b")),
          Map.entry(
              54, List.of("451eb851eb8520004474746ea14cadab", "bc28f5c28f5c2800bb1f64e660545f4b")),
          Map.entry(
              55, List.of("46666666666668004579f4036cc8f0a9", "bae147ae147ae000b99a7e536174de67")),
          Map.entry(
              56, List.of("47ae147ae147b000473d6a6e316a27b3", "b999999999999800b89f20f1d04f9788")),
          Map.entry(
              57, List.of("48f5c28f5c28f40048c8e4f5bc5ddb37", "b851eb851eb85000b7df6535880f2da6")),
          Map.entry(
              58, List.of("4a3d70a3d70a3c0048f9b3c314b3b517", "b70a3d70a3d70c00b6e9bd657fd1b396")),
          Map.entry(
              59, List.of("4b851eb851eb84004b5a875b40c22592", "b5c28f5c28f5c400b588cd015a48fe7c")),
          Map.entry(
              60, List.of("4ccccccccccccc004c1d19579a8f9d1f", "b47ae147ae147c00b3563786c1a04807")),
          Map.entry(
              61, List.of("4e147ae147ae14004dc11f18c408a4df", "b333333333333400b2dd963924dc9ed3")),
          Map.entry(
              62, List.of("4f5c28f5c28f5c004f2f0ff9555ab0ae", "b1eb851eb851ec00b1332579532734cc")),
          Map.entry(
              63, List.of("50a3d70a3d70a4004f8b8548ce49df3c", "b0a3d70a3d70a400b06320b0b9858a82")),
          Map.entry(
              64, List.of("51eb851eb851ec00513111b1cd555041", "af5c28f5c28f5c00af45759cb1fe59e1")),
          Map.entry(
              65, List.of("533333333333340052bf4a091932cc8f", "ae147ae147ae1400ad35bb9c1684d28a")),
          Map.entry(
              66, List.of("547ae147ae147c00536f53e6ac0848ab", "accccccccccccc00ac253f26d7291a23")),
          Map.entry(
              67, List.of("55c28f5c28f5c400556afa2e64d0602d", "ab851eb851eb8400aa9fd5d8569a4ae9")),
          Map.entry(
              68, List.of("570a3d70a3d70c0056d942b6cd44ae98", "aa3d70a3d70a3c00aa272bb6a5733295")),
          Map.entry(
              69, List.of("5851eb851eb8500057376f5c44a9b70b", "a8f5c28f5c28f400a8b492b686bc3d66")),
          Map.entry(
              70, List.of("59999999999998005937c9c9ff103585", "a7ae147ae147b000a70fce2f5f68aa61")),
          Map.entry(
              71, List.of("5ae147ae147ae0005a06fb527af8932c", "a666666666666800a5aea50797827541")),
          Map.entry(
              72, List.of("5c28f5c28f5c28005b7843417b57f06c", "a51eb851eb852000a4a7931b731fe2d7")),
          Map.entry(
              73, List.of("5d70a3d70a3d70005c591179cc3770f9", "a3d70a3d70a3d800a2e6f2a4c14c6360")),
          Map.entry(
              74, List.of("5eb851eb851eb8005dd30817c8106a89", "a28f5c28f5c29000a2596e916b94fa88")),
          Map.entry(
              75, List.of("60000000000000005f9affed0fcd6b78", "a147ae147ae14800a0e9ff841e98cb5f")),
          Map.entry(
              76, List.of("6147ae147ae148006043ffa200e76492", "a0000000000000009f596b5ebcea308b")),
          Map.entry(
              77, List.of("628f5c28f5c29000621270c042c16ba4", "9eb851eb851eb8009e84ee43331cafc1")),
          Map.entry(
              78, List.of("63d70a3d70a3d800639ef85ab2707912", "9d70a3d70a3d70009d65e29f7849b13e")),
          Map.entry(
              79, List.of("651eb851eb8520006409527136ebfd8d", "9c28f5c28f5c28009c0d6834dc3516ad")),
          Map.entry(
              80, List.of("666666666666680065899d90546fd2c1", "9ae147ae147ae00099e0aa6ea1e3cda1")),
          Map.entry(
              81, List.of("67ae147ae147b000675876029fdf4dd5", "9999999999999800994c413868c09d94")),
          Map.entry(
              82, List.of("68f5c28f5c28f4006898aea01b13873e", "9851eb851eb8500097228e4891b8e71e")),
          Map.entry(
              83, List.of("6a3d70a3d70a3c00694584be2eecddbc", "970a3d70a3d70c0096145c65385eaf07")),
          Map.entry(
              84, List.of("6b851eb851eb84006b7810aab2b3e54d", "95c28f5c28f5c40094a20b4a41518bad")),
          Map.entry(
              85, List.of("6ccccccccccccc006c786bb5fc021b18", "947ae147ae147c00942a985032ae949b")),
          Map.entry(
              86, List.of("6e147ae147ae14006df75cd9a244efbc", "933333333333340092ff04bbc6418dc3")),
          Map.entry(
              87, List.of("6f5c28f5c28f5c006e4deac65858db93", "91eb851eb851ec0090fa29c01952f3ef")),
          Map.entry(
              88, List.of("70a3d70a3d70a4007089f07f35dd29a3", "90a3d70a3d70a400903b78ec1cb3b16b")),
          Map.entry(
              89, List.of("71eb851eb851ec0071a774ecd591cbf7", "8f5c28f5c28f5c008e36f794d9fb05c7")),
          Map.entry(
              90, List.of("7333333333333400729418199a42ce93", "8e147ae147ae14008db1c1d3d85612f1")),
          Map.entry(
              91, List.of("747ae147ae147c0073fcb2576968bd42", "8ccccccccccccc008c3175472f6a19c9")),
          Map.entry(
              92, List.of("75c28f5c28f5c400750f0d389e82eca0", "8b851eb851eb84008b0a8c1f022976fb")),
          Map.entry(
              93, List.of("770a3d70a3d70c0076ce6669a954062b", "8a3d70a3d70a3c008a0dbd4b32a79df3")),
          Map.entry(
              94, List.of("7851eb851eb85000778bb6ccc5f2759c", "88f5c28f5c28f40087f69f7a44b7f3de")),
          Map.entry(
              95, List.of("79999999999998007960eb5a77873ac0", "87ae147ae147b00086b94ff200c4d279")),
          Map.entry(
              96, List.of("7ae147ae147ae0007aa8316b75b7886f", "8666666666666800853192225d0ae3c0")),
          Map.entry(
              97, List.of("7c28f5c28f5c28007b50be10b29b2624", "851eb851eb8520008511b617e4099834")),
          Map.entry(
              98, List.of("7d70a3d70a3d70007cfaa61ff678fe65", "83d70a3d70a3d80082bf6c5ca806d028")),
          Map.entry(
              99, List.of("7eb851eb851eb8007eaf2b30fdfc47b7", "828f5c28f5c29000817055f8260e5c26")));

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
      string.append("\n    Map.entry(").append(entry.getKey()).append(", ").append("List.of(");
      for (var traceId : entry.getValue()) {
        string.append("\"").append(traceId).append("\",");
      }
      string.deleteCharAt(string.length() - 1);
      string.append(")),");
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
