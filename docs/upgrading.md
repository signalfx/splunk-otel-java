# Upgrading the Agent

This document provides user guidance around agent upgrades and versioning.
Like every other piece of software included in your project, the agent 
should be upgraded frequently enough to pick up ongoing enhancements 
while allowing room for testing. 

## Watching For New Releases

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
* Use canaries. Let a couple of instances cook with the new code for a few hours or few
  days before promoting to production.
* Minimize the number of dependencies (including instrumentation!) changing in a given release.
  It becomes significantly more difficult to determine the root cause of a problem when multiple
  dependencies have been upgraded at the same time.
* While advanced users with sophisticated build pipelines _may_ maintain compatibility with 
  bleeding-edge snapshot builds, a snapshot build should really never be deployed to production
  unless absolutely necessary. This should be quite uncommon.
* Give special consideration to breaking changes in the release notes. While the community tries
  to minimize breaking changes, they are sometimes present to fix problems or to make the
  code better in the long term.

## What Do These Version Numbers Mean?

We have a [versioning document](https://github.com/signalfx/splunk-otel-java/blob/main/VERSIONING.md)
that provides more detail. Please note that major versions will contain a greater number of
changes which results in increased risk. Minor versions are the most common releases and contain
modest number of changes, and patch releases are infrequent and usually contain only pinpoint
specific fixes or enhancements.

