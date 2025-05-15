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

import io.opentelemetry.api.internal.OtelEncodingUtils;
import io.opentelemetry.sdk.trace.IdGenerator;

import java.util.ArrayDeque;
import java.util.Map;

class SpecialTraceIds {
  /**
   * Get trace ID known to have a computed percentile equal to the requested value. Negative values
   * are accepted.
   *
   * @return trace ID with matching computed percentile
   * @throws IllegalArgumentException when provided percentile's absolute value of is less than 0 or
   *     more than 100
   */
  static String forPercentile(int percentile) {
    if (!isValidPercentile(percentile)) {
      throw new IllegalArgumentException("Invalid percentile: " + percentile);
    }
    return TRACE_IDS.get(percentile);
  }

  private static boolean isValidPercentile(int percentile) {
    int abs = Math.abs(percentile);
    return abs >= 0 && abs <= 100;
  }

  private static final Map<Integer, String> TRACE_IDS =
      Map.ofEntries(
          Map.entry(-99, "bd415487190d9aad81758f1dee8bf481"),
          Map.entry(-98, "cbaaf86ad2dd441d82f563de2ac7c295"),
          Map.entry(-97, "e6124c9b59c41d1284b6174cb3cc5eb9"),
          Map.entry(-96, "dd5b80d28217474785cbf1aa4cb74d20"),
          Map.entry(-95, "4aed865b5b26b72f86b3e6615e41ad05"),
          Map.entry(-94, "d3137bede7b33af5888e78e3d3dc2113"),
          Map.entry(-93, "1768a31f624ef2ce897e265781a0e62c"),
          Map.entry(-92, "80f75365e24e1fc98aa85c2763fdafc8"),
          Map.entry(-91, "d9f89399c8cb1f208c2be8be067b00c2"),
          Map.entry(-90, "df2a6a53a51c9b0d8d0f36de962db6f8"),
          Map.entry(-89, "622afab819edc7a78f01b10a697b17e5"),
          Map.entry(-88, "dd2bcf50fa85f4649040c6b575236b66"),
          Map.entry(-87, "6cff289614da4bb6917b77a1867146b8"),
          Map.entry(-86, "ef869ac206ad5ec9924141df87904925"),
          Map.entry(-85, "0b76e4e0d7f0ed1b93af104178b81780"),
          Map.entry(-84, "ac539b147cf19b30951ade5f9614d102"),
          Map.entry(-83, "accffb3aff7e6638962c60c31ab89772"),
          Map.entry(-82, "224bda5f953f9d6c985026b01cc18dbe"),
          Map.entry(-81, "6d806d515dd20fd79900a0c2f33fd8cd"),
          Map.entry(-80, "3d1a4a2a72c1677c99e51b16ca271539"),
          Map.entry(-79, "bc4221b8b8c1a2cd9b3ff905a8fff231"),
          Map.entry(-78, "1bc4c64473ab94a59c6c0a47b1f55be8"),
          Map.entry(-77, "7113e7b9dd651a8d9e4b31f60ea808e6"),
          Map.entry(-76, "cc15f401626aed279fd61af99888717f"),
          Map.entry(-75, "14ec81737f4d3379a0d75d72e456d3e1"),
          Map.entry(-74, "01cb80abdb2ab31da26c284822088888"),
          Map.entry(-73, "da2023701fee6e3aa3176a4e8b765d90"),
          Map.entry(-72, "fd77bcc0d408ef72a4251feb3df375a6"),
          Map.entry(-71, "35f25c86c29984f0a64b0c47e8782526"),
          Map.entry(-70, "bcf1b32064b0c85da685df9c76a9b75d"),
          Map.entry(-69, "d29f72c960b84641a81f77f450512bfa"),
          Map.entry(-68, "802b11cc21db98f6a9f8de5b12e75cfc"),
          Map.entry(-67, "7204fd8374f88de9aad6a19f63a1216a"),
          Map.entry(-66, "599c897fc912572aacadbc39e5eab15c"),
          Map.entry(-65, "09734db0b28949f4adde47cbae6076fe"),
          Map.entry(-64, "424c9759e5b55b34aebcd2738a81c2ba"),
          Map.entry(-63, "139a492f6546dd08b06a0a760083b9d6"),
          Map.entry(-62, "beb8b1e01825a515b127027a01ca40cb"),
          Map.entry(-61, "498d65414bb058edb3209e484530c1c4"),
          Map.entry(-60, "724a07bb60cca709b3b8f1e5960574a7"),
          Map.entry(-59, "99438cc52ac3315ab59c0549a761cd2f"),
          Map.entry(-58, "4f0b07a4b2b667ceb69c22ba2aead1e4"),
          Map.entry(-57, "4618c708178336a7b72219ae28bb5ae1"),
          Map.entry(-56, "bf158d866432d01fb951ecbdf1254747"),
          Map.entry(-55, "3dcbf32e7a5f0709ba944aac61663e10"),
          Map.entry(-54, "632a633002a11831bbcbb7c47bb392f9"),
          Map.entry(-53, "6d5c4ec4483bb3bbbc7a5df711fce0f6"),
          Map.entry(-52, "303b48162c78ccfebd72e8b262a27a22"),
          Map.entry(-51, "41b40ce68d4236d9bf79337ce40560cc"),
          Map.entry(-50, "4faa84a21fbe9c4cc12eca4af2eddf7e"),
          Map.entry(-49, "cce550da9cded2bfc226e826bfe3d349"),
          Map.entry(-48, "b1f98ef7f34733adc2fc8e0b2c7fe5d6"),
          Map.entry(-47, "edee66eed3531abfc44452a4757263f2"),
          Map.entry(-46, "017f8f890dba70b2c56e47b08c27cb9d"),
          Map.entry(-45, "0d5da5aca4955083c684adb3de91969a"),
          Map.entry(-44, "743e7d27b92ffba5c8b7cb2def891d59"),
          Map.entry(-43, "c36412fff10e140fc92d4149555b7c2a"),
          Map.entry(-42, "e2d3890b3fa2a0bdcadea6feeae1f717"),
          Map.entry(-41, "0c9ebea92c46d291ccb12117443c2f7d"),
          Map.entry(-40, "b60a11472cfde765cdda0285fc439bb0"),
          Map.entry(-39, "4e4b3b2a775c874acf362b1ba9f36007"),
          Map.entry(-38, "d1602621bee45ca7cf79c290095a578b"),
          Map.entry(-37, "643160d634c8c180d167141416329b9d"),
          Map.entry(-36, "b2636ad5ca821815d238a6d1ab614a98"),
          Map.entry(-35, "840fa2f1cf3e9b15d3515df1073202f9"),
          Map.entry(-34, "e3ff2c241deb1e5fd4b288e93867ceb5"),
          Map.entry(-33, "94a292c490a7a6ffd6a921461f1d9303"),
          Map.entry(-32, "11c7c62fb1ec3b71d75431a433a89b0c"),
          Map.entry(-31, "754b92cc86b4413dd87f8396d1f5ca0d"),
          Map.entry(-30, "0500ccb0a253074fd9b5f3fe67ad927b"),
          Map.entry(-29, "7565828039676d19db78b956a3dbb7af"),
          Map.entry(-28, "fafde45b6f3810c0dcec32328641ca87"),
          Map.entry(-27, "9417f8ce5f3c9e22de1e497877b42c1c"),
          Map.entry(-26, "49f42681ae2862f3df13a446fc8c0ad6"),
          Map.entry(-25, "c13379b3753b3d9ae12089abced8594d"),
          Map.entry(-24, "33071b9d7fa3f393e2720b88c55fcde9"),
          Map.entry(-23, "e1d19a12b28045b8e33e35f525f0a0e6"),
          Map.entry(-22, "61b0b41639df6abde4092c04cc065c8e"),
          Map.entry(-21, "7821ebdaae0c7091e5c4d44018a77dc1"),
          Map.entry(-20, "2744ccdafff26bd8e7681ccffd751eb5"),
          Map.entry(-19, "80be4dc8e9588b65e833cb726d88cb9a"),
          Map.entry(-18, "9612374da2e71d39e9f3ec1e650e677c"),
          Map.entry(-17, "6223f180c35f8306ea50cdd2322192d5"),
          Map.entry(-16, "fee2ec65b2283b4beccb4e2172515663"),
          Map.entry(-15, "f868c9c00be1232fedc7dbcd2018ebc6"),
          Map.entry(-14, "99541c86b4243423eed88e9495bf58aa"),
          Map.entry(-13, "d2c1fcd29d071826f056c34335ccada3"),
          Map.entry(-12, "fd7833d285c54bf1f1d19a57596dd046"),
          Map.entry(-11, "fff195e8ba1cef66f2efe44045ea4100"),
          Map.entry(-10, "4f022301835d6f58f4087c79d1f149bd"),
          Map.entry(-9, "1022610fdec29ffef4f85a3ce56bbdc7"),
          Map.entry(-8, "c44f365084aab186f68841e3e1208b38"),
          Map.entry(-7, "ea6cbd5179d37f3ff7d0afe2a209fa68"),
          Map.entry(-6, "48037208dec71569f8cb0b67c5e01381"),
          Map.entry(-5, "986b203a02447d5df9bb619e3666421d"),
          Map.entry(-4, "947035a119bd5455fb861c001125dc25"),
          Map.entry(-3, "4cd88ee12d42abe5fc417cca5a01baac"),
          Map.entry(-2, "436cdf1964184276fdbce3f148296994"),
          Map.entry(-1, "d3d251f275071212fec6ac6aa3e880fa"),
          Map.entry(0, "a28341e910992ea0fedb40ef4a39b11a"),
          Map.entry(1, "1d87be977216913600312d0aaf4d5e4f"),
          Map.entry(2, "833d4be75101a927023507c89ee24202"),
          Map.entry(3, "db5a50fd224c4f3303ce2738fe0e9ccb"),
          Map.entry(4, "208703a6c06ce86a04cbb56b4c564873"),
          Map.entry(5, "7f0b22518868c73b05c51dbee0a7e0d2"),
          Map.entry(6, "47065ef40a67558c07534793e540787e"),
          Map.entry(7, "2145e0308672962607e55a9b9bca833f"),
          Map.entry(8, "984c4fb386bfe7e6091b61fcd60cb4b5"),
          Map.entry(9, "e0bb794a268fbc030adc7689a656003c"),
          Map.entry(10, "3d1f5ffc3ec5317d0cc10a721af8bdce"),
          Map.entry(11, "0eb7c66b58423abd0d1d81913dbea95a"),
          Map.entry(12, "b0f3418ce01b21950ea320361bb0b746"),
          Map.entry(13, "4cc6ba744cc80ff40ff2bbb917eb7c79"),
          Map.entry(14, "d4995d6123e150d710dbe67d2194f56d"),
          Map.entry(15, "2845cc195a54a4c41214c37d8b9c4d9b"),
          Map.entry(16, "cb31bd1a0776574e13e7567174be9d56"),
          Map.entry(17, "c19c69edcc6c33931564d92d240c1c8a"),
          Map.entry(18, "01d43543c09f78e716642fd520e6089b"),
          Map.entry(19, "37658176c5de79fa17deac57a0ba430c"),
          Map.entry(20, "77b32e59244359e51910c9d54e459a2e"),
          Map.entry(21, "12e77ef4988c85bf1aa0c2c6a83247d7"),
          Map.entry(22, "09662a80502adc641b8438a206211f96"),
          Map.entry(23, "997015f45e4792401cd4ce2d5807222e"),
          Map.entry(24, "16b8e3c85a75eff21d77acca8e1c8bae"),
          Map.entry(25, "0c4a5c96628e53651f05ba0ad1c056e2"),
          Map.entry(26, "0960371139409ffc21417c2506bc6d94"),
          Map.entry(27, "658ec1ddb597d69221c6c1eb81d31994"),
          Map.entry(28, "27f4c79c5099553b22f50cf2279c1587"),
          Map.entry(29, "c7ad8ead33b9b3cd24e16afa912b8c53"),
          Map.entry(30, "6b44fcac23e470b52623c7beadb4f960"),
          Map.entry(31, "541279f5c646bbbe27a3d33880c25058"),
          Map.entry(32, "144dd93eeacc85ec282a1cb506dbd85f"),
          Map.entry(33, "ce5a99cba9d216c62a1848eaec51dc82"),
          Map.entry(34, "08d4a177a8e144652a6e66a038798784"),
          Map.entry(35, "1bec090faea3c1cd2c7878c5d3315cbe"),
          Map.entry(36, "48d1e037e91e09682db75128e90774f3"),
          Map.entry(37, "19af4e9217d58df32e76d48a5ab1552c"),
          Map.entry(38, "3a62e89b7c0af425305e283ed937119c"),
          Map.entry(39, "75486cefb1d39530315f38fcc7b2801d"),
          Map.entry(40, "197fc6f2b7bbdcef328a54a360c0e2d1"),
          Map.entry(41, "132d0728aaba5f9833439edf4343769d"),
          Map.entry(42, "e8d4c32253c89e83354995372022eb4a"),
          Map.entry(43, "f3d9b19a5c93f09135ec942424ed8d85"),
          Map.entry(44, "6ebe20526c8b3b64374b4d00e2e8ca4a"),
          Map.entry(45, "7c35e87a5b6572d6390cb1c310936967"),
          Map.entry(46, "5c13529615c1fffa3a53079ec3f224c9"),
          Map.entry(47, "c8895b159f258d9d3b67f884f2765678"),
          Map.entry(48, "00e3a3a1497268ad3d499380cb90756e"),
          Map.entry(49, "952c49b423070cdb3d77823f0bfb3ec5"),
          Map.entry(50, "21225cac2901be7b3f71f747855b4075"),
          Map.entry(51, "d9270f14c780ad2d40f62f006c89407d"),
          Map.entry(52, "3c051b0882a0941341e578001422e8bb"),
          Map.entry(53, "8911e37ea813bd7943564fad071d045e"),
          Map.entry(54, "143507ee0e7658dc43db4c35d275d6d1"),
          Map.entry(55, "273dad2efc8fb0fc45ee0f3aa1409ff0"),
          Map.entry(56, "10a4e74446762dc646c4a7824290d418"),
          Map.entry(57, "5c415d7bc6ae1b22484d12ec977c29e9"),
          Map.entry(58, "9c1de8f614e3578e49eed7b622172965"),
          Map.entry(59, "a0e0c8e68b8481164a429a8a0bfd07b2"),
          Map.entry(60, "b95fd0753669e0d94ca8a31b62a31cd3"),
          Map.entry(61, "43dfdc8d0e685eb84db52e6f962160cb"),
          Map.entry(62, "383374a1b7d0f8994e3917de680d07ca"),
          Map.entry(63, "89e844489ba71f464f9b422efc4c3ff4"),
          Map.entry(64, "7dd165f75ec13e1051c32f23328af068"),
          Map.entry(65, "85fe8b3348e49af152f80bda4335bdfb"),
          Map.entry(66, "68a882b17b8f000353bcf7f3818ffbbf"),
          Map.entry(67, "7d175efaf03a95d555c1c7b8caaf5573"),
          Map.entry(68, "6542ed36634b595d569e484079aab12d"),
          Map.entry(69, "9caf7fe6ef0d0e215717d7385706e2d4"),
          Map.entry(70, "e67e81064ae9e0b75878645ba500190b"),
          Map.entry(71, "b9cc4efb247632cc5a7532d52b46bf2d"),
          Map.entry(72, "5c42b93809ab91e65bd7659a6477c2e9"),
          Map.entry(73, "ace024aa6951721e5d43086d660217be"),
          Map.entry(74, "5abb6a902fe2d66e5e4c2df2a9eabd83"),
          Map.entry(75, "5abdef0aaaf0e46a5fb07faab16563b3"),
          Map.entry(76, "da576a8a43f05f686088f993184a4fb1"),
          Map.entry(77, "aed8ffc354f6038361dbc52cbdcf4151"),
          Map.entry(78, "596a7f61b930f0b2631ebedd1aaad72b"),
          Map.entry(79, "27aba82c1a865d7f64ea34ba12dbfc79"),
          Map.entry(80, "ee58804784a3113866388857441422ae"),
          Map.entry(81, "27af6df879169334671be8556d1ce7f3"),
          Map.entry(82, "dc55b02c91d65c4d67d92ffa5235c243"),
          Map.entry(83, "6f188289db081d0368f7bc3bf95d8d99"),
          Map.entry(84, "713bc714d7f9d35d6ad403a514fc5d55"),
          Map.entry(85, "a43d326b0e3c575d6bfecd8d01972966"),
          Map.entry(86, "2e14791bf3e4dc936dc52c8f43aaa8b1"),
          Map.entry(87, "c3158695e7a704276ee06a7208b73dfb"),
          Map.entry(88, "60c0339e043fd3626f73932d735bfdb1"),
          Map.entry(89, "55ff29f31c0eb46271a586b660bb2af4"),
          Map.entry(90, "70f6d634ea1fbd547206018b2966e2e6"),
          Map.entry(91, "9b998dc6f2ca149b746afbeece74e0f1"),
          Map.entry(92, "a15f6a433bdda90a7553c1f439980d51"),
          Map.entry(93, "59a83cfcf258ae9575e5b2f0db1ebac6"),
          Map.entry(94, "974dbf96228f6cd07776ac805ddadd48"),
          Map.entry(95, "6c823d77396d8d1678a04a208ae285e1"),
          Map.entry(96, "d0c49c1544e5fc9d7a323091b9bbfa92"),
          Map.entry(97, "a422020cd9cb12ab7bf043e0a144bdb5"),
          Map.entry(98, "faed3575a39dd7617cc4f25bda640302"),
          Map.entry(99, "97d72e372fd7d9de7e913b96033738ae"));

