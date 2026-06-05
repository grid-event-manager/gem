# Hostess Boundary Guards

This directory is the single owner for public source boundary scans.

`check-boundaries.sh` is invoked by `./gradlew checkHostessBoundaries` and by normal Gradle `check` tasks. Add or change boundary scan rules here instead of adding a second guard path elsewhere.

The guard scans production build/source paths. This README and the guard script may contain forbidden literal examples because they define the rule set; they are not production behaviour.

The script also runs self-test fixtures for each forbidden-pattern category on every invocation. Those fixture strings live only in `check-boundaries.sh`; they prove the guard still fails on forbidden examples without allowing those examples in production source.

Track B owner rules:

- Raw OkHttp package/client symbols are allowed only in `hostess-protocol-libomv/src/main/kotlin/org/hostess/protocol/libomv/transport`.
- Core, CLI, desktop, and Android production source must not call protocol runtime classes or the protocol HTTP transport contract directly.
- Runtime and transport composition belongs inside `:hostess-protocol-libomv`; app and proof shells must use the existing core service/port/adapter path.
- Legacy HTTP/TLS, private-reference, GUI, METAbolt, and WinForms names remain forbidden in production source.

Track C owner rules:

- Current-groups runtime and transport classes remain inside `:hostess-protocol-libomv`; core, CLI, desktop, and Android source must not call them directly.
- Raw environment reads are allowed only in the protocol env resolver.
- The CLI may mention `--credential-file` only at the blocking parser point that rejects the unsupported route.
- Keychain/plaintext secret-store routes remain forbidden in production source.

Track DS owner rules:

- Login package, hash, machine identity, and XML-RPC serialization owners remain inside `:hostess-protocol-libomv`.
- Android may mention Track DS owner class names only in `AndroidCompatibilityProbe.kt` for no-UI class-load proof.
- The old inline LLSD login body, stale Track DS login fields, and spoofed viewer names remain forbidden in production source.

Non-production proof allowlist for broad source scans:

- `README.md`: public repo-split documentation may mention `../private` to explain where private Hostess workbench material lives.
- `tools/guards/check-boundaries.sh`: guard owner script contains forbidden literals as rule definitions.
- `tools/guards/README.md`: guard owner documentation may mention forbidden literals and allowlist paths.
