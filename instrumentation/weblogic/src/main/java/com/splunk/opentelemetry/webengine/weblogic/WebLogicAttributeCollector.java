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

package com.splunk.opentelemetry.webengine.weblogic;

import com.splunk.opentelemetry.javaagent.bootstrap.WebengineHolder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class WebLogicAttributeCollector {
  private static final String CONTEXT_ATTRIBUTE_NAME = "otel.weblogic.attributes";
  public static final String REQUEST_ATTRIBUTE_NAME = "otel.webengine";

  public static void attachWebengineAttributes(HttpServletRequest servletRequest) {
    WebLogicEntity.Request request = WebLogicEntity.Request.wrap(servletRequest);

    Map<?, ?> attributes = fetchWebengineAttributes(request.getContext());
    request.instance.setAttribute(REQUEST_ATTRIBUTE_NAME, attributes);
  }

  private static Map<?, ?> fetchWebengineAttributes(WebLogicEntity.Context context) {
    if (context.instance == null) {
      return null;
    }

    Object value = context.instance.getAttribute(CONTEXT_ATTRIBUTE_NAME);

    if (value instanceof Map<?, ?>) {
      return (Map<?, ?>) value;
    }

    // Do this here to avoid duplicate work
    storeWebengineIdentity(context);

    Map<String, String> webengine = collectWebengineAttributes(context);
    context.instance.setAttribute(CONTEXT_ATTRIBUTE_NAME, webengine);
    return webengine;
  }

  private static void storeWebengineIdentity(WebLogicEntity.Context context) {
    WebengineHolder.trySetName("WebLogic Server");
    WebengineHolder.trySetVersion(detectVersion(context));
  }

  private static Map<String, String> collectWebengineAttributes(WebLogicEntity.Context context) {
    WebLogicEntity.Bean applicationBean = context.getBean();
    WebLogicEntity.Bean webServerBean = context.getServer().getBean();
    WebLogicEntity.Bean serverBean = webServerBean.getParent();
    WebLogicEntity.Bean clusterBean = WebLogicEntity.Bean.wrap(serverBean.getAttribute("Cluster"));
    WebLogicEntity.Bean domainBean = serverBean.getParent();

    Map<String, String> attributes = new HashMap<>();
    attributes.put("webengine.weblogic.domain", domainBean.getName());
    attributes.put("webengine.weblogic.cluster", clusterBean.getName());
    attributes.put("webengine.weblogic.server", webServerBean.getName());
    attributes.put("webengine.weblogic.application", applicationBean.getName());

    return attributes;
  }

  private static String detectVersion(WebLogicEntity.Context context) {
    String serverInfo = context.instance.getServerInfo();

    if (serverInfo != null) {
      for (String token : serverInfo.split(" ")) {
        if (token.length() > 0 && Character.isDigit(token.charAt(0))) {
          return token;
        }
      }
    }

    return "";
  }
}
