# Add the JVM agent to Java servers

The following sections show how to add the path to the Splunk OTel agent for Java using the [supported servers](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#application-servers).

* [JBoss EAP / WildFly](#jboss-eap--wildfly)
* [Jetty](#jetty)
* [GlassFish / Payara](#glassfish--payara)
* [Tomcat / TomEE](#tomcat--tomee)
* [WebLogic](#weblogic)
* [WebSphere Liberty Profile](#websphere-liberty-profile)
* [WebSphere Traditional](#websphere-traditional)

## JBoss EAP / WildFly

Add the `javaagent` argument to the `standalone` configuration file:

- On Linux and macOS, add the following line at the end of the `standalone.conf` file:
   ```
   JAVA_OPTS="$JAVA_OPTS -javaagent:/path/to/splunk-otel-javaagent.jar"
   ```
- On Windows, add the following line at the end of the `standalone.conf.bat` file:
   ```
   set "JAVA_OPTS=%JAVA_OPTS% -javaagent:<Drive>:\path\to\splunk-otel-javaagent.jar"
   ```

## Jetty

Add the path to the JVM agent using the `-javaagent` argument:

```
java -javaagent:/path/to/splunk-otel-javaagent.jar -jar start.jar
```

Alternatively you can add `-javaagent` argument to your `jetty.sh` or `start.ini` files:

-  If you use the `jetty.sh` file to start jetty, add the following line to `<jetty_home>/bin/jetty.sh`:
   ```
   JAVA_OPTIONS="${JAVA_OPTIONS} -javaagent:/path/to/splunk-otel-javaagent.jar"
   ```
- If you use the `start.ini` file to define JVM arguments, add the `javaagent` argument right below the `--exec` option:
   ```
   #===========================================================
   # Sample Jetty start.ini file
   #-----------------------------------------------------------
   --exec
   -javaagent:/path/to/splunk-otel-javaagent.jar
   ```

## GlassFish / Payara

Add the path to the JVM agent to the settings using the `asadmin` command-line tool:

- On Linux, enter the following command:
   ```
   <server_install_dir>/bin/asadmin create-jvm-options "-javaagent\:/path/to/splunk-otel-javaagent.jar" 
   ```
- On Windows, enter the following command:
   ```
   <server_install_dir>\bin\asadmin.bat create-jvm-options '-javaagent\:<Drive>\:\\path\\to\\splunk-otel-javaagent.jar'
   ```

You can also add the `-javaagent` argument from the Admin Console:

1. Open the GlassFish Admin Console at http://localhost:4848
2. Go to **Configurations > server-config > JVM Settings**.
3. Select **JVM Options** and click **Add JVM Option**.
4. In the blank field, enter the path to `splunk-otel-javaagent.jar`:
   ```
   -javaagent:/path/to/splunk-otel-javaagent.jar
   ```
5. Click **Save** and restart the GlassFish server.

> Tip: Check that the `domain.xml` file in your domain directory contains a `<jmv-options>` entry for the agent.

## Tomcat / TomEE

Add the path to the JVM agent to your Tomcat/TomEE startup script:

- On Linux, add the following line to `<tomcat_home>/bin/setenv.sh`:
   ```
   CATALINA_OPTS="$CATALINA_OPTS -javaagent:/path/to/splunk-otel-javaagent.jar"
   ```
- On Windows, add the following line to `<tomcat_home>\bin\setenv.bat`:
   ```
   set CATALINA_OPTS=%CATALINA_OPTS% -javaagent:"<Drive>:\path\to\splunk-otel-javaagent.jar"
   ```

## WebLogic

Add the path to the JVM agent to your WebLogic domain startup script:

- On Linux and macOS, add the following line to the `<domain_home>/bin/startWebLogic.sh` file:
   ```
   export JAVA_OPTIONS="$JAVA_OPTIONS -javaagent:/path/to/splunk-otel-javaagent.jar"
   ```
- On Windows, add the following line to the `<domain_home>\bin\startWebLogic.cmd` file:
   ```
   set JAVA_OPTIONS=%JAVA_OPTIONS% -javaagent:"<Drive>:\path\to\splunk-otel-javaagent.jar"
   ```

> For managed server instances, add the `-javaagent` argument using the admin console.

## WebSphere Liberty Profile

Add the path to the JVM agent to the `jvm.options` file:

- Open the `jvm.options` file:
   - For a single server, create or edit the `${server.config.dir}/jvm.options` file.
   - For all servers, create or edit the `${wlp.install.dir}/etc/jvm.options` file.
- Add the following line:
   ```
   -javaagent:/path/to/splunk-otel-javaagent.jar
   ```
- Save the file and restart the server.

## WebSphere Traditional

1. Open the WebSphere Admin Console
2. Select **Servers > Server type > WebSphere application servers**.
3. Click on the desired server.
4. Select **Java and Process Management > Process Definition**.
5. Click on **Java Virtual Machine**.
6. In the **Generic JVM arguments**, enter the path to `splunk-otel-javaagent.jar`:
   ```
   -javaagent:/path/to/splunk-otel-javaagent.jar
   ```
7. Press **OK**. When asked, save the master configuration and restart the server.