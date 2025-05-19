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
          Map.entry(-99, "0bc2c1a02bfe0f00828410841d3bc302"),
          Map.entry(-98, "fe0e63651d784e2283702c942c7b2a54"),
          Map.entry(-97, "a6c836a3b947f8ed8461dbbc975a5f43"),
          Map.entry(-96, "ba473ac2b200f0e1863f9a39b5669411"),
          Map.entry(-95, "7a689a3a6260402e869f3b91dbeb9f41"),
          Map.entry(-94, "34dad32f39eaa9f0886a04866604916c"),
          Map.entry(-93, "c7f0558d6ca1590e89fc0b6072fc2cd3"),
          Map.entry(-92, "14614583912305da8a818a3c758c19ab"),
          Map.entry(-91, "1688fbbd1ff872428bff33766d8e6b59"),
          Map.entry(-90, "433a9ae88a1a43e68dd1ebf09e5c534e"),
          Map.entry(-89, "2763902bba0d8f5e8e41b32d5753e2df"),
          Map.entry(-88, "b2a1666455251c9d90970f886f49e5f6"),
          Map.entry(-87, "0df75f89fb3eb22691befee28d017ac2"),
          Map.entry(-86, "fe00206b1168fca392b2b016e3b6388a"),
          Map.entry(-85, "356451f8168597a893361d17014a5d45"),
          Map.entry(-84, "8e7e8ef94eb28420947e711d0585e41d"),
          Map.entry(-83, "e48c651240f66985961197894fea251a"),
          Map.entry(-82, "70d386a41d0e56d8983b8ffb63503b5d"),
          Map.entry(-81, "d62379f9921bcd91997a4628a9c07948"),
          Map.entry(-80, "8f4878d91be1b6219a76595df41890c5"),
          Map.entry(-79, "51189b6ecf6a75b29be8cd1103a34f2a"),
          Map.entry(-78, "82dc07fc557318a79cec76e8ecb1ae4a"),
          Map.entry(-77, "920f23b3aabf01729d731c3f2f7f7283"),
          Map.entry(-76, "9a0272f5f0ed8eb49fe32f7d4a642739"),
          Map.entry(-75, "52117854725194a7a0ca7cfe36f1ba8b"),
          Map.entry(-74, "d3ff80f7229821b2a19562f768a9cd53"),
          Map.entry(-73, "4c4561506a298040a390e47053e09fd3"),
          Map.entry(-72, "501acd4899a14395a4ef42530da333b5"),
          Map.entry(-71, "5f611de48b444e1fa648a594c2a921f9"),
          Map.entry(-70, "3e097463a127a020a735cca5ee0b372a"),
          Map.entry(-69, "b826d2f2fdecf67da860ce43f5303ae9"),
          Map.entry(-68, "cae51e9964f2f3cba914f687c34b16c6"),
          Map.entry(-67, "c305036abd9b6c29ab5a40b3c85fbe5b"),
          Map.entry(-66, "6069b32c775a8d41ab934644c3943a81"),
          Map.entry(-65, "456cddbc7ae3b3c8adc8163f98083cb2"),
          Map.entry(-64, "156e640ce38b3136ae72a2958612e885"),
          Map.entry(-63, "297b6e5748112e5bafed72b52a8c5ac9"),
          Map.entry(-62, "8d1277c58b3b77beb0e96a5480c2bc46"),
          Map.entry(-61, "2234ebd018a2dfcab274e0a8c1b31451"),
          Map.entry(-60, "12116f05a84d674ab396177e07b10789"),
          Map.entry(-59, "f0cb136524a56982b57b8fe6dc92eb62"),
          Map.entry(-58, "399b827f518cc09ab6da8c142fbbfd24"),
          Map.entry(-57, "eb757c36af601e95b83b8b179d68f885"),
          Map.entry(-56, "906eb7d797842ce3b909f3097ae10240"),
          Map.entry(-55, "f8980e267c48cd8bba4e0d776b771cfd"),
          Map.entry(-54, "6b29883bd1f1f301bb72e9861839adc9"),
          Map.entry(-53, "df3c1c2ff4e9f801bd617155e7dcbb81"),
          Map.entry(-52, "8fa95c44ab0b48f0be617b07aa4daf31"),
          Map.entry(-51, "069eb2b3d396798dbf494f17237f58d1"),
          Map.entry(-50, "53dc54c7662d79ffc03d2e51701b879b"),
          Map.entry(-49, "f9b194655ea8ca08c1f16eec64aee20e"),
          Map.entry(-48, "b77523f5119a30d4c34942d9ce325dad"),
          Map.entry(-47, "64080124f6774baac435e7b6f938c8a2"),
          Map.entry(-46, "63e2a46e50cd070bc618851c9b7e4dfb"),
          Map.entry(-45, "fa3b0e4e8e730006c716646c27a4958e"),
          Map.entry(-44, "004a94151a9c635ac7b5290d68c94692"),
          Map.entry(-43, "97dbf29b9a976c42c9f4e6c13166bbf2"),
          Map.entry(-42, "55dcfe8d6d20ee0bcb7c8fe2239f47fa"),
          Map.entry(-41, "c79b47125299483bcbc32979796159c8"),
          Map.entry(-40, "118115a674012af7cdfde0a795e9c917"),
          Map.entry(-39, "223f52a61870b4d7ce53cfd989e90f70"),
          Map.entry(-38, "d57c58f596cbc586d0557335d5111e4b"),
          Map.entry(-37, "b8e8cb768dc8cd04d1d7336dd9dee19a"),
          Map.entry(-36, "7f7e403775508d65d2a0f3b81532cb91"),
          Map.entry(-35, "2bd598306bd3dcd5d3a403c4cf6c53a8"),
          Map.entry(-34, "f6f5ee20a887b86fd5b270f8da5c53bc"),
          Map.entry(-33, "25d2bfa0aaf45bc6d665241275d70bba"),
          Map.entry(-32, "b486b456ca2658f8d727efb24890d4d7"),
          Map.entry(-31, "8682a26d789fb278d8a60e502c520f39"),
          Map.entry(-30, "d302a9fa68c5afb4da622048ba1a7271"),
          Map.entry(-29, "eb13b00d5e4beeeadc075254f4b98022"),
          Map.entry(-28, "635717b49b672cd9dcadfd5426dd89b6"),
          Map.entry(-27, "f17073ece4b3cc75de2915efb1f67403"),
          Map.entry(-26, "1c9be31cde5c1104df6e3eaa2c8e94c8"),
          Map.entry(-25, "b8c9a78003d04060e001558f23b7b3f8"),
          Map.entry(-24, "92bdf95662450aeae1509aa1683a0d42"),
          Map.entry(-23, "f7d33c24ec6e757ce2a951cb0429f4bd"),
          Map.entry(-22, "bbe0b6678ecd26bae3ecb6ce2653e9a5"),
          Map.entry(-21, "4d0190613e005144e55c2933182cb127"),
          Map.entry(-20, "49dc64eb5cbf6716e6d0934663b15d9c"),
          Map.entry(-19, "b0ef3f9a12cf14afe7c58b07ec7eb381"),
          Map.entry(-18, "82dc602eee14893de9babf20d0df58bb"),
          Map.entry(-17, "99035eab01724309ea58b002a36b69ea"),
          Map.entry(-16, "6f8521a03946473beca933ba79147eb3"),
          Map.entry(-15, "4c49cc63bed24f38ecffbd5a91f056de"),
          Map.entry(-14, "7156ad284508c81eef1f84000b7d6f4f"),
          Map.entry(-13, "99a85c29be68e57aeff55d237cf44092"),
          Map.entry(-12, "624524fabba19986f12888d62748ab13"),
          Map.entry(-11, "f4e97ffca38fa978f283fb2df9c18162"),
          Map.entry(-10, "7c4e3f7dcfa9a76bf3425dc2696df401"),
          Map.entry(-9, "4fddbdfc63448148f4ef1376a999bc16"),
          Map.entry(-8, "c1088afa7ff5b013f5c9c921556f2968"),
          Map.entry(-7, "fefce71484ffd68cf7493186ce5c121a"),
          Map.entry(-6, "66c169b6ef9bc55df94b0e49dbcc8f3c"),
          Map.entry(-5, "117c6bcfb2de4926fa6dd5265b65137f"),
          Map.entry(-4, "bf1cd2d7ce977f4dfb0e80ed320edd2a"),
          Map.entry(-3, "8d07a9c810c5c189fc83af88599b1ae3"),
          Map.entry(-2, "51d19ef5602167fffe38b0a7e15e334c"),
          Map.entry(-1, "896cd39c95474f59ff4caf6e79889ef9"),
          Map.entry(1, "026c6bcc83adde89005ab6073adba517"),
          Map.entry(2, "e40c06cb323914e3023fea35e526296b"),
          Map.entry(3, "f07d72baea65ea5d02f50d41926ce74e"),
          Map.entry(4, "f395386a3e0d3c6f041024d82a73c47e"),
          Map.entry(5, "a746a2964721255e05ae7f9e4475ba0a"),
          Map.entry(6, "447bb02e32f3eeaa076ee099394af8d5"),
          Map.entry(7, "68327e17b2f3aafc07cdc15bfd1d264f"),
          Map.entry(8, "33ea09444f6e67a909d25f79da9c51f8"),
          Map.entry(9, "622b9d78a0e07dff0a6b2e30e21bbc0f"),
          Map.entry(10, "af98b3f907b564970c7a79a54b19a4ea"),
          Map.entry(11, "5359d864f09483aa0d3d792ebde41a0c"),
          Map.entry(12, "2a3f7123db40d3c10f1bc9a6dad94bb5"),
          Map.entry(13, "7195322de59d936310024490d3400b8e"),
          Map.entry(14, "e507602406d3874710c516330e69c783"),
          Map.entry(15, "de4e6202d57fff4a1305e32f3da391d8"),
          Map.entry(16, "e0da04332e6e283213869a2bbfd59897"),
          Map.entry(17, "9315c712711db421156bfdff477bf08a"),
          Map.entry(18, "29a486274d79c83d1705cec76b67594e"),
          Map.entry(19, "e4468e515c47c90e173224273e29abb6"),
          Map.entry(20, "59401a8ae7af97591992fb00addc867b"),
          Map.entry(21, "09e52472e753ac001a4f0c65e98a8818"),
          Map.entry(22, "7140d1a2a07cd1861bea118efb0cbb19"),
          Map.entry(23, "5f01a939cbccce531d54631af90961f3"),
          Map.entry(24, "62d4c7ede977a4ed1e791e3f7b06d262"),
          Map.entry(25, "33aad4bcf008d0271ec14fa541e542e0"),
          Map.entry(26, "20582978c5ecef39205d292a91c63bd8"),
          Map.entry(27, "0fe89e941d16503c2173ee5e5b6669a6"),
          Map.entry(28, "0f5b256d80c6179822bc087d1a48c117"),
          Map.entry(29, "7e6a5c876c60c772241c88d38e1a08b7"),
          Map.entry(30, "32025e629d3fa4df25419c8ceedad12b"),
          Map.entry(31, "12b5cfe7f03196d32754834d0dc23efe"),
          Map.entry(32, "17ff6bd5c397402c28f3f48fe92b9986"),
          Map.entry(33, "1a0f9a8f5f46ba952980c1ffaf20db3e"),
          Map.entry(34, "418922e8f096c0802b1fe34f00e6d504"),
          Map.entry(35, "66bd22f6b9670b9d2c9acdef320ae39d"),
          Map.entry(36, "91bb28e7fe4f8eae2de8befd2d987cf3"),
          Map.entry(37, "8092f0a311c8e7422f24f3922c176496"),
          Map.entry(38, "072f867dbff2ddf62f7865ee89c1bf23"),
          Map.entry(39, "6feb34049d57e10b30fffd315c0f3153"),
          Map.entry(40, "b32cededb71cbbd632bf5d14e019c636"),
          Map.entry(41, "11d0f2f2ac71105733ee056565a6018a"),
          Map.entry(42, "7f0075662d12600d35b406ec8da64545"),
          Map.entry(43, "d4c033615150a9dc36685bb7bcce5b4a"),
          Map.entry(44, "3c9211a90dfe993837f1225e926ab468"),
          Map.entry(45, "574adfddf179789238e8756d2d7f8767"),
          Map.entry(46, "95d1263dc5b5d8393a2015b2913b9c79"),
          Map.entry(47, "5f54bb8fd0d67cdc3bd8b692b8852d94"),
          Map.entry(48, "0f52cd637634093e3d44ab0940653cba"),
          Map.entry(49, "4733ee531458499e3dd7fc3ceaadf34b"),
          Map.entry(50, "763c7e90f524d6363f2f34218fc2e907"),
          Map.entry(51, "3b4e81026d0194fe40af10169f76299f"),
          Map.entry(52, "f0d58aabbe0e55234223c53dfa66431f"),
          Map.entry(53, "07004888ca77b57642e694d90c84b41a"),
          Map.entry(54, "5605e76726c2d999450e5850beb3013b"),
          Map.entry(55, "0a44fe3991aad2cd45c85a40dfae74db"),
          Map.entry(56, "d9806d2768e44f2546e1d84b4d3b97a3"),
          Map.entry(57, "9d37e5723977249d47ef038c2bd4b256"),
          Map.entry(58, "67c6d3573af2fb0c49ece9ab07a24931"),
          Map.entry(59, "4af70d943d3cdf074b6d1c684a1cc380"),
          Map.entry(60, "b82f0b83d3e620f94c54499e32e99f40"),
          Map.entry(61, "d8d0513d34ed0e044d733f3fc3ccfecf"),
          Map.entry(62, "8994ce5a851a454f4ee4845dd8e6ba76"),
          Map.entry(63, "1d1b1b7289dc74884fb090430fff2439"),
          Map.entry(64, "a02db97616e1beaa519c38731a9ff32a"),
          Map.entry(65, "ebfe809877f2c56552d3097670dbac0e"),
          Map.entry(66, "9eeed3939a59032f536825460669a302"),
          Map.entry(67, "b98502e4b2c2572e551704aea24b27e4"),
          Map.entry(68, "af0cc4c02548d54e55e12492fe7cbd8c"),
          Map.entry(69, "5845d4fda70f66a457a6563e88262b09"),
          Map.entry(70, "c2f5350c61b11a6c5998ff5f3f1d8696"),
          Map.entry(71, "d617f526a4c1895359e6c3f8b104a8ff"),
          Map.entry(72, "cf71dcf6cff105c75c008f7c1491810b"),
          Map.entry(73, "c994898ecf5381ed5c89732a4061ecd4"),
          Map.entry(74, "94eb8a5ddcc743275e4a2fd99898df4e"),
          Map.entry(75, "22cf0a7de1ed396f5f7318ac4be75fa5"),
          Map.entry(76, "dae52daa54d4f86060ebe6b9dc1aeaec"),
          Map.entry(77, "e23ae8f197c028e061c2f435619a9acc"),
          Map.entry(78, "268f3cde7587451263cc89db6261e99b"),
          Map.entry(79, "145481f7735eebd264fbda6da72a94ea"),
          Map.entry(80, "9a63fa60fc6ae04e65374b9a7ef3260d"),
          Map.entry(81, "4afe36b635320aea666adfa7d0e4840e"),
          Map.entry(82, "a63d3d4cd3e9a8a1681e32e087ead4d3"),
          Map.entry(83, "180fa9e3a49c6b556939f11bf5738b1e"),
          Map.entry(84, "b373e99ac08ac69d6ac442dfb8dc87eb"),
          Map.entry(85, "325f7e73c46d9c0d6bb2bd29f2fb4597"),
          Map.entry(86, "0f9faf3e581fc3cf6ce0047e6a997ac8"),
          Map.entry(87, "c38d76e8a9c757af6f1c4c9a05a11188"),
          Map.entry(88, "750c32a81661da507094aa269987ce4c"),
          Map.entry(89, "1dc8f416d63b1e0c719ec6fa011174a8"),
          Map.entry(90, "476f95abeb0d6d9272bb9abd90d05109"),
          Map.entry(91, "e85add42dfd81e3973f50276441ce602"),
          Map.entry(92, "d63af0f8ec02b0fc74be9d73f98c8c31"),
          Map.entry(93, "9465673a27000ba676e82f024df0ce3b"),
          Map.entry(94, "0f5c0b08cb3f7bf3783e729cfeec12fc"),
          Map.entry(95, "82be8faafe50feca78c93c65d8aa74aa"),
          Map.entry(96, "bf5820acb585fe6c7a71c67d14143322"),
          Map.entry(97, "eb6ac13d81f643ce7b92124a303d7b13"),
          Map.entry(98, "50fa12eee82bcd317d1baac57dc1f896"),
          Map.entry(99, "47799b838b1742aa7e11de1d04f9b628"));

  public static void main(String[] args) {
    var percentiles = new ArrayDeque<Percentile>();
    for (int percentile = -99; percentile <= 99; percentile++) {
      if (percentile != 0) {
        percentiles.add(new Percentile(percentile));
      }
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
