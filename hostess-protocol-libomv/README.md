# Hostess libomv Protocol Adapter

Production adapter boundary for the libomv-java protocol candidate.

This module is the only public production module allowed to wrap promoted libomv-derived protocol code. It must map raw protocol types into Hostess core domain values before returning to app, CLI, or core callers.

It depends on `hostess-core/`. `hostess-core/` must not depend on this module.
