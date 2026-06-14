#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

failures=0

RAW_LIBOMV_PATTERN='(^|[^[:alnum:]_.])libomv\.'
CORE_FORBIDDEN_PATTERN="gem-protocol-libomv|org\.gem\.protocol\.libomv|org\.gem\.protocol\.libomv|:apps:|:tools:cli|reference/|\.\./private|$RAW_LIBOMV_PATTERN"
PRIVATE_REFERENCE_PATTERN='reference/|\.\./private'
FORBIDDEN_PLATFORM_PATTERN='TrustAll|ALLOW_ALL|HostnameVerifier|X509TrustManager|sslSocketFactory|org\.apache\.http|sun\.security|java\.awt|javax\.swing|METAbolt|WinForms|printStackTrace\(|println\('
CLI_COMMAND_REPORT_WRITE_PATTERN='File\(|Path\.of\(|Files\.|writeText\(|appendText\('
OK_HTTP_CLIENT_SYMBOL='OkHttp'"Client"
PROTOCOL_PREFIX='Protocol'
RUNTIME_SUFFIX='Runtime'
PROTOCOL_BOUNDARY_OKHTTP_PATTERN="(^|[^[:alnum:]_.])okhttp3\.|(^|[^[:alnum:]_.])${OK_HTTP_CLIENT_SYMBOL}([^[:alnum:]_]|$)"
PROTOCOL_BOUNDARY_RUNTIME_PATTERN="(^|[^[:alnum:]_.])(${PROTOCOL_PREFIX}Login${RUNTIME_SUFFIX}|${PROTOCOL_PREFIX}Group${RUNTIME_SUFFIX}|${PROTOCOL_PREFIX}Inventory${RUNTIME_SUFFIX}|${PROTOCOL_PREFIX}Notice${RUNTIME_SUFFIX}|${PROTOCOL_PREFIX}HttpClient)([^[:alnum:]_]|$)"
UI_DIRECT_PROTOCOL_PATTERN='Libomv|Protocol[A-Za-z]+Runtime|ProtocolHttpClient|EventQueueGetClient|SimulatorPacketExchange'
UI_DIRECT_VAULT_PATTERN='CredentialVault|VaultAccessService|AndroidKeystore|LocalUserFileVaultKeySource|CredentialManager|Keychain|SecretService|passphrase|TOTP|authenticator'
UI_DIRECT_NOTICE_PATTERN='sendGroupNotice|NoticePort|selectedGroups.*(forEach|map|for \()'
UI_FORBIDDEN_OWNER_PATTERN='(^|[^[:alnum:]_])(UiManager|StyleUtils|CommonUi|ScreenHelpers|CredentialHelper|VaultManager|InventoryBrowserService|NoticeSender|BulkSender)([^[:alnum:]_]|$)'
UI_WEBVIEW_PATTERN='WebView|android\.webkit|<html|styles\.css|prototype\.js'
UI_STAGED_PATTERN='STAGED_ENTRYPOINT|TODO|NotImplemented|UnsupportedOperationException|error\("not implemented"\)'
UI_TEXT_CATALOGUE_PATTERN='"(User Name|Password|Show|Hide|Login|Add new login|Add new login\.\.\.|Save and login|Settings|Add new account|Save new account|Delete account|Delete|Send notices|Online|Offline|Inventory|Landmarks|Textures|Add all|Select\.\.\.)"'
UI_STYLE_TOKEN_PATTERN='#[0-9A-Fa-f]{6}|Color\(|[0-9]+\.dp'
UI_DIRECT_CONTROL_PATTERN='(^|[^[:alnum:]_])(OutlinedTextField|DropdownMenu|DropdownMenuItem|ExposedDropdownMenu|ExposedDropdownMenuBox|Button|OutlinedButton|verticalScroll|rememberScrollState|VerticalScrollbar|rememberScrollbarAdapter)[[:space:]]*\('
SESSION_PROTOCOL_DIRECT_RUNTIME_PATTERN='EnvironmentLoginSecretResolver|ProtocolSimulatorCircuitClient|AgentDataUpdateRequestTransport|EventQueueGetClient'
VAULT_FORBIDDEN_DEPENDENCY_PATTERN='java-keyring|secret-service|multiplatform-settings|KVault|SecureVault|Kassaforte|tink|BouncyCastle|kotlinx\.serialization|protobuf|cbor|EncryptedSharedPreferences|MasterKey|EncryptedFile'
VAULT_STALE_CREDENTIAL_ROUTE_PATTERN='VaultUnlock|passphrase|TOTP|authenticator|platform_deferred|native-store required|VM fallback|plaintext fallback|CredentialManager|PasswordHelper|LoginUtils|VaultManager|VaultHelper|VaultUtils|CommonVault'
VAULT_ANDROIDX_CRYPTO_PATTERN='EncryptedSharedPreferences|EncryptedFile|MasterKey|androidx\.security'
VAULT_RAW_KEY_EXPOSURE_PATTERN='ByteArray.*key|key.*ByteArray|SecretKey'
VAULT_APP_ENV_RESOLVER_PATTERN='EnvironmentLoginSecretResolver'
SIMULATOR_CIRCUIT_STALE_OWNER_PATTERN='BoundedSimulatorCircuit(Client|Sender)'
LIVE_NOTICE_PROOF_STALE_CLI_SEND_PATTERN='sessionProvider|fakeSession\(active = false\)|send-notice --mode live'
LIVE_NOTICE_PROOF_STALE_FULL_PROOF_PATTERN='sendPlainNotice|runAttachmentProof|runBulkProof|boundedBulkTargetSet|plain-notice|existing-attachment-notice|bulk-notice|existingAttachmentId|--existing-attachment-id|bulkLimit|bulkDelayMs'
NOTICE_PROTOCOL_STALE_OWNER_PATTERN='(^|[^[:alnum:]_])(GroupNoticeAdd|NoticeSender|BulkSender)([^[:alnum:]_]|$)'
NOTICE_PROTOCOL_DIRECT_APP_CALL_PATTERN='(^|[^[:alnum:]_])(ProtocolNoticeRuntime|ProtocolNoticeCircuitSource|LibomvNoticePacketCodec|SimulatorPacketExchange)([^[:alnum:]_]|$)'
SIMULATOR_EXCHANGE_DUPLICATE_PATTERN='SimulatorPacketReceiver|UdpSimulatorDatagramReceiver|class[[:space:]]+.*Presence.*Socket|class[[:space:]]+.*Simulator.*Socket|DatagramSocket\('
NOTICE_ARCHIVE_DIRECT_PATTERN='(^|[^[:alnum:]_])(ProtocolGroupNoticeArchiveSource|requestGroupNoticeArchive|GroupNoticesListRequest|GroupNoticesListReply)([^[:alnum:]_]|$)'
FAKE_LIVE_PROOF_PASS_PATTERN='fake mode.*passed|CommandMode\.FAKE[^;]*ProofReportStatus\.PASSED|ProofReportStatus\.PASSED[^;]*CommandMode\.FAKE'
FULL_PROOF_ARCHIVE_REQUIRED_PATTERN='if[[:space:]]*\(!noticeArchive\(session,[[:space:]]*targetSet\)\)'
GROUP_MEMBERSHIP_MUTATION_PATTERN='EjectGroupMemberRequest|LeaveGroupRequest|GroupRoleChanges|GroupRoleUpdate|InviteGroupRequest|RequestBanAction|SetGroupAcceptNotices'
ANDROID_PROBE_FORBIDDEN_RUNTIME_PATTERN='EnvironmentLoginSecretResolver|AgentDataUpdateRequestTransport|EventQueueGetClient|ProtocolLoginRuntime|ProtocolGroupRuntime|ProtocolInventoryRuntime|ProtocolNoticeRuntime|SimulatorPacketExchange'
NOTICE_TOTALS_STALE_COMPLIANCE_PATTERN='NoticeCompliance(Service|Request|Decision|Receipt|Policy|Clock|LedgerResult|Ledgers?)|DefaultNoticeComplianceClock|NoticeSubmission(Count|Projection|LedgerSnapshot|LedgerPort)|NoticeLedgerDay|noticeSubmissionProjectionStatus|noticeSubmissionsProjected|noticeSubmissionLedgerGroupCount|noticeSubmissionLedgerMaxGroupTotal|noticeSubmissionPerGroupHardCap|noticeLedgerConfigured|notice_submission_cap_exceeded|ledger_snapshot_unavailable|ledger_reserve_failed|ledger_record_failed|notice_ledger_unavailable|NoticeRecipientEstimate|NoticeRecipientCount|NoticeRecipientEstimateSource|recipientDeliveryProjected|recipientDeliveryLedgerTotal|recipientDeliveryHardCap|recipientProjectionStatus|recipient_count_|recipient_delivery_|NoticeDeliveryCount|NoticeDeliveryDay|NoticeDeliveryProjection|NoticeDeliveryLedgerSnapshot|NoticeComplianceLedgerPort|HOSTESS_OWKS_COUNT|HOSTESS_MINX_COUNT'
AVATAR_CLASSIC_BAKING_PATTERN='AgentSetAppearance|UploadBakedTexture|CreateBakes|DownloadWearables|WearOutfit|RequestSetAppearance'
AVATAR_FORBIDDEN_OWNER_PATTERN='(^|[^[:alnum:]_])(AvatarManager|AppearanceHelper|ViewerUtils)([^[:alnum:]_]|$)'
AVATAR_DIRECT_CAPABILITY_SEED_PATTERN='seedCapability'
AVATAR_CLI_DIRECT_PROTOCOL_PATTERN='(^|[^[:alnum:]_])(LibomvAvatarAdapter|ProtocolAvatarRuntime|ProtocolAvatarAppearanceSource)([^[:alnum:]_]|$)'
AVATAR_STALE_FULL_PROOF_SIMULATOR_GATE_PATTERN='simulatorPresence\(session\)'
AVATAR_EXTRA_SIMULATOR_EXCHANGE_IMPL_PATTERN='\)[[:space:]]*:[[:space:]]*SimulatorPacketExchange|^[[:space:]]*(class|object)[^=]*:[[:space:]]*SimulatorPacketExchange'
SIMULATOR_SESSION_FORBIDDEN_FACADE_RECEIVE_PATTERN='(^|[^[:alnum:]_])(packetExchange|currentExchange|exchange|simulatorPacketExchange)\.receive\('
SIMULATOR_SESSION_DEAD_CIRCUIT_CLIENT_PATTERN='presentCircuit|pendingNoticeArchiveReplies|drainPreNoticeTraffic|waitForOutgoingAck|waitForPacket'
SIMULATOR_SESSION_DUPLICATE_GATEWAY_PATTERN='(^|[^[:alnum:]_])(NoticeSender2|ArchiveReader2|SimulatorSessionManager|SessionHelper|UdpHelper|GroupNoticeProofHelper)([^[:alnum:]_]|$)'
SIMULATOR_SESSION_FORBIDDEN_FAKE_LIVE_PATTERN='fake.*simulator.*passed|simulator.*fake.*passed|fake.*live.*passed|CommandMode\.FAKE[^;]*ProofReportStatus\.PASSED|ProofReportStatus\.PASSED[^;]*CommandMode\.FAKE|local proof bypass'
PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN='(^|[^[:alnum:]_])(TRACK_[A-Z][A-Z0-9_]*|HS[0-9]{3}_TRACK_[A-Z][A-Z0-9_]*|HS[0-9]{3}-[A-Z][A-Z]?(-[0-9][0-9])?|Track[[:space:]]+[A-Z][A-Z]?|Track([A-Z]|[A-Z][A-Z])|track[A-Z][A-Za-z0-9_]*|track_[a-z][a-z0-9_]*|track-[a-z][a-z0-9_-]*|[A-Z]-[0-9][0-9]-T[0-9]+)([^[:alnum:]_]|$)'
CREDENTIAL_ENV_READ_PATTERN='System(::|\.)getenv'
CREDENTIAL_FILE_ROUTE_PATTERN='credential-file'
UNSUPPORTED_SECRET_STORE_PATTERN='keychain|Keychain|KeyStore|plaintext|plain-text|plain text'
UI_REMEDIATION_SPLIT_LOGIN_PATTERN='LoginSavedAccountPanel|AddLoginPanel|addLoginExpanded|newUsernameDraft|newPasswordDraft|saveAndLoginEnabled'
UI_REMEDIATION_FAKE_LOCATION_PATTERN='startLocation\.orEmpty|SavedAccountProfile\.startLocation|London City|Welcome Area|locationLabel = "[^"]+"'
UI_REMEDIATION_SCAFFOLD_REQUIRED_PATTERN='GemAppScaffold'
UI_REMEDIATION_CUSTOM_ICON_PATTERN='Canvas|drawLine|MenuBarCount|BackIconMidpoint|foundation\.Canvas'
THEME_UI_PREFERENCE_ADAPTER_PATTERN='org\.gem\.preferences|:gem-preferences'
THEME_VAULT_STORAGE_PATTERN='ThemePreference|themePreference|ui\.properties|preferences'
THEME_HACCU_COLOUR_PATTERN='#[0-9A-Fa-f]{6}|Color\(0x'
THEME_VISIBLE_LABEL_PATTERN='"(Grid Event Manager|GEM|GRID EVENT MANAGER|Light|Dark|Theme|Theme preference unavailable|Theme preference could not be saved)"'
THEME_PROTOTYPE_RUNTIME_PATTERN='WebView|android\.webkit|index-multi|<html|styles\.css|loadDataWithBaseURL'
THEME_LOGO_OWNER_PATTERN='fun[[:space:]]+GemBrandLogoIcon'
THEME_TOGGLE_OWNER_PATTERN='fun[[:space:]]+ThemeModeToggle'
THEME_PALETTE_OWNER_PATTERN='object[[:space:]]+HaccuGemPaletteProvider'
THEME_ANDROID_LABEL_PATTERN='manifestPlaceholders\["appLabel"\][[:space:]]*=[[:space:]]*"Grid Event Manager"'
THEME_DESKTOP_VENDOR_PATTERN='vendor[[:space:]]*=[[:space:]]*"ANVLL"'
THEME_ROOT_PROJECT_LABEL_PATTERN='rootProject\.name\.replaceFirstChar'
ARCHITECTURE_GENERIC_OWNER_PATTERN='(^|/)(LoginCompliance|NoticeCompliance|.*(Manager|Helper|Utils|Common))\.kt$'
SESSION_LOGIN_OVERLOAD_PATTERN='fun[[:space:]]+login\([[:space:]]*request:[[:space:]]*LoginRequest[[:space:]]*\)'
SESSION_LOGIN_ONE_ARG_PATTERN='sessionService\.login\([^,\n)]*\)'
NOTICE_DISPATCH_OLD_CALL_PATTERN='dispatch\([^\n]*session[^\n]*draft[^,\n]*\)'
NOTICE_DISPATCH_DIRECT_GROUP_SEND_PATTERN='\.[[:space:]]*sendGroupNotice\('
VIEWER_IDENTITY_PROVIDER_PATTERN='GemViewerIdentityProvider'
VIEWER_IDENTITY_SPOOFED_CHANNEL_PATTERN='channel[[:space:]]*=[[:space:]]*"(METAbolt|Firestorm|Alchemy|Second Life Viewer|Linden)"'
NOTICE_TIME_SOURCE_PATTERN='Instant\.now|LocalDate\.now|Clock\.system|System\.currentTimeMillis'
REPORT_SECRET_KEY_PATTERN='("(mac|id0|host_id|seedCapability|credentialHandle|ledgerPath)"[[:space:]]+to|put\("(mac|id0|host_id|seedCapability|credentialHandle|ledgerPath)")'
GROUP_TARGET_UUID_PATTERN='--group-id|--group-uuid|group uuid|uuid target'
NOTICE_CAP_LITERAL_PATTERN='(^|[^[:alnum:]_])(4500|4_500|5000|5_000)([^[:alnum:]_]|$)'
LOGIN_PACKAGE_OLD_LLSD_PATTERN='application/llsd\+xml|fun[[:space:]]+loginBody\('
LOGIN_PACKAGE_DIRECT_OWNER_PATTERN='LoginPackage|SecondLifePasswordHash|GemMachineIdentity|LoginPackageSerializer'
LOGIN_PACKAGE_STALE_FIELD_PATTERN='platform_version|platform_string|host_id|token|extended_errors|max-agent-groups|viewer_digest|user_agent'
ATTACHMENT_STALE_CREATE_UPLOAD_PATTERN='CreateLandmarkAttachment|UploadTextureAttachment|AttachmentPayloadHandle|LocalPosition|createLandmarkAttachment|uploadTextureAttachment|landmarkAssetBytes|AttachmentPayloadSource|InventoryUploadResult|beginTextureUpload|completeTextureUpload|landmarkRequest|textureRequest|createdLandmarkRequest|uploadTextureRequest|landmarkVenue|landmarkRegionId|landmarkLocalPosition|texturePayloadHandle|textureDigest|textureFileName|safeTextureFileName|BinaryUploadBody|application/octet-stream|upload_url|uploadUrl|attachmentPayloadHandle|attachmentSource|payloadHandle|AssetUploadRequest|AssetUploadComplete|CreateLandmarkForEvent'
ATTACHMENT_FORBIDDEN_OWNER_FILE_PATTERN='(LandmarkAttachmentService|TextureUploadService|AttachmentUploadManager|InventoryUtils|AttachmentHelpers|CommonAttachment|BulkSender)\.kt$'
ATTACHMENT_FORBIDDEN_OWNER_DECL_PATTERN='(^|[^[:alnum:]_])((data[[:space:]]+)?class|object|interface)[[:space:]]+(LandmarkAttachmentService|TextureUploadService|AttachmentUploadManager|InventoryUtils|AttachmentHelpers|CommonAttachment|BulkSender)([^[:alnum:]_]|$)'
LIVE_PROOF_IMPLICIT_FAKE_PATTERN='null,[[:space:]]*"fake"|option\("mode"\)[[:space:]]*\?:[[:space:]]*"fake"|CommandMode\.parse\(null\)'
LIVE_PROOF_LOCAL_HTTP_PATTERN='local test servers|isLocalTestServer|127\.0\.0\.1|localhost|::1'
ANDROID_PROBE_INJECTION_STATUS_PATTERN='AndroidCompatibilityProbe[[:space:]]+internal[[:space:]]+constructor|protocolLoadProbe|forbiddenApiScan[[:space:]]*=[[:space:]]*"external_guard_required"|external_guard_required'
PACKET_GENERATION_COMMONMAIN_PATTERN='generated/sources/libomvPackets/kotlin/commonMain'
PACKET_GENERATION_PRODUCTION_IMPORT_PATTERN='(^|[^[:alnum:]_.])libomv\.packets'
INVENTORY_CAPABILITY_FORBIDDEN_OWNER_PATTERN='(^|[^[:alnum:]_])(NotecardService|NotecardPort|ProtocolNotecardRuntime|LibomvNotecardAdapter|LibomvNotecardTextParser|NotecardAssetReader|BulkSender|NoticeSender|InventoryCataloguePort|InventoryLookupPort|InventoryBrowserService|ProtocolInventoryAssetSource)([^[:alnum:]_]|$)'
INVENTORY_CAPABILITY_EVENT_QUEUE_SEED_PATTERN='fun[[:space:]]+seed[[:space:]]*\(|seedBody|seedCapability'
INVENTORY_CAPABILITY_CLI_DIRECT_PROTOCOL_PATTERN='(^|[^[:alnum:]_])(ProtocolInventoryRuntime|ProtocolCurrentGroupsSource|ProtocolCapabilitySeedClient|EventQueueGetClient)([^[:alnum:]_]|$)'
INVENTORY_CAPABILITY_CLI_RAW_CAPABILITY_PATTERN='seedCapability|capabilityUrl|EventQueueGet|FetchInventory2|FetchInventoryDescendents2'
KMP_COMMON_FORBIDDEN_PLATFORM_PATTERN='java\.|javax\.|okhttp|android\.|System\.|MessageDigest|NetworkInterface|Datagram|ByteBuffer|UUID|Class\.forName|::class\.java'
KMP_PARALLEL_PATH_PATTERN='gem-core-kmp|gem-protocol-android|AndroidProtocolLibomvModule|JvmProtocolLibomvModule|GroupReader|CurrentGroupsClient|LoginRuntimeAndroid|(^|/)[^/]*(Manager|Utils|Helpers|Common)\.kt$|(^|[^[:alnum:]_])((data[[:space:]]+)?class|object|interface|fun)[[:space:]]+[A-Za-z0-9_]*(Manager|Utils|Helpers|Common)([^[:alnum:]_]|$)'
LEGACY_STALE_LOWER_PRODUCT='hostess'
LEGACY_STALE_TITLE_PRODUCT='Hostess'
GEM_STALE_PUBLIC_IDENTITY_PATTERN="org\\.${LEGACY_STALE_LOWER_PRODUCT}|${LEGACY_STALE_TITLE_PRODUCT}|${LEGACY_STALE_LOWER_PRODUCT}|:${LEGACY_STALE_LOWER_PRODUCT}-|check${LEGACY_STALE_TITLE_PRODUCT}Boundaries|data-${LEGACY_STALE_LOWER_PRODUCT}-|${LEGACY_STALE_LOWER_PRODUCT}-simulator-session|${LEGACY_STALE_LOWER_PRODUCT}_[^[:space:]]*\\.deb"
KMP_PLATFORM_API_PATTERN='okhttp3\.|OkHttpClient|System(::|\.)getenv|NetworkInterface|java\.net\.Datagram|Datagram(Packet|Socket)|javax\.xml|org\.xml\.sax|DocumentBuilderFactory|java\.util\.UUID|MessageDigest|java\.nio\.ByteBuffer'
OWNER_DECLARATION_PREFIX='^[[:space:]]*(internal[[:space:]]+|private[[:space:]]+|public[[:space:]]+)?(open[[:space:]]+|sealed[[:space:]]+)?(data[[:space:]]+class|class|object|interface|fun[[:space:]]+interface|value[[:space:]]+class)[[:space:]]+'

