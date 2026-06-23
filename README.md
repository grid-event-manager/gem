<table>
  <tr>
    <td width="88" valign="middle">
      <img src="apps/desktop/src/main/package/icons/gem.png" alt="GEM diamond logo" width="72">
    </td>
    <td valign="middle">
      <h1>GEM</h1>
      <strong>GRID EVENT MANAGER</strong>
    </td>
  </tr>
</table>

GEM is a small cross-platform notice manager for Second Life venue work. It helps an operator log in, choose groups, write a notice, optionally attach an existing landmark or texture, and send the notice to selected groups.

GEM is not affiliated with, sponsored by, or endorsed by Linden Lab. Second Life grid behaviour remains controlled by Linden Lab and the Second Life service.

This repository contains the public source tree for GEM. Internal planning notes, private test fixtures, credentials, live-grid evidence, and release-process records are not part of this public repository.

## Current Status

GEM is beta software. The `0.1.x` builds are intended for controlled testing and feedback. The app has working Linux, Windows, macOS, and Android artifacts, but public distribution still needs normal platform hardening such as installer signing and macOS notarization.

Supported beta artifacts:

- Linux: `.deb`
- Windows: `.msi`
- macOS: `.dmg`
- Android: APK sideload build

## What GEM Does

- Stores local account details in the platform vault route.
- Logs in to Second Life with the selected saved account.
- Reads groups where the account can send notices.
- Lets the operator write a subject and notice body.
- Lets the operator choose one existing inventory attachment.
- Sends notices through the shared core dispatch route.
- Provides shared Compose UI across desktop and Android.
- Provides in-app localization catalogues for supported UI languages.

## Layout

- `gem-core/` - reusable core boundary for login, group listing, display-name targeting, existing-inventory attachment handling, and notice orchestration.
- `gem-ui/` - shared Compose UI used by Android and desktop app shells.
- `apps/android/` - Android launcher and platform composition for the shared UI.
- `apps/desktop/` - desktop launcher and platform composition for Linux, Windows, and macOS builds.
- `gem-credential-vault/` - local encrypted credential storage route.
- `gem-preferences/` - local appearance, language, and last-login preferences.
- `gem-protocol-libomv/` - Second Life protocol adapter boundary and promoted protocol-bootstrap material.
- `tools/cli/` - local proof and operational helpers for the current implementation path.
- `tools/guards/` - boundary checks that keep architecture, localization, package identity, and UI ownership rules intact.

## Build Requirements

- JDK 21
- Android SDK with API 36 for Android builds
- Gradle wrapper from this repository
- Platform packaging tools for desktop installers:
  - Linux for `.deb`
  - Windows for `.msi`
  - macOS for `.dmg`

## Common Checks

```bash
./gradlew --no-daemon :gem-ui:check
./gradlew --no-daemon :apps:desktop:check :apps:android:check checkGemBoundaries
```

## Local Builds

Linux DEB:

```bash
./gradlew --no-daemon :apps:desktop:packageDeb
```

Android debug APK:

```bash
./gradlew --no-daemon :apps:android:assembleDebug
```

Windows MSI and macOS DMG must be built on their target operating systems through the same Gradle packaging route:

```bash
./gradlew --no-daemon :apps:desktop:packageMsi
./gradlew --no-daemon :apps:desktop:packageDmg
```

## Releases

Release binaries are distributed as GitHub Release assets, not committed to the Git repository. GitHub Releases are suitable for the four GEM artifacts because each current artifact is far below GitHub's per-asset release limit.

Download the current beta from [GEM 0.1.47 beta](https://github.com/grid-event-manager/gem/releases/tag/v0.1.47).

See `docs/PUBLISHING.md` for the current pre-publication checklist and release upload commands.

Help and support: [gem.anvll.com](https://gem.anvll.com).

## Security

Do not put real Second Life account names, passwords, session data, inventory identifiers, local machine paths, or support-channel secrets in public issues. See `SECURITY.md`.

## License

GEM is released under the Apache License 2.0. See `LICENSE`.

Third-party provenance and dependency notes live in `THIRD_PARTY_NOTICES.md` and `gem-protocol-libomv/PROMOTION-MANIFEST.md`.
