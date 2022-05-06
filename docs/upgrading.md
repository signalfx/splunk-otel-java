# Upgrading the Splunk Distribution of OpenTelemetry Java

Upgrade the agent on a cycle that lets you pick up enhancements while giving you time to 
test the changes.

## Watch for new releases

To find out about new [releases](https://github.com/signalfx/splunk-otel-java/releases), add
yourself to the main repository watch list. To do this, click **Watch** on the main repository page. If
you don't want notifications for every pull request and issue, choose **Custom** and clear all options
except **Release**.

## When does Splunk Distribution of OpenTelemetry Java release?

Because we are a downstream distribution of [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation),
we generally release within a few working days of every upstream release. We also release when we 
have new features or enhancements that we want to make available.

We contribute to upstream OpenTelemetry and are typically aware of the release cycle
over there. As a result, depending on the timing of certain patch releases, we may sometimes
skip a release to reduce the "release thrash" from our distribution.

## When to upgrade?

In general, we recommend that you upgrade to each new version shortly after it is released. Ideally,
you should upgrade within a few weeks of a release. Given that OpenTelemetry is a very active 
community, consistent, frequent upgrades will help limit the number of changes between upgrades. 

Upgrades should be intentional and version numbers should be pinned in your build pipeline.

## Upgrade verification

The Java Instrumentation is code that runs alongside your code. Because of the rich 
diversity across user deployment environments and the massive number of frameworks and libraries
(and versions!) that are covered, we simply cannot universally guarantee that every release
version will be 100% compatible in every deployment.

Users should verify the upgrade with each new version.

## Best practices

To reduce the risk of problems with an upgrade, do the following:

* Read the release notes and changelog for each release, to help you determine when the release has
  changes that might affect your software stack. Give special consideration to specific mentions of 
  libraries, frameworks, and tools that your software uses.
* Never put untested code into production! This should be obvious, but you should first verify that 
  a new build works in a staging or pre-production environment before promoting it to production.
* Use canary instances. Let the canaries operate with the code before releasing the code to 
  production. Run the canaries for at least a few hours, and preferably for a few days.
* Minimize the number of dependencies (including instrumentation!) changing in a given release.
  It becomes significantly more difficult to determine the root cause of a problem when multiple
  dependencies have been upgraded at the same time.
* Don't use snapshot builds in production.
* Look closely at the release notes to identify breaking changes. The OpenTelemetry tries to
  minimize breaking changes, but sometimes they're needed to fix problems or improve the code in the
  long term.

## Understanding version numbers

Refer to
the [versioning document](https://github.com/signalfx/splunk-otel-java/blob/main/VERSIONING.md) to
learn more about version numbers. Major versions contain a large number of changes, which might
result in increased risk to your production environment. The most common releases are marked with a
minor version, and they contain a modest number of changes. Patch releases are infrequent, and they
pinpoint specific fixes or enhancements.