add_existing() {
    local -n target="$1"
    shift

    for path in "$@"; do
        if [[ -e "$path" ]]; then
            target+=("$path")
        fi
    done
}

check_no_hits() {
    local label="$1"
    local pattern="$2"
    shift 2

    if [[ "$#" -eq 0 ]]; then
        echo "ERROR: $label has no scan targets"
        failures=1
        return
    fi

    local output
    local status

    set +e
    output="$(rg -n \
        --glob '!**/build/**' \
        --glob '!**/.gradle/**' \
        --glob '!**/.kotlin/**' \
        -- "$pattern" "$@" 2>&1)"
    status="$?"
    set -e

    case "$status" in
        0)
            echo "FAIL: $label"
            echo "$output"
            failures=1
            ;;
        1)
            echo "PASS: $label"
            ;;
        *)
            echo "ERROR: $label scan failed"
            echo "$output"
            failures=1
            ;;
    esac
}

check_required_hits() {
    local label="$1"
    local pattern="$2"
    shift 2

    if [[ "$#" -eq 0 ]]; then
        echo "ERROR: $label has no scan targets"
        failures=1
        return
    fi

    local output
    local status

    set +e
    output="$(rg -n -- "$pattern" "$@" 2>&1)"
    status="$?"
    set -e

    case "$status" in
        0)
            echo "PASS: $label"
            ;;
        1)
            echo "FAIL: $label"
            failures=1
            ;;
        *)
            echo "ERROR: $label scan failed"
            echo "$output"
            failures=1
            ;;
    esac
}

is_allowed_stale_gem_identity_hit() {
    local hit="$1"

    case "$hit" in
        apps/desktop/src/main/package/packaging-text.properties:*linux.staleHostessInstallMessage=Old\ local\ Hostess\ files\ were\ found\ at\ /opt/hostess.*) return 0 ;;
        apps/desktop/build.gradle.kts:*linuxStaleHostessInstallMessage*) return 0 ;;
        apps/desktop/build.gradle.kts:*STALE_HOSTESS_MESSAGE*) return 0 ;;
        apps/desktop/build.gradle.kts:*is_project_jpackage_root\ /opt/hostess\ hostess\ hostess.cfg*) return 0 ;;
        apps/desktop/src/main/package/windows/gem-running-instance-check.vbs:*org.hostess.apps.desktop.HostessDesktopAppKt*) return 0 ;;
        apps/desktop/src/main/package/windows/gem-running-instance-check.vbs:*\\Program\ Files\\Hostess\\*) return 0 ;;
        apps/desktop/src/main/package/windows/gem-running-instance-check.vbs:*\\hostess.exe*) return 0 ;;
        *) return 1 ;;
    esac
}

check_pattern_matches() {
    local label="$1"
    local pattern="$2"
    local sample="$3"

    local output
    local status

    set +e
    output="$(printf '%s\n' "$sample" | rg -n -- "$pattern" 2>&1)"
    status="$?"
    set -e

    case "$status" in
        0)
            echo "PASS: $label"
            ;;
        1)
            echo "FAIL: $label did not detect fixture"
            failures=1
            ;;
        *)
            echo "ERROR: $label self-test failed"
            echo "$output"
            failures=1
            ;;
    esac
}

