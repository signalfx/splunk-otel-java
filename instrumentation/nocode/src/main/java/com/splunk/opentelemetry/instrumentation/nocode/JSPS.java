package com.splunk.opentelemetry.instrumentation.nocode;

import java.lang.reflect.Method;
import java.util.logging.Logger;

// JSPS stands for Java-like String-Producing Statement.  A JSPS is
// essentially a single call in Java (as though it ends with a semicolon), with
// some limitations.  Its purpose is to allow pieces of nocode instrumentation
// (attributes, span name) to be derived from the instrumentated context.
// As some illustrative examples:
   // this.getHeaders().get("X-Custom-Header").substring(5)
   // param0.getDetails().getCustomerAccount().getAccountType()
// The limitations are:
   // no access to variables other than 'this' and 'paramN' (N indexed at 0)
   // no control flow (if), no local variables, basically nothing other than a single chain of method calls
   // Methods calls are limited to either 0 or 1 parameters currently
   // Parameters must be literals and only integral (int/long), string, and boolean literals are currently supported
public class JSPS {
  private final static Logger logger = Logger.getLogger(JSPS.class.getName());

  public static String evaluate(String jsps, Object thiz, Object[] params) {
    try {
      return unsafeEvaluate(jsps, thiz, params);
    } catch (Throwable t) {
      logger.warning("Can't evaluate {"+jsps+"}: "+t);
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
    while(curIndex < jsps.length()) {
      curIndex = jsps.indexOf('.', curIndex);
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
