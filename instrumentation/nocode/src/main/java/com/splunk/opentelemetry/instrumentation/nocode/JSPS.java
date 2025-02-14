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

package com.splunk.opentelemetry.instrumentation.nocode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
/**
 * JSPS stands for Java-like String-Producing Statement.  A JSPS is
 * essentially a single call in Java (as though it ends with a semicolon), with
 * some limitations.  Its purpose is to allow pieces of nocode instrumentation
 * (attributes, span name) to be derived from the instrumentated context.
 * As some illustrative examples:
 * this.getHeaders().get("X-Custom-Header").substring(5)
 * param0.getDetails().getCustomerAccount().getAccountType()
 * The limitations are:
 * - no access to variables other than 'this' and 'paramN' (N indexed at 0)
 * - no control flow (if), no local variables, basically nothing other than a single chain of method calls
 * - Method calls are limited to either 0 or 1 parameters currently
 * - Parameters must be literals and only integral (int/long), string, and boolean literals are currently supported
 */
public final class JSPS {
  private static final Logger logger = Logger.getLogger(JSPS.class.getName());
  private static final Class[] NoParamTypes = new Class[0];

  public static String evaluate(String jsps, Object thiz, Object[] params) {
    try {
      return unsafeEvaluate(jsps, thiz, params);
    } catch (Throwable t) {
      logger.warning("Can't evaluate {" + jsps + "}: " + t);
      return null;
    }
  }

  // FIXME Might be nice to support escaping quotes in string literals...
  private static String unsafeEvaluate(String jsps, Object thiz, Object[] params) throws Exception {
    jsps = jsps.trim();
    int nextDot = jsps.indexOf('.');
    String var = jsps.substring(0, nextDot).trim();
    Object curObject = null;
    if (var.equals("this")) {
      curObject = thiz;
    } else if (var.startsWith("param")) {
      int varIndex = Integer.parseInt(var.substring("param".length()));
      curObject = params[varIndex];
    }
    int curIndex = nextDot;
    while (curIndex < jsps.length()) {
      curIndex = jsps.indexOf('.', curIndex);
      while (jsps.charAt(curIndex) == '.' || Character.isWhitespace(jsps.charAt(curIndex))) {
        curIndex++;
      }
      int openParen = jsps.indexOf('(', curIndex);
      String method = jsps.substring(curIndex, openParen).trim();
      int closeParen = jsps.indexOf(')', openParen);
      String paramString = jsps.substring(openParen + 1, closeParen).trim();
      if (paramString.isEmpty()) {
        Method m = findMatchingMethod(curObject, method, NoParamTypes);
        curObject = m.invoke(curObject);
      } else {
        if (paramString.startsWith("\"") && paramString.endsWith("\"")) {
          String passed = paramString.substring(1, paramString.length() - 1);
          Method m = findMethodWithPossibleTypes(curObject, method, String.class, Object.class);
          curObject = m.invoke(curObject, passed);
        } else if (paramString.equals("true") || paramString.equals("false")) {
          Method m =
              findMethodWithPossibleTypes(
                  curObject, method, boolean.class, Boolean.class, Object.class);
          curObject = m.invoke(curObject, Boolean.parseBoolean(paramString));
        } else if (paramString.matches("[0-9]+")) {
          try {
            Method m =
                findMethodWithPossibleTypes(
                    curObject, method, int.class, Integer.class, Object.class);
            int passed = Integer.parseInt(paramString);
            curObject = m.invoke(curObject, passed);
          } catch (NoSuchMethodException tryLongInstead) {
            Method m =
                findMethodWithPossibleTypes(
                    curObject, method, long.class, Long.class, Object.class);
            long passed = Long.parseLong(paramString);
            curObject = m.invoke(curObject, passed);
          }
        } else {
          throw new UnsupportedOperationException(
              "Can't parse \"" + paramString + "\" as literal parameter");
        }
      }
      curIndex = closeParen + 1;
    }
    return curObject == null ? null : curObject.toString();
  }

  // This sequence of methods is here because:
  // - we want to try a variety of parameter types to match a literal
  //      e.g., someMethod(5) could match someMethod(int) or someMethod(Object)
  // - module rules around reflection make some "legal" things harder and require
  //   looking for a public class/interface matching the method to call
  //      e.g., a naive reflective call through
  //         this.getSomeHashMap().entrySet().size()
  //      would fail simply because the HashMap$EntrySet implementation
  //      is not public, even though the interface it's being call through is.
  private static Method findMatchingMethod(
      Object curObject, String methodName, Class[] actualParamTypes) throws NoSuchMethodException {
    Method m = findMatchingMethod(methodName, curObject.getClass(), actualParamTypes);
    if (m == null) {
      throw new NoSuchMethodException(
          "Can't find matching method for " + methodName + " on " + curObject.getClass().getName());
    }
    return m;
  }

  // Returns null for none found
  private static Method findMatchingMethod(String methodName, Class clazz, Class[] actualParamTypes) {
    if (clazz == null) {
      return null;
    }
    if (Modifier.isPublic(clazz.getModifiers())) {
      try {
        return clazz.getMethod(methodName, actualParamTypes);
      } catch (NoSuchMethodException nsme) {
        // keep trying
      }
    }
    // not public, try interfaces and supertype
    for (Class iface : clazz.getInterfaces()) {
      Method m = findMatchingMethod(methodName, iface, actualParamTypes);
      if (m != null) {
        return m;
      }
    }
    return findMatchingMethod(methodName, clazz.getSuperclass(), actualParamTypes);
  }

  private static Method findMethodWithPossibleTypes(
      Object curObject, String methodName, Class<?>... paramTypesToTryInOrder)
      throws NoSuchMethodException {
    Class c = curObject.getClass();
    for (Class<?> paramType : paramTypesToTryInOrder) {
      try {
        return findMatchingMethod(curObject, methodName, new Class[] {paramType});
      } catch (NoSuchMethodException e) {
        // try next one
      }
    }
    throw new NoSuchMethodException(methodName + " with single parameter matching given type");
  }
}