check_pattern_does_not_match() {
    local label="$1"
    local pattern="$2"
    local sample="$3"

    local output
    local status

    set +e
    output="$(printf '%s\n' "$sample" | rg -n -- "$pattern" 2>&1)"
    status="$?"
    set -e

    case "$status" in
        0)
            echo "FAIL: $label matched preserved fixture"
            echo "$output"
            failures=1
            ;;
        1)
            echo "PASS: $label"
            ;;
        *)
            echo "ERROR: $label self-test failed"
            echo "$output"
            failures=1
            ;;
    esac
}

check_public_source_briefing_label_identifier_pattern() {
    check_pattern_matches \
        "self-test public briefing-label detector catches TRACK_D_SOMETHING" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'TRACK_D_SOMETHING'

    check_pattern_matches \
        "self-test public briefing-label detector catches TRACK_E" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'TRACK_E'

    check_pattern_matches \
        "self-test public briefing-label detector catches HS002_TRACK_E_SOMETHING" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'HS002_TRACK_E_SOMETHING'

    check_pattern_matches \
        "self-test public briefing-label detector catches HS002-E" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'HS002-E'

    check_pattern_matches \
        "self-test public briefing-label detector catches HS001-B-01" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'HS001-B-01'

    check_pattern_matches \
        "self-test public briefing-label detector catches Track B" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'Track B'

    check_pattern_matches \
        "self-test public briefing-label detector catches TrackDS" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'TrackDS'

    check_pattern_matches \
        "self-test public briefing-label detector catches trackAVaultLoad" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'trackAVaultLoad'

    check_pattern_matches \
        "self-test public briefing-label detector catches track_e_targets" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'track_e_targets'

    check_pattern_matches \
        "self-test public briefing-label detector catches E-05-T1" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'E-05-T1'

    check_pattern_does_not_match \
        "self-test public briefing-label detector preserves TrackAgent" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'TrackAgent'

    check_pattern_does_not_match \
        "self-test public briefing-label detector preserves toggleTrack" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'toggleTrack'

    check_pattern_does_not_match \
        "self-test public briefing-label detector preserves toggleTrackSelected" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'toggleTrackSelected'

    check_pattern_does_not_match \
        "self-test public briefing-label detector preserves trackingState" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        'trackingState'
}

check_no_forbidden_files() {
    local label="$1"
    shift

    local matches=()
    while IFS= read -r path; do
        matches+=("$path")
    done < <(find "$@" -type f \
        \( -path '*/src/main/*' -o -path '*/src/commonMain/*' \
        -o -path '*/src/jvmMain/*' -o -path '*/src/androidMain/*' \
        -o -path '*/src/jvmAndroidMain/*' \) \
        \( -name 'LoginCompliance.kt' -o -name 'NoticeCompliance.kt' \
        -o -name '*Manager.kt' -o -name '*Helper.kt' -o -name '*Utils.kt' -o -name '*Common.kt' \) \
        2>/dev/null || true)

    if [[ "${#matches[@]}" -eq 0 ]]; then
        echo "PASS: $label"
    else
        echo "FAIL: $label"
        printf '%s\n' "${matches[@]}"
        failures=1
    fi
}

check_no_file_path_hits() {
    local label="$1"
    local pattern="$2"
    shift 2

    if [[ "$#" -eq 0 ]]; then
        echo "ERROR: $label has no scan targets"
        failures=1
        return
    fi

    local output
    local status
    set +e
    output="$(find "$@" -type f -name '*.kt' 2>/dev/null | rg -n -- "$pattern" 2>&1)"
    status="$?"
    set -e

    case "$status" in
        0)
            echo "FAIL: $label"
            echo "$output"
            failures=1
            ;;
        1)
            echo "PASS: $label"
            ;;
        *)
            echo "ERROR: $label scan failed"
            echo "$output"
            failures=1
            ;;
    esac
}

check_no_old_shared_roots() {
    local matches=()
    while IFS= read -r path; do
        matches+=("$path")
    done < <(find \
        "gem-core/src/main/kotlin" \
        "gem-protocol-libomv/src/main/kotlin" \
        -type f 2>/dev/null || true)

    if [[ "${#matches[@]}" -eq 0 ]]; then
        echo "PASS: KMP migration old shared production roots absent"
    else
        echo "FAIL: KMP migration old shared production roots absent"
        printf '%s\n' "${matches[@]}"
        failures=1
    fi
}

check_no_hostess_module_dirs() {
    local matches=()
    while IFS= read -r path; do
        matches+=("$path")
    done < <(find . -maxdepth 1 -type d -name 'hostess-*' -print 2>/dev/null || true)

    if [[ "${#matches[@]}" -eq 0 ]]; then
        echo "PASS: Gem technical rename old hostess module directories absent"
    else
        echo "FAIL: Gem technical rename old hostess module directories absent"
        printf '%s\n' "${matches[@]}"
        failures=1
    fi
}

check_no_stale_gem_identity() {
    local targets=()
    local path
    while IFS= read -r path; do
        case "$path" in
            ./tools/guards/check-boundaries.sh) ;;
            ./tools/guards/README.md) ;;
            ./gem-protocol-libomv/PROMOTION-MANIFEST.md) ;;
            ./gem-credential-vault/src/jvmMain/kotlin/org/gem/credential/vault/LegacyDesktopVaultMigration.kt) ;;
            ./gem-credential-vault/src/jvmTest/kotlin/org/gem/credential/vault/LegacyDesktopVaultMigrationTest.kt) ;;
            ./gem-preferences/src/jvmMain/kotlin/org/gem/preferences/LegacyDesktopPreferenceMigration.kt) ;;
            ./gem-preferences/src/jvmTest/kotlin/org/gem/preferences/LegacyDesktopPreferenceMigrationTest.kt) ;;
            ./apps/desktop/src/main/kotlin/org/gem/apps/desktop/GemDesktopSingleInstanceGuard.kt) ;;
            ./apps/desktop/src/test/kotlin/org/gem/apps/desktop/GemDesktopCompositionRootTest.kt) ;;
            ./apps/desktop/src/test/kotlin/org/gem/apps/desktop/GemDesktopSingleInstanceGuardTest.kt) ;;
            *) targets+=("${path#./}") ;;
        esac
    done < <(find . \
        \( -path './build' -o -path './.gradle' -o -path './.kotlin' -o -path '*/build' \) -prune \
        -o -type f \
        \( -path './apps/*' -o -path './gem-*/*' -o -path './tools/*' \
        -o -path './README.md' -o -path './settings.gradle.kts' \
        -o -path './build.gradle.kts' -o -path './gradle/*' \) \
        -print 2>/dev/null || true)

    local output
    local unexpected=()
    local status

    set +e
    output="$(rg -n \
        --glob '!**/build/**' \
        --glob '!**/.gradle/**' \
        --glob '!**/.kotlin/**' \
        -- "$GEM_STALE_PUBLIC_IDENTITY_PATTERN" "${targets[@]}" 2>&1)"
    status="$?"
    set -e

    case "$status" in
        0)
            local hit
            while IFS= read -r hit; do
                if ! is_allowed_stale_gem_identity_hit "$hit"; then
                    unexpected+=("$hit")
                fi
            done <<< "$output"

            if [[ "${#unexpected[@]}" -eq 0 ]]; then
                echo "PASS: Gem technical rename stale Hostess public identity absent"
            else
                echo "FAIL: Gem technical rename stale Hostess public identity absent"
                printf '%s\n' "${unexpected[@]}"
                failures=1
            fi
            ;;
        1)
            echo "PASS: Gem technical rename stale Hostess public identity absent"
            ;;
        *)
            echo "ERROR: Gem technical rename stale Hostess public identity scan failed"
            echo "$output"
            failures=1
            ;;
    esac
}

check_path_exists() {
    local label="$1"
    local path="$2"

    if [[ -e "$path" ]]; then
        echo "PASS: $label"
    else
        echo "FAIL: $label missing $path"
        failures=1
    fi
}

check_exact_owner_count() {
    local label="$1"
    local owner="$2"
    local expected="$3"
    shift 3

    if [[ "$#" -eq 0 ]]; then
        echo "ERROR: $label has no scan targets"
        failures=1
        return
    fi

    local output
    local status
    set +e
    output="$(rg -n -- "${OWNER_DECLARATION_PREFIX}${owner}([^[:alnum:]_]|$)" "$@" 2>&1)"
    status="$?"
    set -e

    if [[ "$status" -gt 1 ]]; then
        echo "ERROR: $label scan failed"
        echo "$output"
        failures=1
        return
    fi

    local count=0
    if [[ -n "$output" ]]; then
        count="$(printf '%s\n' "$output" | wc -l | tr -d ' ')"
    fi

    if [[ "$count" -eq "$expected" ]]; then
        echo "PASS: $label"
    else
        echo "FAIL: $label expected $expected declaration(s), found $count"
        [[ -n "$output" ]] && echo "$output"
        failures=1
    fi
}

check_notice_dispatch_overloads() {
    local files=()
    add_existing files \
        "gem-core/src/commonMain/kotlin/org/gem/core/services/NoticeDispatchService.kt" \
        "gem-core/src/main/kotlin/org/gem/core/services/NoticeDispatchService.kt"

    if [[ "${#files[@]}" -eq 0 ]]; then
        echo "ERROR: notice totals notice dispatch overload scan has no scan targets"
        failures=1
        return
    fi

    set +e
    local output
    output="$(perl -0ne 'while (/fun\s+dispatch\s*\((.*?)\)\s*:/sg) { print "$ARGV: dispatch overload keeps notice compliance argument\n" if $1 =~ /NoticeComplianceRequest|compliance\s*:/ }' "${files[@]}" 2>&1)"
    local status="$?"
    set -e

    if [[ "$status" -ne 0 ]]; then
        echo "ERROR: login compliance notice dispatch overload scan failed"
        echo "$output"
        failures=1
    elif [[ -n "$output" ]]; then
        echo "FAIL: notice totals notice dispatch overload forbids local notice compliance"
        echo "$output"
        failures=1
    else
        echo "PASS: notice totals notice dispatch overload forbids local notice compliance"
    fi
}

check_notice_dispatch_call_blocks() {
    local label="$1"
    shift

    local output=""
    local path line block
    while IFS=: read -r path line _; do
        [[ -n "$path" && -n "$line" ]] || continue
        block="$(sed -n "${line},$((line + 12))p" "$path")"
        if printf '%s\n' "$block" | rg -q 'compliance[[:space:]]*='; then
            output+="${path}:${line}: noticeDispatchService.dispatch call keeps deleted compliance argument"$'\n'
        fi
    done < <(rg -n 'noticeDispatchService\.dispatch\(' "$@" 2>/dev/null || true)

    if [[ -z "$output" ]]; then
        echo "PASS: $label"
    else
        echo "FAIL: $label"
        printf '%s' "$output"
        failures=1
    fi
}

