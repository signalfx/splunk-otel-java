# Add the JVM agent to Java servers

The following sections show how to add the path to the Splunk OTel agent for Java using the [supported servers](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#application-servers).

## JBoss EAP / WildFly

Add the `javaagent` argument to the `standalone` configuration file:

- On Linux and macOS, add the following line at the end of the `standalone.conf` file:
   ```
   JAVA_OPTS="$JAVA_OPTS -javaagent:/path/to/splunk-otel-javaagent.jar"
   ```
- On Windows, dd the following line at the end of the `standalone.conf.bat` file:
   ```
   set "JAVA_OPTS=%JAVA_OPTS% -javaagent:<Drive>:\path\to\splunk-otel-javaagent.jar"
   ```

## Jetty

Configure the path to the JVM agent either in `start.ini` or in `jetty.sh`:

- In the `start.ini` file, add the `javaagent` argument right below the `--exec` option:
   ```
   #===========================================================
   # Sample Jetty start.ini file
   #-----------------------------------------------------------
   --exec
   -javaagent:/path/to/splunk-otel-javaagent.jar
   ```
-  In the `jetty.sh` file, add the `javaagent` argument:
   ```
   JAVA_OPTIONS="${JAVA_OPTIONS} -javaagent:/path/to/splunk-otel-javaagent.jar"
   ```

## Glassfish / Payara

Add the path to the JVM agent to the settings using the `asadmin` command-line tool:

- On Linux, enter the following command:
   ```
   <server_install_dir>/bin/asadmin create-jvm-options "-javaagent\:/path/to/splunk-otel-javaagent.jar" 
   ```
- On Windows, enter the following command:
   ```
   <server_install_dir>\bin\asadmin.bat create-jvm-options '-javaagent:<Drive>:\path\to\splunk-otel-javaagent.jar'
   ```

You can also instrument your application from the Admin Console:

1. Open the GlassFish Admin Console at http://localhost:4848
2. Go to **Configurations > server-config > JVM Settings**.
3. Select **JVM Options** and click **Add JVM Option**.
4. In the blank field, enter the path to `splunk-otel-javaagent.jar`:

   `-javaagent:/path/to/splunk-otel-javaagent.jar`

5. Click **Save** and restart the GlassFish server.

> Tip: Check that the `domain.xml` file in your domain directory contains a `<jmv-options>` entry for the agent.

## Tomcat / TomEE

Add the path to the JVM agent to your Tomcat/TomEE startup script:

- On Linux, add the following line to `<tomcat_home>/bin/setenv.sh`:
   ```
   CATALINA_OPTS="$CATALINA_OPTS -javaagent:/path/to/splunk-otel-javaagent.jar"
   ```
- On Windows, add the following line to `%CATALINA_BASE%\bin\setenv.bat`:
   ```
   set CATALINA_OPTS=%CATALINA_OPTS% -javaagent:"<Drive>:\path\to\splunk-otel-javaagent.jar"
   ```

## Weblogic

Add the path to the JVM agent to the `startWebLogic` file of your administration server:

- On Linux and macOS, add the following line to the `bin/startWebLogic.sh` file:
   ```
   export JAVA_OPTIONS="$JAVA_OPTIONS -javaagent:/path/to/splunk-otel-javaagent.jar"
   ```
- On Windows, add the following line to the `<install_dir>/<config>/<domain_name>/startWebLogic.cmd` file:
   ```
   set JAVA_OPTIONS=%JAVA_OPTIONS% -javaagent:"<Drive>:\path\to\splunk-otel-javaagent.jar"
   ```

> For managed server instances, add the `javaagent` argument using the admin console.

## Websphere Liberty Profile

Add the path to the JVM agent to the `jvm.options` file:

- Open the `jvm.options` file:
   - For a single server, open the server configuration `${server.config.dir}/jvm.options`.
   - For all servers, open the common configuration in `${wlp.install.dir}/etc/jvm.options`
- Add the following line:
   ```
   -javaagent:/path/to/splunk-otel-javaagent.jar
   ```
- Save the file and restart the server.
