# Hostess libomv Promotion Manifest

Source snapshot:

- Private reference path: `/media/jimx/P-AI/Hostess/private/reference/libomv-java/sourceforge-svn-r1254/`
- Observed revision: `1254`
- Candidate module: `libomv-core`

License summary:

- `libomv-core/LICENSE.txt` is BSD-style source/binary redistribution with attribution, disclaimer retention, and no-endorsement conditions.
- The upstream notice also names inherited BSD-style libsecondlife/libopenmetaverse/Radegast material and an Apache 2.0 BVH component.
- Any promoted source must retain required copyright/license notices.

Promoted file list:

- `src/protocol-bootstrap/message_template.msg`
  - Source: `libomv-core/src/libomv/mapgenerator/message_template.msg`
  - SHA-256: `2a351a754a379765bac2cebf5284692df3f869ce662756ab29733b6330cc668d`
  - Purpose: compile-time packet skeleton generation for adapter bootstrap proof.
- No full upstream login, group, notice, inventory, landmark, texture, CAPS, UDP, or asset source is promoted yet.

Generated packet output policy:

- Gradle task: `:gem-protocol-libomv:generateLibomvPacketCatalog`.
- Input: `hostess-protocol-libomv/src/protocol-bootstrap/message_template.msg`.
- Output: `hostess-protocol-libomv/build/generated/sources/libomvPackets/kotlin/commonTest/libomv/packets/`.
- Output shape: `Packet.kt`, `PacketType.kt`, `PacketCatalog.kt`, and one packet skeleton class per top-level packet definition.
- Generated output count: 479 Kotlin files from 476 packet definitions.
- Generated outputs are build artifacts and are not committed.
- This is a test-only compile-boundary packet bootstrap, not a production source set and not a field-complete libomv runtime packet implementation.

Dependency jars:

- None added to production.
- `.classpath` dependency decision: old Apache HttpComponents, XPP3, Commons Codec, Commons IO, Commons Logging, and JJ2000 jars remain unpromoted.
- HTTP/TLS decision: runtime promotion gap until the old HttpComponents async client and `CertificateStore`/`sun.security.*` helper are replaced or isolated behind a JVM/Android-safe TLS implementation.

Local modifications:

- `message_template.msg` is copied unchanged.
- Packet skeleton generation is Hostess-owned Gradle generation from the promoted template. It does not claim field-complete parity with upstream `mapgenerator.java`.
- HS001-B-01 is evidence-only. It classifies the required Track B protocol surface from private reference source; no production source or generated output was promoted.
- HS001-B-05 reimplements login request and response mapping from retained `LoginManager.java:1032-1155` evidence behind Hostess-owned transport. No upstream login source was copied into production.
- HS001-B-06 reuses retained `GroupManager.java` and `CapsMessage.java` evidence for current-groups field mapping. The live packet/CAPS source remains fail-closed until field-complete runtime promotion lands; no upstream group source was copied into production.
- HS001-B-08 reimplemented source-derived attachment reference mapping and landmark asset byte encoding from retained inventory/asset evidence. Track E later removed the active landmark creation and texture upload route, preserving only existing-inventory attachment mapping; no upstream inventory or asset source was copied into production.
- HS001-B-09 reimplements source-derived group notice request mapping, dialog/online constants, XOR instant-message ID derivation, and attachment OSD/XML bucket encoding from retained `GroupManager.java:389-412`, `GroupManager.java:1624-1628`, and `AgentManager.java:2863-2912` evidence. The live UDP packet sender remains fail-closed until field-complete packet transport promotion lands; no upstream notice source was copied into production.
- HS001-C-01 is evidence-only. It proves the Track C login, simulator circuit, EventQueueGet, current-groups event, mapping, cleanup, and Android no-UI source anchors in private evidence `evidence/HS001-C-01/live-read-source-proof.md`. No retained source or generated runtime code was copied into production.
- HS001-C-04 adds a Hostess-owned bounded low-frequency packet codec for `UseCircuitCode`, `CompleteAgentMovement`, and `AgentDataUpdateRequest`, derived from retained `message_template.msg`, `mapgenerator.java`, `template.java.txt`, `PacketHeader.java`, `PacketFrequency.java`, `Simulator.java`, and `Helpers.java` evidence. No generator expansion was required, generated packet skeletons remain uncommitted build artifacts, and no upstream UDP/runtime source was copied into production.

Maintenance notes:

- Do not promote `libomv-gui`, AWT, Swing, JOGL, METAbolt, or WinForms source.
- `LibomvClientSession` is the only adapter class allowed to own raw protocol client/session state.
- Adapter methods must return typed failure while protocol bootstrap is unavailable; fake success is forbidden.
- Full protocol runtime promotion remains unavailable until login/groups/notices/inventory source can compile without obsolete TLS internals or private dependency paths.