check_simulator_session_boundaries() {
    local session_production_targets=()
    while IFS= read -r path; do
        case "$path" in
            *"/ThreadedSimulatorSessionGateway.kt") ;;
            *) session_production_targets+=("$path") ;;
        esac
    done < <(find \
        "gem-core/src/commonMain" \
        "gem-core/src/jvmMain" \
        "gem-core/src/androidMain" \
        "gem-core/src/jvmAndroidMain" \
        "gem-core/src/main" \
        "gem-protocol-libomv/src/commonMain" \
        "gem-protocol-libomv/src/jvmMain" \
        "gem-protocol-libomv/src/androidMain" \
        "gem-protocol-libomv/src/jvmAndroidMain" \
        "gem-protocol-libomv/src/main" \
        "gem-ui/src/commonMain" \
        "gem-ui/src/jvmMain" \
        "gem-ui/src/androidMain" \
        "tools/cli/src/main" \
        "apps/desktop/src/main" \
        "apps/android/src/main" \
        -type f -name '*.kt' 2>/dev/null || true)

    check_no_hits \
        "simulator session direct SimulatorPacketExchange receive outside session gateway" \
        "$SIMULATOR_SESSION_FORBIDDEN_FACADE_RECEIVE_PATTERN" \
        "${session_production_targets[@]}"

    local circuit_client_targets=()
    add_existing circuit_client_targets \
        "gem-protocol-libomv/src/commonMain/kotlin/org/gem/protocol/libomv/transport/ProtocolSimulatorCircuitClient.kt"

    check_no_hits \
        "simulator session old circuit-client local receive state absent" \
        "$SIMULATOR_SESSION_DEAD_CIRCUIT_CLIENT_PATTERN" \
        "${circuit_client_targets[@]}"

    local session_gateway_targets=()
    add_existing session_gateway_targets \
        "gem-protocol-libomv/src/commonMain" \
        "gem-protocol-libomv/src/jvmMain" \
        "gem-protocol-libomv/src/androidMain" \
        "gem-protocol-libomv/src/jvmAndroidMain" \
        "gem-protocol-libomv/src/main"

    check_exact_owner_count \
        "simulator session single ThreadedSimulatorSessionGateway owner" \
        "ThreadedSimulatorSessionGateway" \
        1 \
        "${session_gateway_targets[@]}"

    check_no_hits \
        "simulator session no duplicate simulator session owner names" \
        "$SIMULATOR_SESSION_DUPLICATE_GATEWAY_PATTERN" \
        "${session_gateway_targets[@]}"

    check_no_hits \
        "simulator session production fake live simulator success routes absent" \
        "$SIMULATOR_SESSION_FORBIDDEN_FAKE_LIVE_PATTERN" \
        "${session_production_targets[@]}"

    local app_core_ui_cli_targets=()
    add_existing app_core_ui_cli_targets \
        "gem-core/src/commonMain" \
        "gem-core/src/jvmMain" \
        "gem-core/src/androidMain" \
        "gem-core/src/jvmAndroidMain" \
        "gem-core/src/main" \
        "gem-ui/src/commonMain" \
        "gem-ui/src/jvmMain" \
        "gem-ui/src/androidMain" \
        "tools/cli/src/main" \
        "apps/desktop/src/main" \
        "apps/android/src/main"

    check_no_hits \
        "simulator session SimulatorSessionGateway stays inside protocol adapter" \
        'SimulatorSessionGateway' \
        "${app_core_ui_cli_targets[@]}"

    local raw_identifier_targets=()
    add_existing raw_identifier_targets \
        "gem-core/src/commonMain" \
        "gem-core/src/jvmMain" \
        "gem-core/src/androidMain" \
        "gem-core/src/jvmAndroidMain" \
        "gem-core/src/main" \
        "gem-protocol-libomv/src/commonMain" \
        "gem-protocol-libomv/src/jvmMain" \
        "gem-protocol-libomv/src/androidMain" \
        "gem-protocol-libomv/src/jvmAndroidMain" \
        "gem-protocol-libomv/src/main" \
        "gem-ui/src/commonMain" \
        "gem-ui/src/jvmMain" \
        "gem-ui/src/androidMain" \
        "tools/cli/src/main" \
        "apps/desktop/src/main" \
        "apps/android/src/main" \
        "README.md"

    check_no_hits \
        "simulator session briefing-label persisted identifiers absent" \
        "$PUBLIC_SOURCE_BRIEFING_LABEL_IDENTIFIER_PATTERN" \
        "${raw_identifier_targets[@]}"
}

core_targets=()
add_existing core_targets \
    "gem-core/build.gradle.kts" \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main"

app_cli_targets=()
add_existing app_cli_targets \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

cli_command_targets=()
add_existing cli_command_targets \
    "tools/cli/src/main/kotlin/org/gem/tools/cli/commands"

production_targets=()
add_existing production_targets \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle/libs.versions.toml" \
    "gem-core/build.gradle.kts" \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/build.gradle.kts" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "gem-ui/build.gradle.kts" \
    "gem-ui/src/commonMain" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/androidMain" \
    "tools/cli/build.gradle.kts" \
    "tools/cli/src/main" \
    "apps/desktop/build.gradle.kts" \
    "apps/desktop/src/main" \
    "apps/android/build.gradle.kts" \
    "apps/android/src/main"

kmp_all_source_targets=()
add_existing kmp_all_source_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/commonTest" \
    "gem-core/src/jvmMain" \
    "gem-core/src/jvmTest" \
    "gem-core/src/androidMain" \
    "gem-core/src/androidTest" \
    "gem-core/src/androidUnitTest" \
    "gem-core/src/androidHostTest" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/commonTest" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/jvmTest" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/androidTest" \
    "gem-protocol-libomv/src/androidUnitTest" \
    "gem-protocol-libomv/src/androidHostTest" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "gem-protocol-libomv/build/generated/sources/libomvPackets/kotlin/commonMain" \
    "gem-ui/src/commonMain" \
    "gem-ui/src/commonTest" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/jvmTest" \
    "gem-ui/src/androidMain" \
    "gem-ui/src/androidTest" \
    "tools/cli/src/main" \
    "tools/cli/src/test" \
    "apps/desktop/src/main" \
    "apps/desktop/src/test" \
    "apps/android/src/main" \
    "apps/android/src/test"

kmp_common_targets=()
add_existing kmp_common_targets \
    "gem-core/src/commonMain" \
    "gem-protocol-libomv/src/commonMain"

architecture_owner_targets=()
add_existing architecture_owner_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-ui/src/commonMain" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/androidMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

kmp_platform_api_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/transport/OkHttpProtocolHttpClient.kt) ;;
        gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/transport/UdpSimulatorDatagramSender.kt) ;;
        gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/runtime/JvmMd5DigestPort.kt) ;;
        gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/runtime/EnvironmentLoginSecretResolver.kt) ;;
        gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/runtime/DefaultGemHardwareAddressSource.kt) ;;
        gem-protocol-libomv/src/androidMain/kotlin/org/gem/protocol/libomv/runtime/AndroidGemProtocolIdentityProviders.kt) ;;
        apps/desktop/src/main/kotlin/org/gem/apps/desktop/GemDesktopStorageMigration.kt) ;;
        apps/desktop/src/main/kotlin/org/gem/apps/desktop/DesktopVaultComposition.kt) ;;
        apps/desktop/src/main/kotlin/org/gem/apps/desktop/DesktopPreferenceComposition.kt) ;;
        *) kmp_platform_api_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "gem-ui/src/commonMain" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/androidMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

okhttp_forbidden_targets=()
add_existing okhttp_forbidden_targets \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle/libs.versions.toml" \
    "gem-core/build.gradle.kts" \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/build.gradle.kts" \
    "tools/cli/build.gradle.kts" \
    "tools/cli/src/main" \
    "apps/desktop/build.gradle.kts" \
    "apps/desktop/src/main" \
    "apps/android/build.gradle.kts" \
    "apps/android/src/main"
for protocol_root in \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main"; do
    while IFS= read -r path; do
        okhttp_forbidden_targets+=("$path")
    done < <(find "$protocol_root" -type f ! -path '*/transport/*' 2>/dev/null || true)
done

protocol_boundary_runtime_forbidden_targets=()
add_existing protocol_boundary_runtime_forbidden_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

ui_targets=()
add_existing ui_targets \
    "gem-ui/src/commonMain" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/androidMain"

ui_text_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/GemText.kt") ;;
        *) ui_text_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-ui/src/commonMain" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/androidMain" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f 2>/dev/null || true)
add_existing ui_text_forbidden_targets \
    "apps/desktop/build.gradle.kts" \
    "apps/android/build.gradle.kts"

ui_style_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/org/gem/ui/design/"*) ;;
        *) ui_style_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-ui/src/commonMain/kotlin/org/gem/ui" \
    "gem-ui/src/jvmMain/kotlin/org/gem/ui" \
    "gem-ui/src/androidMain/kotlin/org/gem/ui" \
    -type f -name '*.kt' 2>/dev/null || true)

ui_direct_control_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/GemButtons.kt") ;;
        *"/GemDropdowns.kt") ;;
        *"/GemFields.kt") ;;
        *"/GemOverflowMenu.kt") ;;
        *"/GemRows.kt") ;;
        *"/GemScrollablePane.kt") ;;
        *"/GemScrollablePane.jvm.kt") ;;
        *"/GemScrollablePane.android.kt") ;;
        *) ui_direct_control_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-ui/src/commonMain/kotlin/org/gem/ui" \
    "gem-ui/src/jvmMain/kotlin/org/gem/ui" \
    "gem-ui/src/androidMain/kotlin/org/gem/ui" \
    -type f -name '*.kt' 2>/dev/null || true)

ui_remediation_split_login_targets=()
add_existing ui_remediation_split_login_targets \
    "gem-ui/src/commonMain"

ui_remediation_fake_location_targets=()
add_existing ui_remediation_fake_location_targets \
    "gem-ui/src/commonMain" \
    "gem-core/src/commonMain" \
    "gem-protocol-libomv/src/commonMain"

ui_remediation_scaffold_required_targets=()
add_existing ui_remediation_scaffold_required_targets \
    "gem-ui/src/commonMain/kotlin/org/gem/ui/GemApp.kt" \
    "gem-ui/src/commonMain/kotlin/org/gem/ui/components/GemAppScaffold.kt"

ui_remediation_custom_icon_targets=()
add_existing ui_remediation_custom_icon_targets \
    "gem-ui/src/commonMain/kotlin/org/gem/ui/components/GemIcons.kt"

theme_ui_adapter_targets=()
add_existing theme_ui_adapter_targets \
    "gem-ui/build.gradle.kts" \
    "gem-ui/src/commonMain" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/androidMain"

theme_vault_storage_targets=()
add_existing theme_vault_storage_targets \
    "gem-credential-vault/build.gradle.kts" \
    "gem-credential-vault/src/commonMain" \
    "gem-credential-vault/src/jvmMain" \
    "gem-credential-vault/src/androidMain" \
    "gem-credential-vault/src/jvmAndroidMain"

theme_colour_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/org/gem/ui/design/"*) ;;
        *) theme_colour_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-ui/src/commonMain/kotlin/org/gem/ui" \
    "gem-ui/src/jvmMain/kotlin/org/gem/ui" \
    "gem-ui/src/androidMain/kotlin/org/gem/ui" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

theme_visible_label_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/GemText.kt") ;;
        *) theme_visible_label_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-ui/src/commonMain" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/androidMain" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

theme_prototype_forbidden_targets=()
add_existing theme_prototype_forbidden_targets \
    "gem-ui/src/commonMain" \
    "gem-ui/src/jvmMain" \
    "gem-ui/src/androidMain" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

theme_root_project_label_targets=()
add_existing theme_root_project_label_targets \
    "apps/desktop/build.gradle.kts" \
    "apps/android/build.gradle.kts"

session_protocol_runtime_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        # Android no-UI load probe may mention protocol owner class names only.
        "apps/android/src/main/kotlin/org/gem/apps/android/AndroidCompatibilityProbe.kt") ;;
        *) session_protocol_runtime_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

credential_env_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/EnvironmentLoginSecretResolver.kt") ;;
        *"/GemDesktopStorageMigration.kt") ;;
        *"/DesktopVaultComposition.kt") ;;
        *"/DesktopPreferenceComposition.kt") ;;
        *) credential_env_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f 2>/dev/null || true)

credential_file_route_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/LiveProofInputs.kt") ;;
        *) credential_file_route_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f 2>/dev/null || true)

login_compliance_main_roots=(
    "gem-core"
    "gem-protocol-libomv"
    "tools"
    "apps"
)

login_compliance_tools_apps_targets=()
add_existing login_compliance_tools_apps_targets \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

notice_dispatch_targets=()
add_existing notice_dispatch_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

viewer_identity_provider_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        # Android compatibility probe must class-load this protocol identity provider.
        "apps/android/src/main/kotlin/org/gem/apps/android/AndroidCompatibilityProbe.kt") ;;
        *) viewer_identity_provider_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

report_secret_key_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/RedactedText.kt") ;;
        *) report_secret_key_targets+=("$path") ;;
    esac
done < <(find "tools/cli/src/main/kotlin/org/gem/tools/cli" -type f -name '*.kt' 2>/dev/null || true)

notice_cap_forbidden_targets=()
add_existing notice_cap_forbidden_targets \
    "tools/cli/src/main/kotlin/org/gem/tools/cli/commands/SendNoticeCommand.kt" \
    "tools/cli/src/main/kotlin/org/gem/tools/cli/commands/LiveProofRunner.kt"

login_package_direct_owner_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        # login package Android no-UI load probe may mention login package owner class names only.
        "apps/android/src/main/kotlin/org/gem/apps/android/AndroidCompatibilityProbe.kt") ;;
        *) login_package_direct_owner_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

login_package_targets=()
add_existing login_package_targets \
    "gem-protocol-libomv/src/commonMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "gem-protocol-libomv/src/jvmMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "gem-protocol-libomv/src/androidMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "gem-protocol-libomv/src/main/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt"
