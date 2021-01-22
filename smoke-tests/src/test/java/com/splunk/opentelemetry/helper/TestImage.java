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

package com.splunk.opentelemetry.helper;

public class TestImage {
  public final Platform platform;
  public final String imageName;
  public final boolean isProprietaryImage;

  public TestImage(Platform platform, String imageName, boolean isProprietaryImage) {
    this.platform = platform;
    this.imageName = imageName;
    this.isProprietaryImage = isProprietaryImage;
  }

  @Override
  public String toString() {
    return imageName + "(" + platform + ")";
  }

  public static TestImage linuxImage(String imageName) {
    return new TestImage(Platform.LINUX_X86_64, imageName, false);
  }

  public static TestImage proprietaryLinuxImage(String imageName) {
    return new TestImage(Platform.LINUX_X86_64, imageName, true);
  }

  public static TestImage proprietaryWindowsImage(String imageName) {
    return new TestImage(Platform.WINDOWS_X86_64, imageName, true);
  }

  public enum Platform {
    WINDOWS_X86_64,
    LINUX_X86_64,
  }
}
