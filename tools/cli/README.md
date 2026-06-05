# Hostess CLI Tools

Local proof and operator helpers.

Available proof commands:

- `login`
- `list-groups`
- `send-notice`
- `live-proof`

Examples:

```bash
./gradlew :tools:cli:run --args="list-groups --mode fake --report ../private/evidence/HS001-A-09/fake-list-groups.json"
./gradlew :tools:cli:run --args="send-notice --mode fake --target 'Venue Hosts' --subject 'Tonight' --body 'Doors at eight'"
./gradlew :tools:cli:run --args="live-proof --mode live --account venue-proof --credential-env HOSTESS_PROOF_CREDENTIAL"
```

Live mode uses credential handles that name environment variables. Do not place raw credentials in command arguments.
