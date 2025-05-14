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

import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
          Map.entry(-99, "395cdc5ab5726d9c1650a16a78682fb0"),
          Map.entry(-98, "e26d9db69dbb8a4e22230bff6ec2059b"),
          Map.entry(-97, "6132ac7d5e75a74ab28929bca9a8d8f7"),
          Map.entry(-96, "d5d96e2ecedefdb558a4ad5b0c703b32"),
          Map.entry(-95, "751fa53be43a3af26fd68969e490bb62"),
          Map.entry(-94, "bcbb73f14a7dbdb0f52f10143dfc9cfa"),
          Map.entry(-93, "8a7f2a03205e6c60156eb71737f8200f"),
          Map.entry(-92, "1271e92ac84d359538192e57ce059869"),
          Map.entry(-91, "43e26018d5e52376e1811a111ec3f8b6"),
          Map.entry(-90, "0d82b237d628133a8a2c3435123f5972"),
          Map.entry(-89, "4dbf865b8ae5cd3aaa74e337990cb5ed"),
          Map.entry(-88, "aeed34b4736eaa1fc395e2286202abcc"),
          Map.entry(-87, "0c142d42b7a60099619fcc75fcdd2723"),
          Map.entry(-86, "5e8f1e912aadde3481732a80e013485d"),
          Map.entry(-85, "86ad09ed730c0162bb449823951b661f"),
          Map.entry(-84, "09a952ee8c281c41c073823055556ee0"),
          Map.entry(-83, "4f36573e0654551289baad76983777d5"),
          Map.entry(-82, "8d185bef853bfe7a15083bdc4ed23835"),
          Map.entry(-81, "19a6c272e18176e789bce0b4dd479fe8"),
          Map.entry(-80, "581d1f89eed629c661244600edb3bb1b"),
          Map.entry(-79, "0e81a00fbe68f558c13e5eda9511b20b"),
          Map.entry(-78, "d683e7d619e6345449ce191f848262a3"),
          Map.entry(-77, "04f51f59fcac192ccccb18106ef2003f"),
          Map.entry(-76, "bcb3e61ea3009fc7bccb81abccb8b11d"),
          Map.entry(-75, "475644dd22f082c1fac783f3cc732dbd"),
          Map.entry(-74, "327a3a6101cd9ec609e0bb8452a9a02e"),
          Map.entry(-73, "75a4998eadc038cc5ed190b2c2e88938"),
          Map.entry(-72, "feed138ba44cf3f5676de160381dc501"),
          Map.entry(-71, "857a0afe28da24f3c3eb1835cc52b315"),
          Map.entry(-70, "ea7360b43d45f8154968d5ea6bcfa05a"),
          Map.entry(-69, "47c0067d92b1ea10c9962a93cac33e09"),
          Map.entry(-68, "9b81ec1407e17e258d8885bf78509e33"),
          Map.entry(-67, "8e324f5409b343fc8d5459819839a071"),
          Map.entry(-66, "558e8200063d702b235f536fa7544031"),
          Map.entry(-65, "7fd4d4e9d556a82ebf922579ae1c25a6"),
          Map.entry(-64, "5006a81ba009b71aae9adaa7270533fc"),
          Map.entry(-63, "6607e4451ea3491b712e6450553e426b"),
          Map.entry(-62, "d58cd68e6885be449668b80b1d172fde"),
          Map.entry(-61, "5ad1922e57b2d99c556bcd4c41304102"),
          Map.entry(-60, "a18567bccb75e5acb1a52e0d308a0783"),
          Map.entry(-59, "1b93956b5172e964e36405271a46cd00"),
          Map.entry(-58, "42ffe6261034213f1e2e382f0f5c9868"),
          Map.entry(-57, "f10a111f23d01ff8a1b90dd4d76c3eeb"),
          Map.entry(-56, "a07b1e52462f10048c1c19453d4a2893"),
          Map.entry(-55, "17d25940a3751f40eb41d0ee39d582bc"),
          Map.entry(-54, "fb13099307b6fd97d7e40119fe68c7b0"),
          Map.entry(-53, "e9fa5f4aadf78070802b6107b2906ac8"),
          Map.entry(-52, "e59f2c31290d0d089d0b7d8c389a699a"),
          Map.entry(-51, "c8f474f3db76ec5f4e42d4ef4b0afcd0"),
          Map.entry(-50, "f0460c3dcdbf32ab678d263d7aa849ab"),
          Map.entry(-49, "c864875b184c8673d00b4b4d0ffc3118"),
          Map.entry(-48, "caffee1ff20a53a1693dbce11e2ec7f8"),
          Map.entry(-47, "5af6bb8a1f6b2536f5091565ae0e431c"),
          Map.entry(-46, "29d71114a0ea7a682fe5d22867725050"),
          Map.entry(-45, "4d53024b8734608bec03ee36b8a333fc"),
          Map.entry(-44, "c60280f3e983df50427055defdabb247"),
          Map.entry(-43, "bdc7566afedcff486dc0e2788086db19"),
          Map.entry(-42, "bf91e73e3be0e6e286b1491223ee8f2f"),
          Map.entry(-41, "40940112d1e78127e198e368ff8739cd"),
          Map.entry(-40, "f87f6e55c460ed9af2ada03c6cc3cb22"),
          Map.entry(-39, "2e19072d2ff49b0829b96f6b6f662c43"),
          Map.entry(-38, "30e6f8c0a3210a3deb8bbeb4f7d25caf"),
          Map.entry(-37, "c417d001ce5990d917a93dbda8f9e597"),
          Map.entry(-36, "33c638dd9358f6d7ef848dccfa69b9c5"),
          Map.entry(-35, "e31183195475f5d1446cb00c3b705152"),
          Map.entry(-34, "0019ddcb9ed84764d0865543f6ef3843"),
          Map.entry(-33, "f708a704dea4aaa0b6fd4f80f563b601"),
          Map.entry(-32, "c98f58b848d0999b2bfbb69ac256282b"),
          Map.entry(-31, "5a6498ff8677fa3083deb4c66e698d7f"),
          Map.entry(-30, "852dd47e7bfb278f1c346eff58775c59"),
          Map.entry(-29, "16ab39ef9f84be05a9aaca5701363a72"),
          Map.entry(-28, "1541b46b9d6f99b4fe21a2c0797dd195"),
          Map.entry(-27, "c944863a153b1892e3e2eb4f1a7c63d9"),
          Map.entry(-26, "3898c4a8fb455e8c4488a2e9e0a2c011"),
          Map.entry(-25, "84fecf8da64fea44499f85beac061dde"),
          Map.entry(-24, "be79bd589c8e27a8130f5439089fe7c6"),
          Map.entry(-23, "14b3e9a39b24664ea55b4a0801dfb1b9"),
          Map.entry(-22, "b5c085c2b3181b4eb494fcec232099da"),
          Map.entry(-21, "00b03698dffa72d248766a9243baf181"),
          Map.entry(-20, "ca07a27bc2271debbf0a31b29e8c2160"),
          Map.entry(-19, "2971a804f0b108f40aede28b4007b644"),
          Map.entry(-18, "6b2cb90b9ac82168b8d7620c5552a648"),
          Map.entry(-17, "061bda1a42f5e4630cb584dbdb42c504"),
          Map.entry(-16, "1d1a4a63be7fa81b77d75d2192bff51a"),
          Map.entry(-15, "4160ed9f72e7961f6c78ce00ab9bb513"),
          Map.entry(-14, "201cf087f13c52a91a0b9cbf733c39f9"),
          Map.entry(-13, "ec252b932a2ba20c7da04fe42ee93f71"),
          Map.entry(-12, "6e37db7a288ead1131dbc5a43f1f16e6"),
          Map.entry(-11, "5e72ea685d408db491c472cd3ec62a06"),
          Map.entry(-10, "70655363e78d8511cb61abe509db66f8"),
          Map.entry(-9, "a7667fb482a1c16caa9f996b9778781c"),
          Map.entry(-8, "a1cd8031ee5d7e2f3cc227b33833d673"),
          Map.entry(-7, "5b826d96caa76b103a0eced852344c6d"),
          Map.entry(-6, "aecf2426c7a393e4f90a48e86ea912cb"),
          Map.entry(-5, "3f2dd96966979708f88c221a34a11efb"),
          Map.entry(-4, "6574ecdb0a36883802f73a68bf8c697a"),
          Map.entry(-3, "7b75fb9eec60b59501158c09123e2175"),
          Map.entry(-2, "7afa6a054f63b30ee0535a2df4177150"),
          Map.entry(-1, "6753530eb8b6c0f271c2dfd92b1f4d12"),
          Map.entry(0, "ac09cb4912a75c5dbfae0a749988f7f8"),
          Map.entry(1, "29267f5586b9efbf5e18356605d68e04"),
          Map.entry(2, "840aad368ce0a73de7aa4d5905bf7ae6"),
          Map.entry(3, "06e84dd9e10634ee08b516904c314cd7"),
          Map.entry(4, "570c6f2d4572a2082db6a3b757fc3dfe"),
          Map.entry(5, "c27a22c4632603fd8f8d610552fcde11"),
          Map.entry(6, "ab956ebddfd59ae669a8a1e9d454488c"),
          Map.entry(7, "dd509e1502256bc9c015c92f9012cdef"),
          Map.entry(8, "36110bb00c474500734e948d394e766f"),
          Map.entry(9, "28f3823e9cbb6b70a3521d81d0510e07"),
          Map.entry(10, "633fc7a527c853c513a119ec2ff62688"),
          Map.entry(11, "1be722be9ed93504b023178b2a7a41d1"),
          Map.entry(12, "ba6b609408b3877fab1d14f06b21e7e0"),
          Map.entry(13, "316e9ebffb7f740b7b53cad5eb2021aa"),
          Map.entry(14, "e3c64506f1904248593c9124c17b6a04"),
          Map.entry(15, "189a18f081c103c3d0094b9424d9037d"),
          Map.entry(16, "db3a4a6fa43259a98390dedbfc9e71d9"),
          Map.entry(17, "d6ee18c830b7cd5fca802e94e74ce35a"),
          Map.entry(18, "91e3cb5b88355749f90433851f51fb57"),
          Map.entry(19, "632f5d37928aad8fc3f29163092eaee8"),
          Map.entry(20, "c49f6e0eb0d2af066b2770f2918577d0"),
          Map.entry(21, "0f82a2dbd58a5d5fe2a0d42672837d16"),
          Map.entry(22, "cf03b7075e0a2ce9faa708a00f027023"),
          Map.entry(23, "36121a0c3db464442995917ad888226d"),
          Map.entry(24, "69520d249cb9316d31bb073ba586f908"),
          Map.entry(25, "4926db263c78b9f90890c2e044d59435"),
          Map.entry(26, "f41a3916169e074e47721f39812d71f0"),
          Map.entry(27, "93ac30f5e4766decc0256c72cdbaf8f5"),
          Map.entry(28, "03674b141a3c8f0f90819ee8c8dd5451"),
          Map.entry(29, "c423bbaa83dbce32f71346ce8b72a32e"),
          Map.entry(30, "2bcf3185ea4789257e74e997ca232cb2"),
          Map.entry(31, "15bbbcb104c7322279903695e0c99484"),
          Map.entry(32, "46a77c57601a680b51ec40ed0ba91ff2"),
          Map.entry(33, "1ab5f312cbd7be71c6f8f5d18de3ea2d"),
          Map.entry(34, "30553c590c7f5551105b59d999c51331"),
          Map.entry(35, "b67608afd211eadde0925cdbcb7b7bee"),
          Map.entry(36, "8ea0eabd1dc7b15fe4ee1673cd2d3359"),
          Map.entry(37, "4e725cef6f37489c394a7979019f743e"),
          Map.entry(38, "1888d57ff727586f1bb9b8ff8241c080"),
          Map.entry(39, "97a0f5ea4ee150f54d0cc25c92422894"),
          Map.entry(40, "8de43ac72ef1e2b759e596425b56dd64"),
          Map.entry(41, "c3627f99c4787bf2306d4f3fdcf4b85c"),
          Map.entry(42, "9c4e53983b6f4a84a9e48804c6fb870e"),
          Map.entry(43, "367c818ef9f5b7ffbdebcb44d24ef3e2"),
          Map.entry(44, "e440eab638bbe6525aa9d062b08bd9ab"),
          Map.entry(45, "813edc5cc34dd5f24755a6aa951e1b90"),
          Map.entry(46, "357b4b46459c3fc0c7274b92039256c8"),
          Map.entry(47, "79db7493d745f7898cc897337b4756c0"),
          Map.entry(48, "cead6a63cbeaa2273d439c2fe7a032bb"),
          Map.entry(49, "027d7ede6cf3c6d7170e6d46610e8330"),
          Map.entry(50, "2f8d59d70b69bbf2a6142a45de724277"),
          Map.entry(51, "e92b65196e96a383449d3454e5cb5600"),
          Map.entry(52, "a38970e035052e9694bfa95c8705f11c"),
          Map.entry(53, "521fb887366ceaead3b0a66fffa91c8b"),
          Map.entry(54, "f9e41d15f6abc5151f0d5e9e376157ae"),
          Map.entry(55, "69482a23ca82a40d413b956964dfbd00"),
          Map.entry(56, "1c942129811f72478815c4f71b62df6f"),
          Map.entry(57, "5884b8b9a7f3da37a62610794d0bba46"),
          Map.entry(58, "25b02404439a6436361ea382bfffe14d"),
          Map.entry(59, "159efabcadcb21d6dc27aa69bf9eb281"),
          Map.entry(60, "f605fbd30c4fb74ee2a121ce9a1f7e48"),
          Map.entry(61, "d82f64b6367eb6e09443154231b54fbb"),
          Map.entry(62, "afabb56790c2e8edf69627eeae667896"),
          Map.entry(63, "9de59d99401de88753424dfa2e28fc47"),
          Map.entry(64, "d481a52eff013e35363e4a49f6352cb3"),
          Map.entry(65, "b3df6f44272aca47f348309a7df14821"),
          Map.entry(66, "a0d4ec4767badd85be8a32c31cc7b28d"),
          Map.entry(67, "5f37bcfa8162118aef300e636655360a"),
          Map.entry(68, "fc6629189a02a249cb8d8697503c60f3"),
          Map.entry(69, "541960b4aa1e4ef8d24d1330385f3a42"),
          Map.entry(70, "e4c03148147930cbfd008b471e4eb86c"),
          Map.entry(71, "de6dd0e958b5342809de42773d273408"),
          Map.entry(72, "adde51485bd837a88e607a2de6d06e41"),
          Map.entry(73, "8b26bdc3810664f91fba013b47929f73"),
          Map.entry(74, "453764603adf27ea56e043247a66ab99"),
          Map.entry(75, "0741dbc1a3fa8cb0746ce100c6bac3c4"),
          Map.entry(76, "cdee363997bf7639b3f249938fd1af36"),
          Map.entry(77, "9c4c892f7ae176ab431eb533f6404bc0"),
          Map.entry(78, "11f1061f4cb41e1b17e88b2df4ef0116"),
          Map.entry(79, "6cf3e721dbe189d471cb0d730f1cd87c"),
          Map.entry(80, "1d108bc70b3f16e585e687f4a3678df6"),
          Map.entry(81, "3dd5c7caafce2d4462bd84a74200a289"),
          Map.entry(82, "b72dfc375f575d61fcf4524868ff1693"),
          Map.entry(83, "98f552b6c763b90cef159af315adac68"),
          Map.entry(84, "fc146a8dac485a416d7c56e2c02aa44c"),
          Map.entry(85, "0bc310e1b01d6d8a99b4a3f5fe6a4a42"),
          Map.entry(86, "77caea93843dc154e8d56c77245f09e0"),
          Map.entry(87, "19474c2b16691de95b065653ea2a17ad"),
          Map.entry(88, "a31249eff39854e73f5bb7dc93eca38d"),
          Map.entry(89, "7263464414bb32b7f9746876267b2c36"),
          Map.entry(90, "e9eaedd6f02f5999af6e0e5226ce9752"),
          Map.entry(91, "9af6708ec04184b50c4fe54bd2442b7b"),
          Map.entry(92, "a8479ba182eebae8ccb38df611c252e5"),
          Map.entry(93, "e7adc1c69028dc16f91444f0971778d5"),
          Map.entry(94, "b8750f665078154958e5d2c14527abd5"),
          Map.entry(95, "f54ab97f670a4212935f7f78dba5b6a4"),
          Map.entry(96, "7804937fcc05b8159b74be49e38cbc58"),
          Map.entry(97, "a521bc9ebf35e6a5f4ddf3e8f783ad56"),
          Map.entry(98, "54b46a7da6653ec553e60b72ca52f56a"),
          Map.entry(99, "2852dffbf85fd78857853a4aad57f029"));

  @Test
  @Disabled
  void generateTraceIds() {
    Queue<Integer> ids = new ArrayDeque<>();
    for (int i = -99; i <= 99; i++) {
      ids.add(i);
    }

    var string = new StringBuilder();
    string.append("private static final Map<Integer, String> TRACE_IDS = Map.ofEntries(");
    while (!ids.isEmpty()) {
      int percentile = ids.remove();

      boolean keepGoing = true;
      while (keepGoing) {
        var traceId = IdGenerator.random().generateTraceId();
        int hash = traceId.hashCode();
        int asPercent = hash % 100;
        if (asPercent == percentile) {
          string.append("\n    Map.entry(" + percentile + ", \"" + traceId + "\")");
          if (!ids.isEmpty()) {
            string.append(",");
          }
          keepGoing = false;
        }
      }
    }
    string.append("\n};");

    System.out.println(string);
  }

  private SpecialTraceIds() {}
}
