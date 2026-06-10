# Hostess

Hostess is a planned Second Life venue-manager aid focused on one narrow job: help a pre-provisioned account log in, choose its groups by display name, and send event notices with an existing landmark or texture attachment to selected group targets.

This repository is the public-ready production source tree inside the Hostess project container. Internal RFCs, briefs, standard operating procedures, runbooks, reference-source snapshots, and discovery archives are kept outside this repo in `../private/`.

## Layout

- `hostess-core/` - reusable core boundary for login, group listing, display-name targeting, existing-inventory attachment handling, and notice orchestration.
- `hostess-ui/` - shared Compose UI used by Android and desktop app shells.
- `apps/android/` - Android launcher and platform composition for the shared UI.
- `apps/desktop/` - Linux desktop launcher and platform composition for the shared UI.
- `tools/cli/` - local proof and operational helpers for the current implementation path.

Public production documentation can be added under `docs/` later when it is intended to ship with the project.
