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

- Gradle task: `:hostess-protocol-libomv:generateLibomvPacketCatalog`.
- Input: `hostess-protocol-libomv/src/protocol-bootstrap/message_template.msg`.
- Output: `hostess-protocol-libomv/build/generated/sources/libomvPackets/java/main/libomv/packets/`.
- Output shape: `Packet.java`, `PacketType.java`, `PacketCatalog.java`, and one packet skeleton class per top-level packet definition.
- Generated output count: 479 Java files from 476 packet definitions.
- Generated outputs are build artifacts and are not committed.
- This is a compile-boundary packet bootstrap, not a field-complete libomv runtime packet implementation.

Dependency jars:

- None added to production.
- `.classpath` dependency decision: old Apache HttpComponents, XPP3, Commons Codec, Commons IO, Commons Logging, and JJ2000 jars remain unpromoted.
- HTTP/TLS decision: runtime promotion gap until the old HttpComponents async client and `CertificateStore`/`sun.security.*` helper are replaced or isolated behind a JVM/Android-safe TLS implementation.

Local modifications:

- `message_template.msg` is copied unchanged.
- Packet skeleton generation is Hostess-owned Gradle generation from the promoted template. It does not claim field-complete parity with upstream `mapgenerator.java`.

Maintenance notes:

- Do not promote `libomv-gui`, AWT, Swing, JOGL, METAbolt, or WinForms source.
- `LibomvClientSession` is the only adapter class allowed to own raw protocol client/session state.
- Adapter methods must return typed failure while protocol bootstrap is unavailable; fake success is forbidden.
- Full protocol runtime promotion remains unavailable until login/groups/notices/inventory source can compile without obsolete TLS internals or private dependency paths.
