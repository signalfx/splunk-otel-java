# Upgrading the Agent

Upgrade the agent on a cycle that lets you pick up enhancements while giving you time to 
test the changes.

## Watch for new releases

How can you find out about new releases? The best way is to "Watch" this repo
from the "Watch" button in the upper-right corner of the main repository 
page. If you don't want to be notified for every pull request and issue, you can 
choose "Custom" under "Watch" and select just the "Releases" checkbox.

Another approach is to just check in regularly with the GitHub 
[releases page](https://github.com/signalfx/splunk-otel-java/releases) for this 
project. 

## When Does Splunk Release?

Because we are a downstream distribution of [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation),
we generally release within a few working days of every upstream release. We also release when we 
have new features or enhancements that we want to make available.

We contribute to upstream OpenTelemetry and are typically aware of the release cycle
over there. As a result, depending on the timing of certain patch releases, we may sometimes
skip a release to reduce the "release thrash" from our distribution.

## When To Upgrade

In general, we recommend that you upgrade to each new version shortly after it is released. Ideally,
you should upgrade within a few weeks of a release. Given that OpenTelemetry is a very active 
community, consistent, frequent upgrades will help limit the number of changes between upgrades. 

Upgrades should be intentional and version numbers should be pinned in your build pipeline.

## Upgrade Verification

The Java Instrumentation is a piece of code that runs alongside your code. Because of the rich 
diversity across user deployment environments and the massive number of frameworks and libraries
(and versions!) that are covered, we simply cannot universally guarantee that every release
version will be 100% compatible in every deployment.

Users should verify the upgrade with each new version.

## Best Practices

We recommend following these best practices in order to reduce upgrade risk:

* Read the release notes and changelog for each release. This should help you to determine when
  a given release has specific changes that are relevant to your software stack.
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

