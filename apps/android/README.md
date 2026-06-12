# Hostess Android App

Android launcher for the shared Gem Compose UI.

This module owns Android app-shell composition only: Android vault access, protocol runtime wiring, core services, and `HostessAndroidActivity`.

The production surface is the shared `org.gem.ui.GemApp`; Android must not add a separate screen tree or duplicate login/group/inventory/notice behaviour. The compatibility probe remains a no-live class-load probe and does not prove live UI send acceptance.
