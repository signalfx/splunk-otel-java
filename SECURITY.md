# Security

## Reporting Security Issues

Please *DO NOT* report security vulnerabilities with public GitHub issue
reports. Please [report security issues here](
https://advisory.splunk.com/report).

## Dependencies

This project relies on a variety of external dependencies.
These dependencies are monitored by
[Renovate](https://github.com/apps/renovate).
Dependencies are [checked
periodically](https://github.com/signalfx/splunk-otel-java/blob/main/.github/renovate.json5)
and associated pull requests are opened automatically. Upgrading to the [latest
release](https://github.com/signalfx/splunk-otel-java/releases)
is recommended to ensure you have the latest security updates. If a security
vulnerability is detected for a dependency of this project then either:

- You are running an older release
- A new release with the updates has not been cut yet
- The updated dependency has not been merged likely due to some breaking change
  (in this case, we will actively work to resolve the issue)
- The dependency has not released an updated version with the patch
