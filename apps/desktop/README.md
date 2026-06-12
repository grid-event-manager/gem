# Gem Desktop App

Linux desktop launcher for the shared Gem Compose UI.

This module owns desktop app-shell composition only: local vault access, protocol runtime wiring, core services, and the Compose desktop window.

The production surface is the shared `org.gem.ui.GemApp`; desktop must not add a separate screen tree or duplicate login/group/inventory/notice behaviour. Desktop must not claim live UI send acceptance unless current UI/live proof docs say so.
