# Hostess

Hostess is a planned Second Life venue-manager aid focused on one narrow job: help an account log in, choose its groups by display name, and send event notices with landmark or texture attachments to selected group targets.

This repository is the public-ready production source tree inside the Hostess project container. Internal RFCs, briefs, standard operating procedures, runbooks, reference-source snapshots, and discovery archives are kept outside this repo in `../private/`.

## Layout

- `hostess-core/` - reusable JVM core boundary for login, group listing, notice composition, attachment handling, and send orchestration.
- `apps/android/` - future Android client shell.
- `apps/desktop/` - future Linux desktop client shell.
- `tools/cli/` - future local proof and operational helpers.

Public production documentation can be added under `docs/` later when it is intended to ship with the project.
