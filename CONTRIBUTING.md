# Contributing to IKO

## Contributing

Thank you for your interest in contributing to IKO! We welcome bug reports, feature requests, and pull requests from the community.

1. **Open an issue** - Create an issue in the IKO issues repository describing the bug or feature. Tag the Product Owner; their GitHub handle can be found in the README. A member of the product team will get back to you to discuss the issue and proposed solution.

2. **Implement** - Create a branch off the active release-candidate branch (`rc/x.x.x`) and implement your changes. The active rc is determined by the maintainers.

3. **Open a draft PR** - Create a draft PR targeting the active `rc/x.x.x` branch. Targeting `main` directly is not allowed and is enforced by CI. If you cannot determine the correct rc branch, tag a @maintainer in the draft PR to ask.

4. **Apply a label** - Label your PR so it appears correctly in the auto-generated release notes:

   | Label | Release notes category |
   |---|---|
   | `breaking-change` | Breaking Changes |
   | `enhancement` | New Features |
   | `bug` | Other Changes |
   | `documentation` | Other Changes |

5. **Mark the PR as ready for review** - Once the work is complete and the SonarCloud check passes, mark the PR as ready for review. This notifies the maintainers, so please do not mark it ready while work is still in progress.

6. **Address any review feedback** - Respond to reviewer comments and await approval from the IKO team.

7. **Merge** - Once approved, merge the PR into the rc branch.

## Release Process

Once an `rc/x.x.x` branch is deemed ready for release by the maintainers:

1. **Merge the rc branch** - Open a PR from `rc/x.x.x` into `main` and merge it.
2. **Trigger the release workflow** - Go to GitHub Actions, select "Start release", and click "Run workflow" on the `main` branch.
3. **Enter the version** - Provide the version matching the rc branch (e.g. `1.2.0`). The version must be valid semver. Versions containing `-` (e.g. `1.2.0-beta.1`) are automatically published as a GitHub pre-release.
4. **Wait for the release workflow to complete** - The workflow builds the Docker image, pushes it to GHCR as both `ghcr.io/<org>/iko:<version>` and `:latest`, and creates a GitHub Release tagged `v<version>` with release notes generated from the merged PR labels.

> **Note:** A snapshot Docker image (tagged `main` and `snapshot-<timestamp>`) is automatically published to GHCR on every push to `main`, before the versioned release is triggered.
