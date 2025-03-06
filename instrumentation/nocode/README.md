# "nocode" instrumentation

Please don't use this if you have the ability to edit the code being instrumented.

Set `SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE=/path/to/some.yml`

Where the yml looks like 
```
- class: foo.Foo
  method: foo
  spanName: this.getName()
  attributes:
    - key: "business.context"
      value: this.getDetails().get("context")

- class: foo.Foo
  method: throwSomething
  spanKind: CLIENT
  spanStatus: 'returnValue.code() > 3 ? "OK" : "ERROR"`
  attributes:
    - key: "special.header"
      value: 'param0.headers().get("special-header").substring(5)"'
```

Expressions are written in [JEXL](https://commons.apache.org/proper/commons-jexl/reference/syntax.html) and may use
the following variables:
  - `this` - which may be null for a static method
  - `param0` through `paramN` where 0 indexes the first parameter to the method
  - `returnValue` which is only defined for `spanStatus` and may be null (if an exception is thrown or the method returns void)
  - `error` which is only defined for `spanStatus` and is the `Throwable` thrown by the method invocation (or null if a normal return)

