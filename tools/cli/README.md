# Gem CLI Tools

Local proof and operator helpers for the current Gem implementation path.

Available proof commands:

- `login`
- `list-groups`
- `send-notice`
- `live-proof`

Examples:

```bash
./gradlew :tools:cli:run --args="list-groups --mode fake --report build/reports/gem/fake-list-groups.json"
./gradlew :tools:cli:run --args="send-notice --mode fake --target 'Venue Hosts' --subject 'Tonight' --body 'Doors at eight'"
./gradlew :tools:cli:run --args="live-proof --mode live --proof-scope full --report build/reports/gem/live-full-cycle.json --account venue-proof --credential-env GEM_PROOF_CREDENTIAL --existing-attachment-name 'Venue Landmark'"
```

Live mode uses credential handles that name environment variables. Do not place raw credentials in command arguments.
Live notice mutation is gated to `live-proof --proof-scope full` with explicit authorisation; `send-notice` is fake-mode only.