  public static void main(String[] args) {
    var percentiles = new ArrayDeque<Percentile>();
    for (int percentile = -99; percentile <= 99; percentile++) {
      percentiles.add(new Percentile(percentile));
    }

    var string = new StringBuilder();
    string.append("private static final Map<Integer, String> TRACE_IDS = Map.ofEntries(");
    while (!percentiles.isEmpty()) {
      var percentile = percentiles.remove();
      boolean keepGoing = true;
      while (keepGoing) {
        var traceId = IdGenerator.random().generateTraceId();

        long hash = OtelEncodingUtils.longFromBase16String(traceId, 16);
        if (percentile.contains(hash)) {
          string
              .append("\n    Map.entry(")
              .append(percentile.value)
              .append(", \"")
              .append(traceId)
              .append("\")");
          if (!percentiles.isEmpty()) {
            string.append(",");
          }
          keepGoing = false;
        }
      }
    }
    string.append(");");

    System.out.println(string);
  }

  private static class Percentile {
    private final int value;
    private final long lower;
    private final long upper;

    private Percentile(int value) {
      this.value = value;
      if (value < 0) {
        this.lower = (long) (Long.MAX_VALUE * ((double) (value) / 100));
        this.upper = (long) (Long.MAX_VALUE * ((double) (value + 1) / 100));
      } else {
        this.lower = (long) (Long.MAX_VALUE * ((double) (value - 1) / 100));
        this.upper = (long) (Long.MAX_VALUE * ((double) value / 100));
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
