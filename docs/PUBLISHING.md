# GitHub Publishing Checklist

This checklist prepares GEM for a public GitHub repository and a GitHub beta Release with binary artifacts.

## Current Release Candidate

- Version: `0.1.47`
- Repository: `grid-event-manager/gem`
- Public source head: release-prep head for the `0.1.47` beta.
- Release artifacts folder before upload: local operator-owned release folder for version `0.1.47`.

Artifacts:

- `gema_0.1.47_amd64.deb`
- `gema-0.1.47.msi`
- `gema-1.0.47.dmg`
- `gem-android-0.1.47-debug.apk`
- `SHA256SUMS.txt`

## GitHub Release Asset Feasibility

GitHub Releases can carry these four artifacts as release assets. GitHub currently allows up to 1000 release assets per release, each under 2 GiB, with no total release size or bandwidth limit stated for release assets.

Do not commit binary artifacts into Git. GitHub blocks repository files larger than 100 MiB and recommends Releases for distributing large binaries.

References:

- https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
- https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github

## Before Creating The Repository

1. Create the ANVLL GitHub organization or account.
2. Create a public repository for GEM.
3. Enable private vulnerability reporting if available.
4. Decide whether issue discussions are open immediately or limited during beta.
5. Decide whether release artifacts remain beta-unsigned or wait for signing.

## Local Pre-Push Gate

Run from the public repository root:

```bash
git status --short
./gradlew --no-daemon :gem-ui:check
./gradlew --no-daemon :apps:desktop:check :apps:android:check checkGemBoundaries
git diff --check
```

Run a public-leak scan before the first push:

```bash
rg -n --hidden --glob '!.git/**' --glob '!**/build/**' --glob '!**/.gradle/**' --glob '!local.properties' '(password|passwd|secret|token|api[_-]?key|bearer|credential|sl[-]creds|C[D]P|/media/[^[:space:]]+|[^[:space:]]+@[^[:space:]]+\.local)' .
```

Expected hits are limited to source-code vocabulary, tests with dummy values, and documentation that warns against secret leakage. Real credentials, private hostnames, and private local paths must not appear.

## First Push

After the GitHub repository exists:

```bash
git remote add origin git@github.com:ANVLL/<repo-name>.git
git push -u origin main
```

Use the actual organization and repository names chosen on GitHub.

## Release Upload

Create the release tag and upload all user-test artifacts:

```bash
export GEM_REPO=/path/to/gem/public
export GEM_RELEASE_ARTIFACTS=/path/to/release-artifacts/0.1.47
cd "$GEM_RELEASE_ARTIFACTS"
gh release create v0.1.47 \
  --repo grid-event-manager/gem \
  --title "GEM 0.1.47 beta" \
  --prerelease \
  --notes-file "$GEM_REPO/docs/releases/0.1.47.md" \
  gema_0.1.47_amd64.deb \
  gema-0.1.47.msi \
  gema-1.0.47.dmg \
  gem-android-0.1.47-debug.apk \
  SHA256SUMS.txt
```

## Release Caveats To Keep Visible

- Linux package is unsigned unless distributed through a signed apt repository later.
- Windows MSI is unsigned unless a code-signing certificate is added.
- macOS DMG is unsigned and not notarized unless Apple Developer signing is added.
- Android artifact is a debug-signed sideload APK for beta testing unless a release signing pipeline is added.
