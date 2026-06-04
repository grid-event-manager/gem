# Hostess Boundary Guards

This directory is the single owner for public source boundary scans.

`check-boundaries.sh` is invoked by `./gradlew checkHostessBoundaries` and by normal Gradle `check` tasks. Add or change boundary scan rules here instead of adding a second guard path elsewhere.

The guard scans production build/source paths. This README and the guard script may contain forbidden literal examples because they define the rule set; they are not production behaviour.

The script also runs self-test fixtures for each forbidden-pattern category on every invocation. Those fixture strings live only in `check-boundaries.sh`; they prove the guard still fails on forbidden examples without allowing those examples in production source.

Non-production proof allowlist for broad source scans:

- `README.md`: public repo-split documentation may mention `../private` to explain where private Hostess workbench material lives.
- `tools/guards/check-boundaries.sh`: guard owner script contains forbidden literals as rule definitions.
- `tools/guards/README.md`: guard owner documentation may mention forbidden literals and allowlist paths.