for protocol_runtime_root in \
    "gem-protocol-libomv/src/commonMain/kotlin/org/gem/protocol/libomv/runtime" \
    "gem-protocol-libomv/src/jvmMain/kotlin/org/gem/protocol/libomv/runtime" \
    "gem-protocol-libomv/src/androidMain/kotlin/org/gem/protocol/libomv/runtime" \
    "gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/runtime" \
    "gem-protocol-libomv/src/main/kotlin/org/gem/protocol/libomv/runtime"; do
    while IFS= read -r path; do
        login_package_targets+=("$path")
    done < <(find "$protocol_runtime_root" -maxdepth 1 -type f -name 'LoginPackage*.kt' 2>/dev/null || true)
done

session_login_service_targets=()
add_existing session_login_service_targets \
    "gem-core/src/commonMain/kotlin/org/gem/core/services/SessionService.kt" \
    "gem-core/src/main/kotlin/org/gem/core/services/SessionService.kt"

notice_time_targets=()
add_existing notice_time_targets \
    "gem-core/src/commonMain/kotlin/org/gem/core/services/NoticeComplianceService.kt" \
    "gem-core/src/main/kotlin/org/gem/core/services/NoticeComplianceService.kt"

login_package_old_login_targets=()
add_existing login_package_old_login_targets \
    "gem-protocol-libomv/src/commonMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "gem-protocol-libomv/src/jvmMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "gem-protocol-libomv/src/androidMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "gem-protocol-libomv/src/main/kotlin/org/gem/protocol/libomv/runtime/ProtocolLoginRuntime.kt"

attachment_kotlin_targets=()
add_existing attachment_kotlin_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/commonTest" \
    "gem-core/src/jvmMain" \
    "gem-core/src/jvmTest" \
    "gem-core/src/androidMain" \
    "gem-core/src/androidTest" \
    "gem-core/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/commonTest" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/jvmTest" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/androidTest" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "tools/cli/src/main" \
    "tools/cli/src/test" \
    "apps/desktop/src/main" \
    "apps/desktop/src/test" \
    "apps/android/src/main" \
    "apps/android/src/test" \
    "apps/android/src/androidTest"

live_proof_fake_default_targets=()
add_existing live_proof_fake_default_targets \
    "tools/cli/src/main" \
    "tools/cli/src/test"

live_proof_local_http_targets=()
add_existing live_proof_local_http_targets \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmAndroidMain"

android_probe_injection_targets=()
add_existing android_probe_injection_targets \
    "apps/android/src/main"

packet_generation_targets=()
add_existing packet_generation_targets \
    "gem-protocol-libomv/build.gradle.kts"

packet_generation_production_import_targets=()
add_existing packet_generation_production_import_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

inventory_capability_main_targets=()
add_existing inventory_capability_main_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

inventory_capability_event_queue_targets=()
add_existing inventory_capability_event_queue_targets \
    "gem-protocol-libomv/src/commonMain/kotlin/org/gem/protocol/libomv/transport/EventQueueGetClient.kt"

inventory_capability_cli_targets=()
add_existing inventory_capability_cli_targets \
    "tools/cli/src/main"

live_notice_proof_cli_send_targets=()
add_existing live_notice_proof_cli_send_targets \
    "tools/cli/src/main" \
    "tools/cli/README.md"

live_notice_proof_full_targets=()
add_existing live_notice_proof_full_targets \
    "tools/cli/src/main" \
    "tools/cli/src/test"

notice_protocol_stale_targets=()
add_existing notice_protocol_stale_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

notice_protocol_direct_app_targets=()
while IFS= read -r path; do
    case "$path" in
        # notice protocol Android no-UI load probe may mention notice protocol protocol owner class names only.
        "apps/android/src/main/kotlin/org/gem/apps/android/AndroidCompatibilityProbe.kt") ;;
        *) notice_protocol_direct_app_targets+=("$path") ;;
    esac
done < <(find \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

simulator_exchange_duplicate_targets=()
while IFS= read -r path; do
    case "$path" in
        "gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/transport/UdpSimulatorDatagramSender.kt") ;;
        *) simulator_exchange_duplicate_targets+=("$path") ;;
    esac
done < <(find \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

notice_archive_direct_targets=()
add_existing notice_archive_direct_targets \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

fake_live_proof_targets=()
add_existing fake_live_proof_targets \
    "tools/cli/src/main"

full_proof_archive_targets=()
add_existing full_proof_archive_targets \
    "tools/cli/src/main/kotlin/org/gem/tools/cli/commands/LiveNoticeSendProofRunner.kt"

group_membership_mutation_targets=()
add_existing group_membership_mutation_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

android_probe_targets=()
add_existing android_probe_targets \
    "apps/android/src/main/kotlin/org/gem/apps/android/AndroidCompatibilityProbe.kt"

vault_dependency_targets=()
add_existing vault_dependency_targets \
    "gradle/libs.versions.toml" \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gem-core/build.gradle.kts" \
    "gem-credential-vault/build.gradle.kts" \
    "gem-protocol-libomv/build.gradle.kts" \
    "tools/cli/build.gradle.kts" \
    "apps/desktop/build.gradle.kts" \
    "apps/android/build.gradle.kts"

vault_production_targets=()
add_existing vault_production_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-credential-vault/src/commonMain" \
    "gem-credential-vault/src/jvmMain" \
    "gem-credential-vault/src/androidMain" \
    "gem-credential-vault/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

vault_raw_key_forbidden_targets=()
add_existing vault_raw_key_forbidden_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

vault_app_composition_targets=()
add_existing vault_app_composition_targets \
    "apps/desktop/src/main" \
    "apps/android/src/main"

notice_totals_stale_compliance_targets=()
add_existing notice_totals_stale_compliance_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

notice_totals_public_doc_targets=()
add_existing notice_totals_public_doc_targets \
    "README.md" \
    "gem-core/README.md" \
    "tools/cli/README.md" \
    "apps/desktop/README.md" \
    "apps/android/README.md"

avatar_main_targets=()
add_existing avatar_main_targets \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

avatar_source_targets=()
add_existing avatar_source_targets \
    "gem-protocol-libomv/src/commonMain/kotlin/org/gem/protocol/libomv/runtime/ProtocolAvatarAppearanceSource.kt"

avatar_cli_command_targets=()
add_existing avatar_cli_command_targets \
    "tools/cli/src/main/kotlin/org/gem/tools/cli/commands"

avatar_full_proof_targets=()
add_existing avatar_full_proof_targets \
    "tools/cli/src/main/kotlin/org/gem/tools/cli/commands/LiveNoticeSendProofRunner.kt"

avatar_simulator_exchange_impl_targets=()
while IFS= read -r path; do
    case "$path" in
        "gem-protocol-libomv/src/commonMain/kotlin/org/gem/protocol/libomv/transport/SimulatorPacketExchange.kt") ;;
        "gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/transport/SimulatorPacketExchangeFactory.kt") ;;
        "gem-protocol-libomv/src/jvmAndroidMain/kotlin/org/gem/protocol/libomv/transport/UdpSimulatorDatagramSender.kt") ;;
        *) avatar_simulator_exchange_impl_targets+=("$path") ;;
    esac
