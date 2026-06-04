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

- None yet. `HS001-A-06` creates adapter seams and mapping fixtures only.

Generated packet output policy:

- Not generated or vendored yet.
- `HS001-A-07` must either reproduce generation from `libomv-core/src/libomv/mapgenerator/message_template.msg` or vendor generated outputs under a named generated-source directory with command/source-hash evidence.

Dependency jars:

- None added yet.
- `HS001-A-07` must classify the old HttpComponents and other libomv-core classpath dependencies before promotion.

Local modifications:

- None to promoted upstream source yet.

Maintenance notes:

- Do not promote `libomv-gui`, AWT, Swing, JOGL, METAbolt, or WinForms source.
- `LibomvClientSession` is the only adapter class allowed to own raw protocol client/session state.
- Adapter methods must return typed failure while protocol bootstrap is unavailable; fake success is forbidden.
