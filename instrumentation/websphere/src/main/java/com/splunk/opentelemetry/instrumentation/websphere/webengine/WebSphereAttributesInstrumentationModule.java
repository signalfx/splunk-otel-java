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

package com.splunk.opentelemetry.instrumentation.websphere.webengine;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import com.ibm.ws.runtime.PlatformHelper;
import com.splunk.opentelemetry.javaagent.bootstrap.WebengineHolder;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class WebSphereAttributesInstrumentationModule extends InstrumentationModule {

  public WebSphereAttributesInstrumentationModule() {
    super("websphere");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("com.ibm.ws.runtime.WsServerImpl");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new Instrumentation());
  }

  public static class Instrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("com.ibm.ws.runtime.WsServerImpl");
    }

    @Override
    public void transform(TypeTransformer typeTransformer) {
      typeTransformer.applyAdviceToMethod(
          named("start"),
          WebSphereAttributesInstrumentationModule.class.getName() + "$WebengineInitializedAdvice");
    }
  }

  @SuppressWarnings("unused")
  public static class WebengineInitializedAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit() {
      WebengineHolder.trySetVersion(PlatformHelper.getPlatformVersion());
      WebengineHolder.trySetName("websphere");
    }
  }
}
