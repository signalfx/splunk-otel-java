package com.splunk.opentelemetry.instrumentation.nocode;

import java.lang.reflect.Method;

// JSPS stands for Java-like String-Producing Statement.  FIXME describe in more detail and pick a better nane
public class JSPS {

  public static String evaluate(String jsps, Object thiz, Object[] params) {
    try {
      return unsafeEvaluate(jsps, thiz, params);
    } catch (Throwable t) {
      // FIXME better logging
      System.out.println("can't eval jsps: "+t);
      return null;
    }
  }


  // FIXME improve performance - reduce allocations, etc.
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
    while(curIndex < jsps.length()) {
      while(jsps.charAt(curIndex) == '.' || Character.isWhitespace(jsps.charAt(curIndex))) {
        curIndex++;
      }
      int openParen = jsps.indexOf('(', curIndex);
      String method = jsps.substring(curIndex, openParen).trim();
      int closeParen = jsps.indexOf(')', openParen);
      String paramString = jsps.substring(openParen + 1, closeParen).trim();
      if (paramString.isEmpty()) {
        // FIXME how does javaagent open the class?  getting exceptions gere
        // e.g., with hashmap.entrySet().size() on the entryset accessor
        curObject = curObject.getClass().getMethod(method).invoke(curObject);
      } else {
        if (paramString.startsWith("\"") && paramString.endsWith("\"")) {
          String passed = paramString.substring(1, paramString.length()-1);
          Method m = findMethod(curObject, method,
              String.class, Object.class);
          curObject = m.invoke(curObject, passed);
        } else if (paramString.equals("true") || paramString.equals("false")) {
          Method m = findMethod(curObject, method,
              boolean.class, Boolean.class, Object.class);
          curObject = m.invoke(curObject, Boolean.parseBoolean(paramString));
        } else if (paramString.matches("[0-9]+")) {
          try {
            Method m = findMethod(curObject, method, int.class, Integer.class, Object.class);
            int passed = Integer.parseInt(paramString);
            curObject = m.invoke(curObject, passed);
          } catch (NoSuchMethodException tryLongInstead) {
            Method m = findMethod(curObject, method, long.class, Long.class, Object.class);
            long passed = Long.parseLong(paramString);
            curObject = m.invoke(curObject, passed);
          }
        } else {
          throw new UnsupportedOperationException("Can't parse \""+paramString+"\" as literal parameter");
        }
      }
      curIndex = closeParen + 1;
    }
    return curObject == null ? null : curObject.toString();
  }

  // FIXME must be a better way to look through a series of type options
  private static Method findMethod(Object curObject, String methodName, Class<?>... paramTypesToTryInOrder) throws NoSuchMethodException{
    Class c = curObject.getClass();
    for(Class<?> paramType : paramTypesToTryInOrder) {
      try {
        return c.getMethod(methodName, paramType);
      } catch (NoSuchMethodException e) {
        // try next one
      }
    }
    throw new NoSuchMethodException(methodName + " with single parameter matching given type");
  }
}
