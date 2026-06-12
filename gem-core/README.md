# Gem Core

Reusable JVM boundary for the Second Life notice workflow.

Current responsibilities:

- authenticate an account without exposing credentials in logs;
- list account groups as `{ id, displayName, canSendNotices }`;
- maintain notice target sets by group display name while preserving UUIDs internally;
- select and resolve a pre-provisioned landmark inventory item for notice attachment;
- send notices to one or more selected group IDs with pacing, per-group result reporting, and retry-safe failure handling.

This module owns Gem domain objects, application services, and port interfaces. The protocol adapter is separate: `gem-protocol-libomv/` is the only production module intended to wrap promoted libomv-derived protocol code.

Android, desktop, and CLI surfaces should depend on this boundary instead of calling reference libraries directly.
