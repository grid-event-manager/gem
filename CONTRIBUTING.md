# Contributing

GEM is in alpha. Contributions are welcome after the public repository opens, but the project is intentionally strict about architecture, UI ownership, localization, and protocol boundaries.

## Ground Rules

- Do not commit credentials, account names, live-grid identifiers, private logs, local paths, or generated release artifacts.
- Keep UI text in the localization catalogue route.
- Keep shared UI in `gem-ui`; do not fork separate desktop and Android screen trees.
- Keep app shells thin; they wire platform services into the shared UI.
- Keep protocol work behind `gem-protocol-libomv`; app and UI code must not call protocol internals.
- Keep generated build outputs out of Git.

## Checks

Run the focused public checks before opening a pull request:

```bash
./gradlew --no-daemon :gem-ui:check
./gradlew --no-daemon :apps:desktop:check :apps:android:check checkGemBoundaries
```

Run the full check when changing shared architecture, localization, protocol, package identity, or guards:

```bash
./gradlew --no-daemon check
```

## Pull Requests

Pull requests should include:

- a short problem statement;
- the user-visible behaviour change;
- the tests or checks run;
- screenshots for visible UI changes;
- a note when platform-specific packaging or signing was not tested.

## Public Issues

Bug reports should not include sensitive data. If a report requires secrets or private live-grid evidence, follow `SECURITY.md`.
