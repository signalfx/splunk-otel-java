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

package com.splunk.opentelemetry.netty.v4_0;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class NettyInstrumentationModule extends InstrumentationModule {
  public NettyInstrumentationModule() {
    super("netty", "netty-4.0");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    // Class added in 4.1.0 and not in 4.0.56 to avoid resolving this instrumentation completely
    // when using 4.1.
    return not(hasClassesNamed("io.netty.handler.codec.http.CombinedHttpHeaders"));
  }

  @Override
  protected String[] additionalHelperClassNames() {
    return new String[] {
      ServerTimingHeader.class.getName(),
      this.getClass().getPackage().getName() + ".ServerTimingHandler",
      this.getClass().getPackage().getName() + ".ServerTimingHandler$HeadersSetter"
    };
  }

  // run after the upstream netty instrumentation
  @Override
  public int getOrder() {
    return 1;
  }

  // enable the instrumentation only if the server-timing header flag is on
  @Override
  protected boolean defaultEnabled() {
    return super.defaultEnabled() && ServerTimingHeader.shouldEmitServerTimingHeader();
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new ChannelPipelineInstrumentation());
  }
}
