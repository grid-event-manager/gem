# Gem Boundary Guards

This directory is the single owner for public source boundary scans.

`check-boundaries.sh` is invoked by `./gradlew checkGemBoundaries` and by normal Gradle `check` tasks. Add or change boundary scan rules here instead of adding a second guard path elsewhere.

The guard scans production build/source paths. This README and the guard script may contain forbidden literal examples because they define the rule set; they are not production behaviour.

The script also runs self-test fixtures for each forbidden-pattern category on every invocation. Those fixture strings live only in `check-boundaries.sh`; they prove the guard still fails on forbidden examples without allowing those examples in production source.

Vault owner rules:

- Vault/keyring/serializer dependencies such as java-keyring, Secret Service wrappers, AndroidX Security Crypto, Tink, Bouncy Castle, kotlinx serialization, protobuf, CBOR, and KMP vault wrappers are forbidden in build files/version catalogues.
- Saved-login storage must not grow a passphrase, TOTP/authenticator, VM fallback, plaintext fallback, Android Credential Manager, generic password helper, or duplicate vault manager path.
- `AndroidKeystoreVaultKeySource`, `LocalUserFileVaultKeySource`, `DesktopVaultPaths`, and `CredentialVaultLoginSecretResolver` have one production owner each.
- Raw key bytes and `SecretKey` must not appear outside `:gem-credential-vault`; app shells consume `GemCredentialRuntimeState`.
- App saved-login composition must not use `EnvironmentLoginSecretResolver`; that route remains proof/CLI only.

Protocol and shared UI boundary rules:

- Raw OkHttp package/client symbols are allowed only in `gem-protocol-libomv/src/main/kotlin/org/gem/protocol/libomv/transport`.
- Core, CLI, desktop, and Android production source must not call protocol runtime classes or the protocol HTTP transport contract directly.
- Runtime and transport composition belongs inside `:gem-protocol-libomv`; app and proof shells must use the existing core service/port/adapter path.
- Legacy HTTP/TLS, private-reference, GUI, METAbolt, and WinForms names remain forbidden in production source.
- `:gem-ui` must exist and remains shared Compose UI over `gem-core`; it must not import protocol runtime/transport, vault/native credential, `NoticePort`, `sendGroupNotice`, or UI-side selected-group send loops.
- Shared UI visible labels live in `GemText.kt`; style constants live under `org/gem/ui/design`; production browser-backed prototype routes and staged placeholder code are forbidden.
- Android and desktop app shells may compose services, but must not add platform-specific screen trees or duplicate product behaviour.
- `JvmPlatformFontCatalogue.kt` is the only approved `java.awt.GraphicsEnvironment` caller; it owns desktop system-font enumeration behind the shared `PlatformFontCatalogue` interface.
- Appearance customiser state, fonts, RGB/hex feedback, selector-open actions, and visible palette surfaces stay on the shared controller/design-token/component routes; prototype imports, local screen fonts, local colour state, and duplicate settings titles are forbidden.

Localization rules:

- Shipped in-app locale source is exactly the approved 23-file set in `gem-ui/src/commonMain/localization`; extra or missing `.properties` files fail the guard.
- Production source must not depend on private translation evidence, Android `strings.xml`, package-visible text diffs, public workstream labels, handwritten catalogues, or checked-in generated non-English catalogue objects.
- Generated catalogue objects belong only under generated build output; production source consumes them through `GemTextCatalogueRegistry`.
- Visible UI-copy literals outside locale source require an explicit row in `localization-visible-literal-allowlist.txt`.

Credential and session owner rules:

- Current-groups runtime and transport classes remain inside `:gem-protocol-libomv`; core, CLI, desktop, and Android source must not call them directly.
- Raw environment reads are allowed only in the protocol env resolver.
- The CLI may mention `--credential-file` only at the blocking parser point that rejects the unsupported route.
- Keychain/plaintext secret-store routes remain forbidden in production source.
- UI remediation must keep one shared `GemAppScaffold`, must not resurrect the old split login panels, must not project fake session locations such as `London City` or `Welcome Area`, and must not return to custom-drawn menu/back icons.
- Retained `startLocation` data fields may still exist for login material/profile storage; they must not feed the current session strip.

Login package rules:

- Login package, hash, machine identity, and XML-RPC serialization owners remain inside `:gem-protocol-libomv`.
- Android may mention login package owner class names only in `AndroidCompatibilityProbe.kt` for no-UI class-load proof.
- The old inline LLSD login body, stale login package fields, and spoofed viewer names remain forbidden in production source.

Notice protocol rules:

- `ProtocolSimulatorCircuitClient` is the single simulator circuit owner; bounded circuit owner names remain forbidden.
- Full live proof uses one `group-notice` workflow. Old plain, existing-attachment-notice, and bulk proof routes and inputs remain forbidden.
- `InventorySelectionService`, `LibomvInventoryPermissionMapping`, `LibomvNoticePacketCodec`, and `ProtocolNoticeCircuitSource` each have one production owner.
- CLI and app shells must not call notice protocol/runtime owners directly. Android may mention notice protocol owner class names only in `AndroidCompatibilityProbe.kt` for no-UI class-load proof.
- `GroupNoticeAdd`, `NoticeSender`, and `BulkSender` remain forbidden production notice-send routes.

Notice totals rules:

- Local notice totals are non-authoritative and must not exist in production source. `NoticeComplianceService`, `NoticeSubmissionLedgerPort`, local notice ledger/cap types, `noticeSubmissionProjectionStatus` and `noticeLedgerConfigured`, old recipient-estimate/delivery types, old recipient-delivery report fields, and stale notice count environment variables remain forbidden in production source.
- Stale `--ledger`, `--recipient-count`, and `--recipient-count-source` strings are allowed only at command boundaries that reject those options before send; they must not feed a retained notice totals model.
- The notice totals guard scans `gem-core` common/JVM production source, CLI/app production shells, and public readmes for local notice totals classes, report fields, and stale owner language.
- Historical mentions are allowed only outside public production source and public readmes, such as private RFC/brief/archive/reference/evidence material, or inside this guard owner script and README.

Legacy identity classification:

- Current public source, README, Gradle, package, Android, thread, and test-tag identity must use Gem names.
- Legacy Hostess strings are allowed only inside desktop storage migration owners/tests, exact single-instance cleanup markers/tests, exact package lifecycle cleanup markers, this guard owner, and retained promotion/provenance notes.
- Guard checks must fail if an old package root, module name, boundary task, UI tag, simulator thread, or current `.deb` route appears.

Non-production proof allowlist for broad source scans:

- `README.md`: public repo-split documentation may mention `../private` to explain where private Gem workbench material lives.
- `tools/guards/check-boundaries.sh`: guard owner script contains forbidden literals as rule definitions.
- `tools/guards/README.md`: guard owner documentation may mention forbidden literals and allowlist paths.
