# GEM libomv Promotion Manifest

This manifest records the public protocol-bootstrap material used by GEM's Second Life protocol adapter.

## Upstream Source

- Upstream project family: libomv / OpenMetaverse-derived Java protocol material.
- Observed source revision: SourceForge SVN revision `1254`.
- Source component: `libomv-core`.

## License Summary

- The referenced libomv source carries BSD-style source and binary redistribution terms with attribution, disclaimer retention, and no-endorsement conditions.
- The upstream notice also identifies inherited BSD-style libsecondlife/libopenmetaverse/Radegast material and an Apache-2.0 BVH component.
- Promoted source material must retain required upstream copyright and license notices.

## Promoted Source

- `src/protocol-bootstrap/message_template.msg`
  - Upstream source path: `libomv-core/src/libomv/mapgenerator/message_template.msg`
  - SHA-256: `2a351a754a379765bac2cebf5284692df3f869ce662756ab29733b6330cc668d`
  - Purpose: compile-time packet skeleton generation for adapter bootstrap proof.

No full upstream login, group, notice, inventory, landmark, texture, CAPS, UDP, asset, GUI, AWT, Swing, JOGL, METAbolt, or WinForms source is promoted into this repository.

## Generated Packet Output Policy

- Gradle task: `:gem-protocol-libomv:generateLibomvPacketCatalog`.
- Input: `gem-protocol-libomv/src/protocol-bootstrap/message_template.msg`.
- Output: `gem-protocol-libomv/build/generated/sources/libomvPackets/kotlin/commonTest/libomv/packets/`.
- Output shape: `Packet.kt`, `PacketType.kt`, `PacketCatalog.kt`, and one packet skeleton class per top-level packet definition.
- Generated output count: 479 Kotlin files from 476 packet definitions.
- Generated outputs are build artifacts and are not committed.
- This is a test-only compile-boundary packet bootstrap. It is not a production source set and does not claim field-complete parity with upstream runtime code.

## Dependency Decisions

- No upstream dependency jars are promoted into production.
- Old Apache HttpComponents, XPP3, Commons Codec, Commons IO, Commons Logging, and JJ2000 jars remain unpromoted.
- Protocol runtime code in this repository is GEM-owned adapter code over promoted source facts, not copied upstream runtime source.

## Maintenance Notes

- `LibomvClientSession` is the only adapter class allowed to own raw protocol client/session state.
- Direct UI or app-shell access to protocol runtime internals is forbidden; app shells use the shared core and UI routes.
- Generated packet outputs stay uncommitted.
