# Third-Party Notices

This file is a public provenance note for the GEM source tree. Gradle metadata remains the authoritative dependency graph for exact versions used by a build.

## Runtime and Build Dependencies

GEM uses Kotlin Multiplatform, Compose Multiplatform, Android Gradle Plugin, AndroidX, kotlinx.coroutines, and OkHttp through Gradle dependencies declared in `gradle/libs.versions.toml`.

The main dependency families used by the project are:

- Kotlin and Kotlin Gradle plugins - JetBrains / Kotlin project.
- Compose Multiplatform and Material icons - JetBrains Compose / AndroidX-derived UI libraries.
- Android Gradle Plugin and AndroidX libraries - Android Open Source Project / Google.
- kotlinx.coroutines - Kotlin project.
- OkHttp - Square.

These dependencies are not vendored into this repository.

## libomv / OpenMetaverse-Derived Protocol Material

GEM includes one promoted protocol-bootstrap source file:

- `gem-protocol-libomv/src/protocol-bootstrap/message_template.msg`

The promotion manifest records source provenance, checksum, generated-output policy, and what is not promoted:

- `gem-protocol-libomv/PROMOTION-MANIFEST.md`

The promoted material is used to generate test-only packet skeletons at build time. Generated packet skeletons are not committed.

## Second Life

GEM interoperates with Second Life grid services but is not affiliated with, sponsored by, or endorsed by Linden Lab.