done < <(find \
    "gem-core/src/commonMain" \
    "gem-core/src/jvmMain" \
    "gem-core/src/androidMain" \
    "gem-core/src/jvmAndroidMain" \
    "gem-core/src/main" \
    "gem-protocol-libomv/src/commonMain" \
    "gem-protocol-libomv/src/jvmMain" \
    "gem-protocol-libomv/src/androidMain" \
    "gem-protocol-libomv/src/jvmAndroidMain" \
    "gem-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

check_no_hostess_module_dirs

check_no_stale_gem_identity

check_no_hits \
    "gem-core forbidden dependencies" \
    "$CORE_FORBIDDEN_PATTERN" \
    "${core_targets[@]}"

check_no_hits \
    "CLI/app raw libomv use" \
    "$RAW_LIBOMV_PATTERN" \
    "${app_cli_targets[@]}"

check_no_hits \
    "private/reference production dependency" \
    "$PRIVATE_REFERENCE_PATTERN" \
    "${production_targets[@]}"

check_no_hits \
    "forbidden platform/security/logging calls" \
    "$FORBIDDEN_PLATFORM_PATTERN" \
    "${production_targets[@]}"

check_no_hits \
    "CLI command ad hoc report writes" \
    "$CLI_COMMAND_REPORT_WRITE_PATTERN" \
    "${cli_command_targets[@]}"

check_no_hits \
    "protocol boundary direct OkHttp outside protocol transport" \
    "$PROTOCOL_BOUNDARY_OKHTTP_PATTERN" \
    "${okhttp_forbidden_targets[@]}"

check_no_hits \
    "protocol boundary runtime/transport direct calls outside protocol module" \
    "$PROTOCOL_BOUNDARY_RUNTIME_PATTERN" \
    "${protocol_boundary_runtime_forbidden_targets[@]}"

check_path_exists \
    "protocol boundary shared UI module exists" \
    "gem-ui/src/commonMain"

check_no_hits \
    "protocol boundary UI direct protocol/runtime path" \
    "$UI_DIRECT_PROTOCOL_PATTERN" \
    "${ui_targets[@]}"

check_no_hits \
    "protocol boundary UI direct vault/native credential path" \
    "$UI_DIRECT_VAULT_PATTERN" \
    "${ui_targets[@]}"

check_no_hits \
    "protocol boundary UI direct notice/group send path" \
    "$UI_DIRECT_NOTICE_PATTERN" \
    "${ui_targets[@]}"

check_no_hits \
    "protocol boundary UI forbidden generic owner names" \
    "$UI_FORBIDDEN_OWNER_PATTERN" \
    "${ui_targets[@]}"

check_no_hits \
    "protocol boundary UI WebView/HTML prototype route" \
    "$UI_WEBVIEW_PATTERN" \
    "${ui_targets[@]}" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

check_no_hits \
    "protocol boundary UI unfinished staged code" \
    "$UI_STAGED_PATTERN" \
    "${ui_targets[@]}"

check_no_hits \
    "protocol boundary UI visible labels centralized" \
    "$UI_TEXT_CATALOGUE_PATTERN" \
    "${ui_text_forbidden_targets[@]}"

check_no_hits \
    "protocol boundary UI style constants centralized" \
    "$UI_STYLE_TOKEN_PATTERN" \
    "${ui_style_forbidden_targets[@]}"

check_no_hits \
    "protocol boundary UI raw controls centralized" \
    "$UI_DIRECT_CONTROL_PATTERN" \
    "${ui_direct_control_forbidden_targets[@]}"

check_no_hits \
    "session credential direct runtime/transport calls outside protocol module" \
    "$SESSION_PROTOCOL_DIRECT_RUNTIME_PATTERN" \
    "${session_protocol_runtime_forbidden_targets[@]}"

check_no_hits \
    "session credential raw env reads outside resolver" \
    "$CREDENTIAL_ENV_READ_PATTERN" \
    "${credential_env_forbidden_targets[@]}"

check_no_hits \
    "session credential unsupported file route outside blocking parser" \
    "$CREDENTIAL_FILE_ROUTE_PATTERN" \
    "${credential_file_route_forbidden_targets[@]}"

check_no_hits \
    "session credential unsupported secret stores" \
    "$UNSUPPORTED_SECRET_STORE_PATTERN" \
    "${production_targets[@]}"

check_no_hits \
    "session credential UI old split-login route" \
    "$UI_REMEDIATION_SPLIT_LOGIN_PATTERN" \
    "${ui_remediation_split_login_targets[@]}"

check_no_hits \
    "session credential UI fake session location route" \
    "$UI_REMEDIATION_FAKE_LOCATION_PATTERN" \
    "${ui_remediation_fake_location_targets[@]}"

check_required_hits \
    "session credential UI scaffold owner present" \
    "$UI_REMEDIATION_SCAFFOLD_REQUIRED_PATTERN" \
    "${ui_remediation_scaffold_required_targets[@]}"

check_no_hits \
    "session credential UI custom icon route" \
    "$UI_REMEDIATION_CUSTOM_ICON_PATTERN" \
    "${ui_remediation_custom_icon_targets[@]}"

check_no_hits \
    "theme UI has no preference adapter dependency" \
    "$THEME_UI_PREFERENCE_ADAPTER_PATTERN" \
    "${theme_ui_adapter_targets[@]}"

check_no_hits \
    "theme vault has no theme preference storage" \
    "$THEME_VAULT_STORAGE_PATTERN" \
    "${theme_vault_storage_targets[@]}"

check_no_hits \
    "theme Haccu colours centralized" \
    "$THEME_HACCU_COLOUR_PATTERN" \
    "${theme_colour_forbidden_targets[@]}"

check_no_hits \
    "theme visible labels centralized" \
    "$THEME_VISIBLE_LABEL_PATTERN" \
    "${theme_visible_label_forbidden_targets[@]}"

check_no_hits \
    "theme prototype runtime not promoted" \
    "$THEME_PROTOTYPE_RUNTIME_PATTERN" \
    "${theme_prototype_forbidden_targets[@]}"

check_required_hits \
    "theme brand logo owner present" \
    "$THEME_LOGO_OWNER_PATTERN" \
    "gem-ui/src/commonMain/kotlin/org/gem/ui/components/GemIcons.kt"

check_required_hits \
    "theme theme toggle owner present" \
    "$THEME_TOGGLE_OWNER_PATTERN" \
    "gem-ui/src/commonMain/kotlin/org/gem/ui/components/ThemeModeToggle.kt"

check_required_hits \
    "theme palette provider present" \
    "$THEME_PALETTE_OWNER_PATTERN" \
    "gem-ui/src/commonMain/kotlin/org/gem/ui/design/HaccuGemPaletteProvider.kt"

check_required_hits \
    "theme Android app label explicit" \
    "$THEME_ANDROID_LABEL_PATTERN" \
    "apps/android/build.gradle.kts"

check_required_hits \
    "theme desktop vendor explicit" \
    "$THEME_DESKTOP_VENDOR_PATTERN" \
    "apps/desktop/build.gradle.kts"

check_no_hits \
    "theme app labels do not derive from root project name" \
    "$THEME_ROOT_PROJECT_LABEL_PATTERN" \
    "${theme_root_project_label_targets[@]}"

check_no_hits \
    "vault forbidden vault dependency acquisition" \
    "$VAULT_FORBIDDEN_DEPENDENCY_PATTERN" \
    "${vault_dependency_targets[@]}"

check_no_hits \
    "vault stale credential unlock routes" \
    "$VAULT_STALE_CREDENTIAL_ROUTE_PATTERN" \
    "${vault_production_targets[@]}"

check_no_hits \
    "vault AndroidX crypto route absent" \
    "$VAULT_ANDROIDX_CRYPTO_PATTERN" \
    "${vault_production_targets[@]}" \
    "${vault_dependency_targets[@]}"

check_no_hits \
    "vault raw key exposure outside vault module" \
    "$VAULT_RAW_KEY_EXPOSURE_PATTERN" \
    "${vault_raw_key_forbidden_targets[@]}"

check_no_hits \
    "vault app composition does not use env secret resolver" \
    "$VAULT_APP_ENV_RESOLVER_PATTERN" \
    "${vault_app_composition_targets[@]}"

check_exact_owner_count "vault single AndroidKeystoreVaultKeySource owner" "AndroidKeystoreVaultKeySource" 1 "${vault_production_targets[@]}"
check_exact_owner_count "vault single LocalUserFileVaultKeySource owner" "LocalUserFileVaultKeySource" 1 "${vault_production_targets[@]}"
check_exact_owner_count "vault single DesktopVaultPaths owner" "DesktopVaultPaths" 1 "${vault_production_targets[@]}"
check_exact_owner_count "vault single CredentialVaultLoginSecretResolver owner" "CredentialVaultLoginSecretResolver" 1 "${vault_production_targets[@]}"

check_no_old_shared_roots

check_no_hits \
    "KMP migration commonMain forbidden platform APIs" \
    "$KMP_COMMON_FORBIDDEN_PLATFORM_PATTERN" \
    "${kmp_common_targets[@]}"

check_no_hits \
    "KMP migration no parallel KMP or generic owner paths" \
    "$KMP_PARALLEL_PATH_PATTERN" \
    "${kmp_all_source_targets[@]}"

check_no_hits \
    "KMP migration platform APIs confined to platform adapters" \
    "$KMP_PLATFORM_API_PATTERN" \
    "${kmp_platform_api_forbidden_targets[@]}"

check_exact_owner_count "KMP migration single GemInstant owner" "GemInstant" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single GemDelay owner" "GemDelay" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ClockPort owner" "ClockPort" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ProtocolLibomvModule owner" "ProtocolLibomvModule" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single LibomvPlatformAdapterBundle owner" "LibomvPlatformAdapterBundle" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ProtocolXmlTreeParser owner" "ProtocolXmlTreeParser" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ProtocolXmlElement owner" "ProtocolXmlElement" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single LibomvUuidCodec owner" "LibomvUuidCodec" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single UnsignedLongBitsParser owner" "UnsignedLongBitsParser" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single LibomvPacketCodec owner" "LibomvPacketCodec" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single LibomvBytePacketWriter owner" "LibomvBytePacketWriter" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ProtocolHttpRequestPolicy owner" "ProtocolHttpRequestPolicy" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single Md5DigestPort owner" "Md5DigestPort" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single LoginSecretJsonDecoder owner" "LoginSecretJsonDecoder" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single GemViewerIdentityBuilder owner" "GemViewerIdentityBuilder" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ProtocolLoginRuntime owner" "ProtocolLoginRuntime" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ProtocolGroupRuntime owner" "ProtocolGroupRuntime" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ProtocolInventoryRuntime owner" "ProtocolInventoryRuntime" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single ProtocolNoticeRuntime owner" "ProtocolNoticeRuntime" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "KMP migration single NoticeDispatchService owner" "NoticeDispatchService" 1 "${architecture_owner_targets[@]}"

check_no_forbidden_files \
    "login compliance generic owner file names" \
    "${login_compliance_main_roots[@]}"

check_no_hits \
    "login compliance old SessionService login overload" \
    "$SESSION_LOGIN_OVERLOAD_PATTERN" \
    "${session_login_service_targets[@]}"

check_no_hits \
    "login compliance one-argument CLI/app login calls" \
    "$SESSION_LOGIN_ONE_ARG_PATTERN" \
    "${login_compliance_tools_apps_targets[@]}"

check_notice_dispatch_overloads

check_no_hits \
    "login compliance old notice dispatch single-line route" \
    "$NOTICE_DISPATCH_OLD_CALL_PATTERN" \
    "${notice_dispatch_targets[@]}"

check_notice_dispatch_call_blocks \
    "notice totals notice dispatch calls forbid named compliance" \
    "${notice_dispatch_targets[@]}"

check_no_hits \
    "login compliance direct tools/apps group notice send" \
    "$NOTICE_DISPATCH_DIRECT_GROUP_SEND_PATTERN" \
    "${login_compliance_tools_apps_targets[@]}"

check_no_hits \
    "login compliance spoofed viewer channel names" \
    "$VIEWER_IDENTITY_SPOOFED_CHANNEL_PATTERN" \
    "${production_targets[@]}"

check_no_hits \
    "login package old LLSD login route" \
    "$LOGIN_PACKAGE_OLD_LLSD_PATTERN" \
    "${login_package_old_login_targets[@]}"

check_no_hits \
    "login package direct package owners outside protocol module" \
    "$LOGIN_PACKAGE_DIRECT_OWNER_PATTERN" \
    "${login_package_direct_owner_forbidden_targets[@]}"

check_no_hits \
    "login package stale login package fields" \
    "$LOGIN_PACKAGE_STALE_FIELD_PATTERN" \
    "${login_package_targets[@]}"

check_no_hits \
    "attachment cleanup stale create/upload symbols" \
    "$ATTACHMENT_STALE_CREATE_UPLOAD_PATTERN" \
    "${attachment_kotlin_targets[@]}"

check_no_file_path_hits \
    "attachment cleanup forbidden attachment owner file names" \
    "$ATTACHMENT_FORBIDDEN_OWNER_FILE_PATTERN" \
    "${attachment_kotlin_targets[@]}"

check_no_hits \
    "attachment cleanup forbidden attachment owner declarations" \
    "$ATTACHMENT_FORBIDDEN_OWNER_DECL_PATTERN" \
    "${attachment_kotlin_targets[@]}"

check_no_hits \
    "attachment cleanup implicit fake defaults" \
    "$LIVE_PROOF_IMPLICIT_FAKE_PATTERN" \
    "${live_proof_fake_default_targets[@]}"

check_no_hits \
    "attachment cleanup production local HTTP allowance" \
    "$LIVE_PROOF_LOCAL_HTTP_PATTERN" \
    "${live_proof_local_http_targets[@]}"

check_no_hits \
    "attachment cleanup Android probe injection/status leakage" \
    "$ANDROID_PROBE_INJECTION_STATUS_PATTERN" \
    "${android_probe_injection_targets[@]}"

check_no_hits \
    "attachment cleanup generated packet catalogue not wired to commonMain" \
    "$PACKET_GENERATION_COMMONMAIN_PATTERN" \
    "${packet_generation_targets[@]}"

check_no_hits \
    "attachment cleanup generated packet imports absent from production source" \
    "$PACKET_GENERATION_PRODUCTION_IMPORT_PATTERN" \
    "${packet_generation_production_import_targets[@]}"

check_no_hits \
    "inventory capability forbidden capability/notecard owner names" \
    "$INVENTORY_CAPABILITY_FORBIDDEN_OWNER_PATTERN" \
    "${inventory_capability_main_targets[@]}"

check_no_hits \
    "inventory capability old EventQueue seed route" \
    "$INVENTORY_CAPABILITY_EVENT_QUEUE_SEED_PATTERN" \
    "${inventory_capability_event_queue_targets[@]}"

check_no_hits \
    "inventory capability CLI direct protocol route" \
    "$INVENTORY_CAPABILITY_CLI_DIRECT_PROTOCOL_PATTERN" \
    "${inventory_capability_cli_targets[@]}"

check_no_hits \
    "inventory capability CLI raw capability evidence labels" \
    "$INVENTORY_CAPABILITY_CLI_RAW_CAPABILITY_PATTERN" \
    "${inventory_capability_cli_targets[@]}"

check_no_hits \
    "notice protocol stale bounded circuit owner" \
    "$SIMULATOR_CIRCUIT_STALE_OWNER_PATTERN" \
    "${kmp_all_source_targets[@]}"

check_no_hits \
    "notice protocol stale CLI live send path" \
    "$LIVE_NOTICE_PROOF_STALE_CLI_SEND_PATTERN" \
    "${live_notice_proof_cli_send_targets[@]}"

check_no_hits \
    "notice protocol stale full proof routes" \
    "$LIVE_NOTICE_PROOF_STALE_FULL_PROOF_PATTERN" \
    "${live_notice_proof_full_targets[@]}"

check_no_hits \
    "notice protocol stale notice owner routes" \
    "$NOTICE_PROTOCOL_STALE_OWNER_PATTERN" \
    "${notice_protocol_stale_targets[@]}"

check_no_hits \
    "notice protocol direct CLI/app notice protocol routes" \
    "$NOTICE_PROTOCOL_DIRECT_APP_CALL_PATTERN" \
    "${notice_protocol_direct_app_targets[@]}"

check_no_hits \
    "simulator archive duplicate simulator exchange outside UDP owner" \
    "$SIMULATOR_EXCHANGE_DUPLICATE_PATTERN" \
    "${simulator_exchange_duplicate_targets[@]}"

check_no_hits \
    "simulator archive direct CLI/app archive protocol routes" \
    "$NOTICE_ARCHIVE_DIRECT_PATTERN" \
    "${notice_archive_direct_targets[@]}"

check_no_hits \
    "simulator archive fake live proof pass route" \
    "$FAKE_LIVE_PROOF_PASS_PATTERN" \
    "${fake_live_proof_targets[@]}"

check_required_hits \
    "simulator archive full proof requires archive read-back" \
    "$FULL_PROOF_ARCHIVE_REQUIRED_PATTERN" \
    "${full_proof_archive_targets[@]}"

check_no_hits \
    "Incident freeze forbids group membership mutation packets" \
    "$GROUP_MEMBERSHIP_MUTATION_PATTERN" \
    "${group_membership_mutation_targets[@]}"

check_no_hits \
    "Android compatibility probe forbidden runtime routes" \
    "$ANDROID_PROBE_FORBIDDEN_RUNTIME_PATTERN" \
    "${android_probe_targets[@]}"

check_no_hits \
    "notice totals stale local notice totals production paths" \
    "$NOTICE_TOTALS_STALE_COMPLIANCE_PATTERN" \
    "${notice_totals_stale_compliance_targets[@]}"

check_no_hits \
    "notice totals stale local notice totals public docs" \
    "$NOTICE_TOTALS_STALE_COMPLIANCE_PATTERN" \
    "${notice_totals_public_doc_targets[@]}"

check_no_hits \
    "avatar readiness forbids classic avatar baking production paths" \
    "$AVATAR_CLASSIC_BAKING_PATTERN" \
    "${avatar_main_targets[@]}"

check_no_hits \
    "avatar readiness forbidden avatar owner names" \
    "$AVATAR_FORBIDDEN_OWNER_PATTERN" \
    "${avatar_main_targets[@]}"

check_no_hits \
    "avatar readiness avatar appearance source does not seed capabilities directly" \
    "$AVATAR_DIRECT_CAPABILITY_SEED_PATTERN" \
    "${avatar_source_targets[@]}"

check_no_hits \
    "avatar readiness CLI commands do not construct protocol avatar runtime" \
    "$AVATAR_CLI_DIRECT_PROTOCOL_PATTERN" \
    "${avatar_cli_command_targets[@]}"

check_no_hits \
    "avatar readiness full proof no stale simulator-presence gate" \
    "$AVATAR_STALE_FULL_PROOF_SIMULATOR_GATE_PATTERN" \
    "${avatar_full_proof_targets[@]}"

check_no_hits \
    "avatar readiness no extra SimulatorPacketExchange implementations" \
    "$AVATAR_EXTRA_SIMULATOR_EXCHANGE_IMPL_PATTERN" \
    "${avatar_simulator_exchange_impl_targets[@]}"

check_simulator_session_boundaries

check_exact_owner_count "inventory capability single InventoryPort owner" "InventoryPort" 1 "${inventory_capability_main_targets[@]}"
check_exact_owner_count "inventory capability single InventoryDirectoryService owner" "InventoryDirectoryService" 1 "${inventory_capability_main_targets[@]}"
check_exact_owner_count "inventory capability single ProtocolCapabilitySeedClient owner" "ProtocolCapabilitySeedClient" 1 "${inventory_capability_main_targets[@]}"
check_exact_owner_count "inventory capability single ProtocolCapabilityCacheProvider owner" "ProtocolCapabilityCacheProvider" 1 "${inventory_capability_main_targets[@]}"
check_exact_owner_count "notice protocol single InventorySelectionService owner" "InventorySelectionService" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "notice protocol single LibomvInventoryPermissionMapping owner" "LibomvInventoryPermissionMapping" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "notice protocol single ProtocolSimulatorCircuitClient owner" "ProtocolSimulatorCircuitClient" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "notice protocol single LibomvNoticePacketCodec owner" "LibomvNoticePacketCodec" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "notice protocol single ProtocolNoticeCircuitSource owner" "ProtocolNoticeCircuitSource" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "avatar readiness single AvatarPort owner" "AvatarPort" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "avatar readiness single AvatarReadinessService owner" "AvatarReadinessService" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "avatar readiness single CurrentOutfitVersionSource owner" "CurrentOutfitVersionSource" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "avatar readiness single LibomvAvatarAdapter owner" "LibomvAvatarAdapter" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "avatar readiness single ProtocolAvatarRuntime owner" "ProtocolAvatarRuntime" 1 "${architecture_owner_targets[@]}"
check_exact_owner_count "avatar readiness single ProtocolAvatarAppearanceSource owner" "ProtocolAvatarAppearanceSource" 1 "${architecture_owner_targets[@]}"

check_no_hits \
    "login compliance viewer identity provider outside protocol module" \
    "$VIEWER_IDENTITY_PROVIDER_PATTERN" \
    "${viewer_identity_provider_forbidden_targets[@]}"

if [[ "${#notice_time_targets[@]}" -gt 0 ]]; then
    check_no_hits \
        "login compliance NoticeComplianceService direct system time" \
        "$NOTICE_TIME_SOURCE_PATTERN" \
        "${notice_time_targets[@]}"
else
    echo "PASS: login compliance NoticeComplianceService direct system time owner deleted"
fi

check_no_hits \
    "login compliance raw report key leakage" \
    "$REPORT_SECRET_KEY_PATTERN" \
    "${report_secret_key_targets[@]}"

check_no_hits \
    "login compliance direct UUID target UX" \
    "$GROUP_TARGET_UUID_PATTERN" \
    "${cli_command_targets[@]}"

check_no_hits \
    "login compliance CLI-owned cap literals" \
    "$NOTICE_CAP_LITERAL_PATTERN" \
    "${notice_cap_forbidden_targets[@]}"

check_pattern_matches \
    "self-test core forbidden dependency pattern" \
    "$CORE_FORBIDDEN_PATTERN" \
    'implementation(project(":gem-protocol-libomv"))'

check_pattern_matches \
    "self-test raw libomv pattern" \
    "$RAW_LIBOMV_PATTERN" \
    'libomv.GroupManager'

check_pattern_matches \
    "self-test private/reference pattern" \
    "$PRIVATE_REFERENCE_PATTERN" \
    '../private/reference/source'

check_pattern_matches \
    "self-test forbidden platform/logging pattern" \
    "$FORBIDDEN_PLATFORM_PATTERN" \
    'println("debug")'

check_pattern_matches \
    "self-test CLI command report write pattern" \
    "$CLI_COMMAND_REPORT_WRITE_PATTERN" \
    'Files.writeString(path, value)'

check_public_source_briefing_label_identifier_pattern

check_pattern_matches \
    "self-test protocol boundary direct OkHttp pattern" \
    "$PROTOCOL_BOUNDARY_OKHTTP_PATTERN" \
    "import okhttp3.${OK_HTTP_CLIENT_SYMBOL}"

check_pattern_matches \
    "self-test protocol boundary runtime/transport direct-call pattern" \
    "$PROTOCOL_BOUNDARY_RUNTIME_PATTERN" \
    "${PROTOCOL_PREFIX}HttpClient.execute(request)"

check_pattern_matches \
    "self-test protocol boundary stale platform pattern" \
    "$FORBIDDEN_PLATFORM_PATTERN" \
    'org.apache.http.client.HttpClient'

check_pattern_matches \
    "self-test protocol boundary UI direct protocol pattern" \
    "$UI_DIRECT_PROTOCOL_PATTERN" \
    'ProtocolNoticeRuntime(clientSession)'

check_pattern_matches \
    "self-test protocol boundary UI direct vault pattern" \
    "$UI_DIRECT_VAULT_PATTERN" \
    'CredentialVault.resolve(handle)'

check_pattern_matches \
    "self-test protocol boundary UI direct notice pattern" \
    "$UI_DIRECT_NOTICE_PATTERN" \
    'targetSet.selectedGroups.forEach { noticePort.sendGroupNotice(session, it, draft, null) }'

check_pattern_matches \
    "self-test protocol boundary UI forbidden owner pattern" \
    "$UI_FORBIDDEN_OWNER_PATTERN" \
    'class ScreenHelpers'

check_pattern_matches \
    "self-test protocol boundary UI WebView pattern" \
    "$UI_WEBVIEW_PATTERN" \
    'android.webkit.WebView(context).loadUrl("styles.css")'

check_pattern_matches \
    "self-test protocol boundary UI staged code pattern" \
    "$UI_STAGED_PATTERN" \
    'error("not implemented")'

check_pattern_matches \
    "self-test protocol boundary UI visible label pattern" \
    "$UI_TEXT_CATALOGUE_PATTERN" \
    '"Send notices"'

check_pattern_matches \
    "self-test protocol boundary UI style constant pattern" \
    "$UI_STYLE_TOKEN_PATTERN" \
    'val gap = 12.dp'

check_pattern_matches \
    "self-test protocol boundary UI raw control pattern" \
    "$UI_DIRECT_CONTROL_PATTERN" \
    'OutlinedTextField(value = name, onValueChange = {})'

check_pattern_matches \
    "self-test session credential direct runtime pattern" \
    "$SESSION_PROTOCOL_DIRECT_RUNTIME_PATTERN" \
    'EventQueueGetClient()'

check_pattern_matches \
    "self-test session credential raw env pattern" \
    "$CREDENTIAL_ENV_READ_PATTERN" \
    'System.getenv("HOSTESS_SECRET")'

check_pattern_matches \
    "self-test session credential file route pattern" \
    "$CREDENTIAL_FILE_ROUTE_PATTERN" \
    '--credential-file'

check_pattern_matches \
    "self-test session credential unsupported secret store pattern" \
    "$UNSUPPORTED_SECRET_STORE_PATTERN" \
    'keychain lookup'

check_pattern_matches \
    "self-test session credential UI split-login pattern" \
    "$UI_REMEDIATION_SPLIT_LOGIN_PATTERN" \
    'LoginSavedAccountPanel(state)'

check_pattern_matches \
    "self-test session credential UI fake location pattern" \
    "$UI_REMEDIATION_FAKE_LOCATION_PATTERN" \
    'locationLabel = "London City"'

check_pattern_matches \
    "self-test session credential UI scaffold pattern" \
    "$UI_REMEDIATION_SCAFFOLD_REQUIRED_PATTERN" \
    'GemAppScaffold(content = {})'

check_pattern_matches \
    "self-test session credential UI custom icon pattern" \
    "$UI_REMEDIATION_CUSTOM_ICON_PATTERN" \
    'Canvas(modifier) { drawLine(color, start, end) }'

check_pattern_matches \
    "self-test theme UI preference adapter pattern" \
    "$THEME_UI_PREFERENCE_ADAPTER_PATTERN" \
    'implementation(project(":gem-preferences"))'

check_pattern_matches \
    "self-test theme vault theme storage pattern" \
    "$THEME_VAULT_STORAGE_PATTERN" \
    'ThemePreferenceService(FileThemePreferenceStore(Path.of("ui.properties")))'

check_pattern_matches \
    "self-test theme Haccu colour pattern" \
    "$THEME_HACCU_COLOUR_PATTERN" \
    'val accent = Color(0xFF8B0101)'

check_pattern_matches \
    "self-test theme visible label pattern" \
    "$THEME_VISIBLE_LABEL_PATTERN" \
    '"Grid Event Manager"'

check_pattern_matches \
    "self-test theme prototype runtime pattern" \
    "$THEME_PROTOTYPE_RUNTIME_PATTERN" \
    'android.webkit.WebView(context).loadDataWithBaseURL("index-multi", "<html", "text/html", "utf-8", null)'

check_pattern_matches \
    "self-test theme logo owner pattern" \
    "$THEME_LOGO_OWNER_PATTERN" \
    'fun GemBrandLogoIcon(modifier: Modifier = Modifier)'

check_pattern_matches \
    "self-test theme theme toggle owner pattern" \
    "$THEME_TOGGLE_OWNER_PATTERN" \
    'fun ThemeModeToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit)'

check_pattern_matches \
    "self-test theme palette provider pattern" \
    "$THEME_PALETTE_OWNER_PATTERN" \
    'object HaccuGemPaletteProvider : GemPaletteProvider'

check_pattern_matches \
    "self-test theme Android label pattern" \
    "$THEME_ANDROID_LABEL_PATTERN" \
    'manifestPlaceholders["appLabel"] = "Grid Event Manager"'

check_pattern_matches \
    "self-test theme desktop vendor pattern" \
    "$THEME_DESKTOP_VENDOR_PATTERN" \
    'vendor = "ANVLL"'

check_pattern_matches \
    "self-test theme root project label pattern" \
    "$THEME_ROOT_PROJECT_LABEL_PATTERN" \
    'manifestPlaceholders["appLabel"] = rootProject.name.replaceFirstChar { it.titlecase() }'

check_pattern_matches \
    "self-test vault forbidden vault dependency pattern" \
    "$VAULT_FORBIDDEN_DEPENDENCY_PATTERN" \
    'implementation("com.example:java-keyring:1.0")'

check_pattern_matches \
    "self-test vault stale credential route pattern" \
    "$VAULT_STALE_CREDENTIAL_ROUTE_PATTERN" \
    'VaultUnlock asks for passphrase'

check_pattern_matches \
    "self-test vault AndroidX crypto pattern" \
    "$VAULT_ANDROIDX_CRYPTO_PATTERN" \
    'EncryptedSharedPreferences.create(...)'

check_pattern_matches \
    "self-test vault raw key exposure pattern" \
    "$VAULT_RAW_KEY_EXPOSURE_PATTERN" \
    'fun exportKey(): SecretKey = key'

check_pattern_matches \
    "self-test vault app env resolver pattern" \
    "$VAULT_APP_ENV_RESOLVER_PATTERN" \
    'EnvironmentLoginSecretResolver()'

check_pattern_matches \
    "self-test KMP migration common forbidden platform API pattern" \
    "$KMP_COMMON_FORBIDDEN_PLATFORM_PATTERN" \
    'java.util.UUID.randomUUID()'

check_pattern_matches \
    "self-test KMP migration parallel path pattern" \
    "$KMP_PARALLEL_PATH_PATTERN" \
    'class AndroidProtocolLibomvModule'

check_pattern_matches \
    "self-test Gem stale identity pattern catches old package root" \
    "$GEM_STALE_PUBLIC_IDENTITY_PATTERN" \
    "package org.${LEGACY_STALE_LOWER_PRODUCT}.core"

check_pattern_matches \
    "self-test Gem stale identity pattern catches Hostess source owner" \
    "$GEM_STALE_PUBLIC_IDENTITY_PATTERN" \
    'class HostessApp'

check_pattern_matches \
    "self-test Gem stale identity pattern catches old Gradle project" \
    "$GEM_STALE_PUBLIC_IDENTITY_PATTERN" \
    'implementation(project(":hostess-core"))'

check_pattern_matches \
    "self-test Gem stale identity pattern catches old boundary task" \
    "$GEM_STALE_PUBLIC_IDENTITY_PATTERN" \
    './gradlew checkHostessBoundaries'

check_pattern_matches \
    "self-test Gem stale identity pattern catches old UI tag" \
    "$GEM_STALE_PUBLIC_IDENTITY_PATTERN" \
    'data-hostess-app'

check_pattern_matches \
    "self-test Gem stale identity pattern catches old simulator thread" \
    "$GEM_STALE_PUBLIC_IDENTITY_PATTERN" \
    'hostess-simulator-session'

check_pattern_matches \
    "self-test Gem stale identity pattern catches old deb artifact" \
    "$GEM_STALE_PUBLIC_IDENTITY_PATTERN" \
    'hostess_0.1.10_amd64.deb'

check_pattern_matches \
    "self-test KMP migration platform API confinement pattern" \
    "$KMP_PLATFORM_API_PATTERN" \
    'import java.security.MessageDigest'

check_pattern_matches \
    "self-test login compliance generic owner pattern" \
    "$ARCHITECTURE_GENERIC_OWNER_PATTERN" \
    'src/main/kotlin/org/gem/core/LoginCompliance.kt'

check_pattern_matches \
    "self-test login compliance old login overload pattern" \
    "$SESSION_LOGIN_OVERLOAD_PATTERN" \
    'fun login(request: LoginRequest): SessionLoginResult'

check_pattern_matches \
    "self-test login compliance one-argument login call pattern" \
    "$SESSION_LOGIN_ONE_ARG_PATTERN" \
    'runtime.sessionService.login(request)'

check_pattern_matches \
    "self-test login compliance old notice route pattern" \
    "$NOTICE_DISPATCH_OLD_CALL_PATTERN" \
    'runtime.noticeDispatchService.dispatch(session, draft)'

check_pattern_matches \
    "self-test login compliance direct group notice send pattern" \
    "$NOTICE_DISPATCH_DIRECT_GROUP_SEND_PATTERN" \
    'runtime.noticePort.sendGroupNotice(session, group, draft, null)'

check_pattern_matches \
    "self-test login compliance viewer provider placement pattern" \
    "$VIEWER_IDENTITY_PROVIDER_PATTERN" \
    'GemViewerIdentityProvider'

check_pattern_matches \
    "self-test login compliance spoofed channel pattern" \
    "$VIEWER_IDENTITY_SPOOFED_CHANNEL_PATTERN" \
    'channel = "Firestorm"'

check_pattern_matches \
    "self-test login compliance notice time pattern" \
    "$NOTICE_TIME_SOURCE_PATTERN" \
    'Instant.now()'

check_pattern_matches \
    "self-test login compliance raw report key pattern" \
    "$REPORT_SECRET_KEY_PATTERN" \
    '"seedCapability" to identity.seedCapability'

check_pattern_matches \
    "self-test login compliance UUID target UX pattern" \
    "$GROUP_TARGET_UUID_PATTERN" \
    '--group-uuid'

check_pattern_matches \
    "self-test login compliance CLI cap literal pattern" \
    "$NOTICE_CAP_LITERAL_PATTERN" \
    'if (count > 4_500) return blocked'

check_pattern_matches \
    "self-test login package old LLSD login route pattern" \
    "$LOGIN_PACKAGE_OLD_LLSD_PATTERN" \
    'body = ProtocolHttpBody.TextBody(loginBody(secret), "application/llsd+xml")'

check_pattern_matches \
    "self-test login package direct package owner pattern" \
    "$LOGIN_PACKAGE_DIRECT_OWNER_PATTERN" \
    'LoginPackageBuilder().build(secret, viewer, machine)'

check_pattern_matches \
    "self-test login package stale login field pattern" \
    "$LOGIN_PACKAGE_STALE_FIELD_PATTERN" \
    'field("platform_string", value)'

check_pattern_matches \
    "self-test attachment cleanup stale create upload pattern" \
    "$ATTACHMENT_STALE_CREATE_UPLOAD_PATTERN" \
    'body = ProtocolHttpBody.BinaryUploadBody(bytes, "application/octet-stream")'

check_pattern_matches \
    "self-test attachment cleanup forbidden owner file pattern" \
    "$ATTACHMENT_FORBIDDEN_OWNER_FILE_PATTERN" \
    'src/main/kotlin/org/gem/core/AttachmentUploadManager.kt'

check_pattern_matches \
    "self-test attachment cleanup forbidden owner declaration pattern" \
    "$ATTACHMENT_FORBIDDEN_OWNER_DECL_PATTERN" \
    'class LandmarkAttachmentService'

check_pattern_matches \
    "self-test attachment cleanup implicit fake default pattern" \
    "$LIVE_PROOF_IMPLICIT_FAKE_PATTERN" \
    'CommandMode.parse(null)'

check_pattern_matches \
    "self-test attachment cleanup production local HTTP pattern" \
    "$LIVE_PROOF_LOCAL_HTTP_PATTERN" \
    'fun isLocalTestServer() = host == "127.0.0.1"'

check_pattern_matches \
    "self-test attachment cleanup Android probe hook pattern" \
    "$ANDROID_PROBE_INJECTION_STATUS_PATTERN" \
    'class AndroidCompatibilityProbe internal constructor(private val protocolLoadProbe: () -> State)'

check_pattern_matches \
    "self-test attachment cleanup generated packet commonMain pattern" \
    "$PACKET_GENERATION_COMMONMAIN_PATTERN" \
    'generated/sources/libomvPackets/kotlin/commonMain'

check_pattern_matches \
    "self-test attachment cleanup production generated packet import pattern" \
    "$PACKET_GENERATION_PRODUCTION_IMPORT_PATTERN" \
    'import libomv.packets.AssetUploadRequestPacket'

check_pattern_matches \
    "self-test inventory capability forbidden owner pattern" \
    "$INVENTORY_CAPABILITY_FORBIDDEN_OWNER_PATTERN" \
    'class InventoryBrowserService'

check_pattern_matches \
    "self-test inventory capability old EventQueue seed pattern" \
    "$INVENTORY_CAPABILITY_EVENT_QUEUE_SEED_PATTERN" \
    'private fun seedBody(requested: Set<String>) = requested.joinToString()'

check_pattern_matches \
    "self-test inventory capability CLI direct protocol pattern" \
    "$INVENTORY_CAPABILITY_CLI_DIRECT_PROTOCOL_PATTERN" \
    'ProtocolInventoryRuntime().listItems(session)'

check_pattern_matches \
    "self-test inventory capability CLI raw capability label pattern" \
    "$INVENTORY_CAPABILITY_CLI_RAW_CAPABILITY_PATTERN" \
    '"FetchInventoryDescendents2" to capabilityUrl'

check_pattern_matches \
    "self-test notice protocol stale bounded circuit owner pattern" \
    "$SIMULATOR_CIRCUIT_STALE_OWNER_PATTERN" \
    'class BoundedSimulatorCircuitClient'

check_pattern_matches \
    "self-test notice protocol stale CLI live send pattern" \
    "$LIVE_NOTICE_PROOF_STALE_CLI_SEND_PATTERN" \
    'sessionProvider = { fakeSession(active = false) }'

check_pattern_matches \
    "self-test notice protocol stale full proof pattern" \
    "$LIVE_NOTICE_PROOF_STALE_FULL_PROOF_PATTERN" \
    'private fun runBulkProof() = "bulk-notice"'

check_pattern_matches \
    "self-test notice protocol stale notice owner pattern" \
    "$NOTICE_PROTOCOL_STALE_OWNER_PATTERN" \
    'class NoticeSender { fun send() = GroupNoticeAdd }'

check_pattern_matches \
    "self-test notice protocol direct CLI app notice pattern" \
    "$NOTICE_PROTOCOL_DIRECT_APP_CALL_PATTERN" \
    'ProtocolNoticeCircuitSource(transport).send(packet)'

check_pattern_matches \
    "self-test simulator archive duplicate simulator exchange pattern" \
    "$SIMULATOR_EXCHANGE_DUPLICATE_PATTERN" \
    'class ExtraSimulatorSocket { val socket = DatagramSocket() }'

check_pattern_matches \
    "self-test simulator archive direct archive pattern" \
    "$NOTICE_ARCHIVE_DIRECT_PATTERN" \
    'ProtocolGroupNoticeArchiveSource(client).read(group)'

check_pattern_matches \
    "self-test simulator archive fake live pass pattern" \
    "$FAKE_LIVE_PROOF_PASS_PATTERN" \
    'CommandMode.FAKE returns ProofReportStatus.PASSED'

check_pattern_matches \
    "self-test incident group mutation pattern" \
    "$GROUP_MEMBERSHIP_MUTATION_PATTERN" \
    'EjectGroupMemberRequestPacket()'

check_pattern_matches \
    "self-test Android probe forbidden runtime route pattern" \
    "$ANDROID_PROBE_FORBIDDEN_RUNTIME_PATTERN" \
    'val resolver = EnvironmentLoginSecretResolver()'

check_pattern_matches \
    "self-test notice totals stale notice compliance pattern" \
    "$NOTICE_TOTALS_STALE_COMPLIANCE_PATTERN" \
    'NoticeComplianceService; NoticeSubmissionLedgerPort; noticeSubmissionProjectionStatus; noticeLedgerConfigured'

check_pattern_matches \
    "self-test avatar readiness classic baking pattern" \
    "$AVATAR_CLASSIC_BAKING_PATTERN" \
    'client.Appearance.RequestSetAppearance(true); AgentSetAppearance'

check_pattern_matches \
    "self-test avatar readiness forbidden avatar owner pattern" \
    "$AVATAR_FORBIDDEN_OWNER_PATTERN" \
    'class AvatarManager'

check_pattern_matches \
    "self-test avatar readiness direct avatar seed pattern" \
    "$AVATAR_DIRECT_CAPABILITY_SEED_PATTERN" \
    'identity.seedCapability'

check_pattern_matches \
    "self-test avatar readiness CLI direct avatar pattern" \
    "$AVATAR_CLI_DIRECT_PROTOCOL_PATTERN" \
    'ProtocolAvatarRuntime(clientSession).ensureReady(session)'

check_pattern_matches \
    "self-test avatar readiness stale full-proof simulator gate pattern" \
    "$AVATAR_STALE_FULL_PROOF_SIMULATOR_GATE_PATTERN" \
    'if (simulatorPresence(session).passed) sendNotice()'

check_pattern_matches \
    "self-test avatar readiness extra simulator exchange implementation pattern" \
    "$AVATAR_EXTRA_SIMULATOR_EXCHANGE_IMPL_PATTERN" \
    ') : SimulatorPacketExchange {'

check_pattern_matches \
    "self-test simulator session direct receive pattern" \
    "$SIMULATOR_SESSION_FORBIDDEN_FACADE_RECEIVE_PATTERN" \
    'currentExchange.receive(endpoint, timeoutMillis)'

check_pattern_matches \
    "self-test simulator session dead circuit client pattern" \
    "$SIMULATOR_SESSION_DEAD_CIRCUIT_CLIENT_PATTERN" \
    'private val pendingNoticeArchiveReplies = mutableListOf<Packet>()'

check_pattern_matches \
    "self-test simulator session duplicate session owner pattern" \
    "$SIMULATOR_SESSION_DUPLICATE_GATEWAY_PATTERN" \
    'class SimulatorSessionManager'

check_pattern_matches \
    "self-test simulator session fake live route pattern" \
    "$SIMULATOR_SESSION_FORBIDDEN_FAKE_LIVE_PATTERN" \
    'CommandMode.FAKE returns ProofReportStatus.PASSED'

if [[ "$failures" -ne 0 ]]; then
    exit 1
fi

echo "Gem boundary checks passed."
