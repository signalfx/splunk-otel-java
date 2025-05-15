# "nocode" instrumentation

Stability: under active development; breaking changes can occur

Please don't use this if you have the ability to edit the code being instrumented.

By using this feature you are essentially changing your application's code. Doing so without understanding
the behavior of the code you are adding may cause any or all of:
  - Data corruption
  - Deadlocks or application crashes
  - Memory leaks
  - Greatly increased latency or timeouts
  - Errors reported back to your application's clients

## How to add custom instrumentation without source access

Set `SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE=/path/to/some.yml`

Where the yml looks like
```yaml
- class: foo.Foo
  method: foo
  spanName: this.getName()
  attributes:
    - key: "business.context"
      value: this.getDetails().get("context")

- class: foo.Foo
  method: doStuff
  spanKind: CLIENT
  spanStatus: 'returnValue.code() > 3 ? "OK" : "ERROR"'
  attributes:
    - key: "special.header"
      value: 'param0.headers().get("special-header").substring(5)'
```

Expressions are written in [JEXL](https://commons.apache.org/proper/commons-jexl/reference/syntax.html) and may use
the following variables:
  - `this` - which may be null for a static method
  - `param0` through `paramN` where 0 indexes the first parameter to the method
  - `returnValue` which is only defined for `spanStatus` and may be null (if an exception is thrown or the method returns void)
  - `error` which is only defined for `spanStatus` and is the `Throwable` thrown by the method invocation (or null if a normal return)

## More complex class/method selection

You may use more complex logic for class or method selection.  The following example shows all the options:

```yaml
- class:
    and:
      - or:
        - named: some.specific.Name
        - nameMatches: some.*Regex.*
      - hasSuperType: some.superclass.or.Interface
  method:
    and:
      - nameMatches: methodName.*Regex.*
      - not:
          nameMatches: specificMethodNameToNotMatch
      - hasParameterCount: 2
      - hasParameterOfType: 0 int
```

  - `and:`, `or:`, `not:`, `named:`, and `nameMatches:` apply to both class and method matchers
  - `hasSuperType:` is only for classes and matches the given type exactly, or anything that directly or indirectly extends from or implements it
  - `hasParameterCount:` is only for methods and requires an exact match (you can combine with `or:`/`and:`/`not:` to enable logic like "more than 1 parameter")
  - `hasParameterOfType` uses a 0-based index and requires an exact match to the declared type of the parameter
