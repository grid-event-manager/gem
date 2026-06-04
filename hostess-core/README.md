# Hostess Core

Reusable JVM boundary for the Second Life notice workflow.

Planned responsibilities:

- authenticate an account without exposing credentials in logs;
- list account groups as `{ id, displayName, canSendNotices }`;
- maintain notice target sets by group display name while preserving UUIDs internally;
- create or resolve landmark and texture attachment inventory items;
- send notices to one or more selected group IDs with pacing, per-group result reporting, and retry-safe failure handling.

This module owns the protocol adapter. Android, desktop, and CLI surfaces should depend on this boundary instead of calling reference libraries directly.
