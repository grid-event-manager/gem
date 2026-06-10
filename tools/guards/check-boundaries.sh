#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

failures=0

RAW_LIBOMV_PATTERN='(^|[^[:alnum:]_.])libomv\.'
CORE_FORBIDDEN_PATTERN="hostess-protocol-libomv|org\.hostess\.protocol\.libomv|:apps:|:tools:cli|reference/|\.\./private|$RAW_LIBOMV_PATTERN"
PRIVATE_REFERENCE_PATTERN='reference/|\.\./private'
FORBIDDEN_PLATFORM_PATTERN='TrustAll|ALLOW_ALL|HostnameVerifier|X509TrustManager|sslSocketFactory|org\.apache\.http|sun\.security|java\.awt|javax\.swing|METAbolt|WinForms|printStackTrace\(|println\('
CLI_COMMAND_REPORT_WRITE_PATTERN='File\(|Path\.of\(|Files\.|writeText\(|appendText\('
OK_HTTP_CLIENT_SYMBOL='OkHttp'"Client"
PROTOCOL_PREFIX='Protocol'
RUNTIME_SUFFIX='Runtime'
TRACK_B_OKHTTP_PATTERN="(^|[^[:alnum:]_.])okhttp3\.|(^|[^[:alnum:]_.])${OK_HTTP_CLIENT_SYMBOL}([^[:alnum:]_]|$)"
TRACK_B_RUNTIME_PATTERN="(^|[^[:alnum:]_.])(${PROTOCOL_PREFIX}Login${RUNTIME_SUFFIX}|${PROTOCOL_PREFIX}Group${RUNTIME_SUFFIX}|${PROTOCOL_PREFIX}Inventory${RUNTIME_SUFFIX}|${PROTOCOL_PREFIX}Notice${RUNTIME_SUFFIX}|${PROTOCOL_PREFIX}HttpClient)([^[:alnum:]_]|$)"
TRACK_B_UI_DIRECT_PROTOCOL_PATTERN='Libomv|Protocol[A-Za-z]+Runtime|ProtocolHttpClient|EventQueueGetClient|SimulatorPacketExchange'
TRACK_B_UI_DIRECT_VAULT_PATTERN='CredentialVault|VaultAccessService|AndroidKeystore|LocalUserFileVaultKeySource|CredentialManager|Keychain|SecretService|passphrase|TOTP|authenticator'
TRACK_B_UI_DIRECT_NOTICE_PATTERN='sendGroupNotice|NoticePort|selectedGroups.*(forEach|map|for \()'
TRACK_B_UI_FORBIDDEN_OWNER_PATTERN='(^|[^[:alnum:]_])(UiManager|StyleUtils|CommonUi|ScreenHelpers|CredentialHelper|VaultManager|InventoryBrowserService|NoticeSender|BulkSender)([^[:alnum:]_]|$)'
TRACK_B_UI_WEBVIEW_PATTERN='WebView|android\.webkit|<html|styles\.css|prototype\.js'
TRACK_B_UI_STAGED_PATTERN='STAGED_ENTRYPOINT|TODO|NotImplemented|UnsupportedOperationException|error\("not implemented"\)'
TRACK_B_UI_TEXT_PATTERN='"(Hostess|User Name|Password|Show|Hide|Login|Add new login|Add new login\.\.\.|Save and login|Settings|Add new account|Save new account|Delete account|Delete|Send notices|Online|Offline|Inventory|Landmarks|Textures|Add all|Add Groups)"'
TRACK_B_UI_STYLE_PATTERN='#[0-9A-Fa-f]{6}|Color\(|[0-9]+\.dp'
TRACK_C_RUNTIME_PATTERN='EnvironmentLoginSecretResolver|ProtocolSimulatorCircuitClient|AgentDataUpdateRequestTransport|EventQueueGetClient'
TRACK_A_FORBIDDEN_DEP_PATTERN='java-keyring|secret-service|multiplatform-settings|KVault|SecureVault|Kassaforte|tink|BouncyCastle|kotlinx\.serialization|protobuf|cbor|EncryptedSharedPreferences|MasterKey|EncryptedFile'
TRACK_A_STALE_CREDENTIAL_ROUTE_PATTERN='VaultUnlock|passphrase|TOTP|authenticator|platform_deferred|native-store required|VM fallback|plaintext fallback|CredentialManager|PasswordHelper|LoginUtils|VaultManager|VaultHelper|VaultUtils|CommonVault'
TRACK_A_ANDROIDX_CRYPTO_PATTERN='EncryptedSharedPreferences|EncryptedFile|MasterKey|androidx\.security'
TRACK_A_RAW_KEY_EXPOSURE_PATTERN='ByteArray.*key|key.*ByteArray|SecretKey'
TRACK_A_APP_ENV_RESOLVER_PATTERN='EnvironmentLoginSecretResolver'
TRACK_H_STALE_CIRCUIT_OWNER_PATTERN='BoundedSimulatorCircuit(Client|Sender)'
TRACK_H_STALE_CLI_LIVE_SEND_PATTERN='sessionProvider|fakeSession\(active = false\)|send-notice --mode live'
TRACK_H_STALE_FULL_PROOF_PATTERN='sendPlainNotice|runAttachmentProof|runBulkProof|boundedBulkTargetSet|plain-notice|existing-attachment-notice|bulk-notice|existingAttachmentId|--existing-attachment-id|bulkLimit|bulkDelayMs'
TRACK_H_STALE_NOTICE_PATTERN='(^|[^[:alnum:]_])(GroupNoticeAdd|NoticeSender|BulkSender)([^[:alnum:]_]|$)'
TRACK_H_DIRECT_APP_NOTICE_PATTERN='(^|[^[:alnum:]_])(ProtocolNoticeRuntime|ProtocolNoticeCircuitSource|LibomvNoticePacketCodec|SimulatorPacketExchange)([^[:alnum:]_]|$)'
TRACK_HS_DUPLICATE_SIMULATOR_EXCHANGE_PATTERN='SimulatorPacketReceiver|UdpSimulatorDatagramReceiver|class[[:space:]]+.*Presence.*Socket|class[[:space:]]+.*Simulator.*Socket|DatagramSocket\('
TRACK_HS_DIRECT_ARCHIVE_PATTERN='(^|[^[:alnum:]_])(ProtocolGroupNoticeArchiveSource|requestGroupNoticeArchive|GroupNoticesListRequest|GroupNoticesListReply)([^[:alnum:]_]|$)'
TRACK_HS_FAKE_LIVE_PASS_PATTERN='fake mode.*passed|CommandMode\.FAKE[^;]*ProofReportStatus\.PASSED|ProofReportStatus\.PASSED[^;]*CommandMode\.FAKE'
TRACK_HS_FULL_PROOF_ARCHIVE_REQUIRED_PATTERN='if[[:space:]]*\(!noticeArchive\(session,[[:space:]]*targetSet\)\)'
TRACK_INCIDENT_GROUP_MUTATION_PATTERN='EjectGroupMemberRequest|LeaveGroupRequest|GroupRoleChanges|GroupRoleUpdate|InviteGroupRequest|RequestBanAction|SetGroupAcceptNotices'
TRACK_ANDROID_PROBE_FORBIDDEN_RUNTIME_PATTERN='EnvironmentLoginSecretResolver|AgentDataUpdateRequestTransport|EventQueueGetClient|ProtocolLoginRuntime|ProtocolGroupRuntime|ProtocolInventoryRuntime|ProtocolNoticeRuntime|SimulatorPacketExchange'
TRACK_I_STALE_NOTICE_COMPLIANCE_PATTERN='NoticeCompliance(Service|Request|Decision|Receipt|Policy|Clock|LedgerResult|Ledgers?)|DefaultNoticeComplianceClock|NoticeSubmission(Count|Projection|LedgerSnapshot|LedgerPort)|NoticeLedgerDay|noticeSubmissionProjectionStatus|noticeSubmissionsProjected|noticeSubmissionLedgerGroupCount|noticeSubmissionLedgerMaxGroupTotal|noticeSubmissionPerGroupHardCap|noticeLedgerConfigured|notice_submission_cap_exceeded|ledger_snapshot_unavailable|ledger_reserve_failed|ledger_record_failed|notice_ledger_unavailable|NoticeRecipientEstimate|NoticeRecipientCount|NoticeRecipientEstimateSource|recipientDeliveryProjected|recipientDeliveryLedgerTotal|recipientDeliveryHardCap|recipientProjectionStatus|recipient_count_|recipient_delivery_|NoticeDeliveryCount|NoticeDeliveryDay|NoticeDeliveryProjection|NoticeDeliveryLedgerSnapshot|NoticeComplianceLedgerPort|HOSTESS_OWKS_COUNT|HOSTESS_MINX_COUNT'
TRACK_J_CLASSIC_BAKING_PATTERN='AgentSetAppearance|UploadBakedTexture|CreateBakes|DownloadWearables|WearOutfit|RequestSetAppearance'
TRACK_J_FORBIDDEN_AVATAR_OWNER_PATTERN='(^|[^[:alnum:]_])(AvatarManager|AppearanceHelper|ViewerUtils)([^[:alnum:]_]|$)'
TRACK_J_DIRECT_AVATAR_SEED_PATTERN='seedCapability'
TRACK_J_CLI_DIRECT_AVATAR_PATTERN='(^|[^[:alnum:]_])(LibomvAvatarAdapter|ProtocolAvatarRuntime|ProtocolAvatarAppearanceSource)([^[:alnum:]_]|$)'
TRACK_J_STALE_FULL_PROOF_SIMULATOR_GATE_PATTERN='simulatorPresence\(session\)|LiveProofSimulatorPresenceVerifier'
TRACK_J_EXTRA_SIMULATOR_EXCHANGE_IMPL_PATTERN='\)[[:space:]]*:[[:space:]]*SimulatorPacketExchange|^[[:space:]]*(class|object)[^=]*:[[:space:]]*SimulatorPacketExchange'
TRACK_C_ENV_PATTERN='System(::|\.)getenv'
TRACK_C_FILE_ROUTE_PATTERN='credential-file'
TRACK_C_UNSUPPORTED_SECRET_PATTERN='keychain|Keychain|KeyStore|plaintext|plain-text|plain text'
TRACK_C_UI_SPLIT_LOGIN_PATTERN='LoginSavedAccountPanel|AddLoginPanel|addLoginExpanded|newUsernameDraft|newPasswordDraft|saveAndLoginEnabled'
TRACK_C_UI_FAKE_LOCATION_PATTERN='startLocation\.orEmpty|SavedAccountProfile\.startLocation|London City|Welcome Area|locationLabel = "[^"]+"'
TRACK_C_UI_SCAFFOLD_REQUIRED_PATTERN='HostessAppScaffold'
TRACK_C_UI_CUSTOM_ICON_PATTERN='Canvas|drawLine|MenuBarCount|BackIconMidpoint|foundation\.Canvas'
HS002_TRACK_D_UI_PREFERENCE_ADAPTER_PATTERN='org\.hostess\.preferences|:hostess-preferences'
HS002_TRACK_D_VAULT_THEME_STORAGE_PATTERN='ThemePreference|themePreference|ui\.properties|preferences'
HS002_TRACK_D_HACCU_COLOUR_PATTERN='#[0-9A-Fa-f]{6}|Color\(0x'
HS002_TRACK_D_VISIBLE_LABEL_PATTERN='"(Ella Hostess|Light|Dark|Theme|Theme preference unavailable|Theme preference could not be saved)"'
HS002_TRACK_D_PROTOTYPE_RUNTIME_PATTERN='WebView|android\.webkit|index-multi|<html|styles\.css|loadDataWithBaseURL'
HS002_TRACK_D_LOGO_OWNER_PATTERN='fun[[:space:]]+HostessBrandLogoIcon'
HS002_TRACK_D_TOGGLE_OWNER_PATTERN='fun[[:space:]]+ThemeModeToggle'
HS002_TRACK_D_PALETTE_OWNER_PATTERN='object[[:space:]]+HaccuHostessPaletteProvider'
HS002_TRACK_D_ANDROID_LABEL_PATTERN='manifestPlaceholders\["appLabel"\][[:space:]]*=[[:space:]]*"Ella Hostess"'
HS002_TRACK_D_DESKTOP_VENDOR_PATTERN='vendor[[:space:]]*=[[:space:]]*"Ella Hostess"'
HS002_TRACK_D_ROOT_PROJECT_LABEL_PATTERN='rootProject\.name\.replaceFirstChar'
TRACK_D_GENERIC_OWNER_PATTERN='(^|/)(LoginCompliance|NoticeCompliance|.*(Manager|Helper|Utils|Common))\.kt$'
TRACK_D_SESSION_LOGIN_OVERLOAD_PATTERN='fun[[:space:]]+login\([[:space:]]*request:[[:space:]]*LoginRequest[[:space:]]*\)'
TRACK_D_SESSION_LOGIN_ONE_ARG_PATTERN='sessionService\.login\([^,\n)]*\)'
TRACK_D_OLD_NOTICE_CALL_PATTERN='dispatch\([^\n]*session[^\n]*draft[^,\n]*\)'
TRACK_D_SEND_GROUP_NOTICE_CALL_PATTERN='\.[[:space:]]*sendGroupNotice\('
TRACK_D_VIEWER_PROVIDER_PATTERN='HostessViewerIdentityProvider'
TRACK_D_SPOOFED_CHANNEL_PATTERN='channel[[:space:]]*=[[:space:]]*"(METAbolt|Firestorm|Alchemy|Second Life Viewer|Linden)"'
TRACK_D_NOTICE_TIME_PATTERN='Instant\.now|LocalDate\.now|Clock\.system|System\.currentTimeMillis'
TRACK_D_RAW_REPORT_KEY_PATTERN='("(mac|id0|host_id|seedCapability|credentialHandle|ledgerPath)"[[:space:]]+to|put\("(mac|id0|host_id|seedCapability|credentialHandle|ledgerPath)")'
TRACK_D_UUID_TARGET_PATTERN='--group-id|--group-uuid|group uuid|uuid target'
TRACK_D_CLI_CAP_LITERAL_PATTERN='(^|[^[:alnum:]_])(4500|4_500|5000|5_000)([^[:alnum:]_]|$)'
TRACK_DS_OLD_LOGIN_LLSD_PATTERN='application/llsd\+xml|fun[[:space:]]+loginBody\('
TRACK_DS_DIRECT_OWNER_PATTERN='LoginPackage|SecondLifePasswordHash|HostessMachineIdentity|LoginPackageSerializer'
TRACK_DS_STALE_LOGIN_FIELD_PATTERN='platform_version|platform_string|host_id|token|extended_errors|max-agent-groups|viewer_digest|user_agent'
TRACK_E_STALE_ATTACHMENT_PATTERN='CreateLandmarkAttachment|UploadTextureAttachment|AttachmentPayloadHandle|LocalPosition|createLandmarkAttachment|uploadTextureAttachment|landmarkAssetBytes|AttachmentPayloadSource|InventoryUploadResult|beginTextureUpload|completeTextureUpload|landmarkRequest|textureRequest|createdLandmarkRequest|uploadTextureRequest|landmarkVenue|landmarkRegionId|landmarkLocalPosition|texturePayloadHandle|textureDigest|textureFileName|safeTextureFileName|BinaryUploadBody|application/octet-stream|upload_url|uploadUrl|attachmentPayloadHandle|attachmentSource|payloadHandle|AssetUploadRequest|AssetUploadComplete|CreateLandmarkForEvent'
TRACK_E_FORBIDDEN_OWNER_FILE_PATTERN='(LandmarkAttachmentService|TextureUploadService|AttachmentUploadManager|InventoryUtils|AttachmentHelpers|CommonAttachment|BulkSender)\.kt$'
TRACK_E_FORBIDDEN_OWNER_DECL_PATTERN='(^|[^[:alnum:]_])((data[[:space:]]+)?class|object|interface)[[:space:]]+(LandmarkAttachmentService|TextureUploadService|AttachmentUploadManager|InventoryUtils|AttachmentHelpers|CommonAttachment|BulkSender)([^[:alnum:]_]|$)'
TRACK_E_IMPLICIT_FAKE_PATTERN='null,[[:space:]]*"fake"|option\("mode"\)[[:space:]]*\?:[[:space:]]*"fake"|CommandMode\.parse\(null\)'
TRACK_E_LOCAL_HTTP_PATTERN='local test servers|isLocalTestServer|127\.0\.0\.1|localhost|::1'
TRACK_E_ANDROID_PROBE_PATTERN='AndroidCompatibilityProbe[[:space:]]+internal[[:space:]]+constructor|protocolLoadProbe|forbiddenApiScan[[:space:]]*=[[:space:]]*"external_guard_required"|external_guard_required'
TRACK_E_GENERATED_PACKET_COMMONMAIN_PATTERN='generated/sources/libomvPackets/kotlin/commonMain'
TRACK_E_PRODUCTION_PACKET_IMPORT_PATTERN='(^|[^[:alnum:]_.])libomv\.packets'
TRACK_G_FORBIDDEN_OWNER_PATTERN='(^|[^[:alnum:]_])(NotecardService|NotecardPort|ProtocolNotecardRuntime|LibomvNotecardAdapter|LibomvNotecardTextParser|NotecardAssetReader|BulkSender|NoticeSender|InventoryCataloguePort|InventoryLookupPort|InventoryBrowserService|ProtocolInventoryAssetSource)([^[:alnum:]_]|$)'
TRACK_G_EVENT_QUEUE_SEED_PATTERN='fun[[:space:]]+seed[[:space:]]*\(|seedBody|seedCapability'
TRACK_G_CLI_DIRECT_PROTOCOL_PATTERN='(^|[^[:alnum:]_])(ProtocolInventoryRuntime|ProtocolCurrentGroupsSource|ProtocolCapabilitySeedClient|EventQueueGetClient)([^[:alnum:]_]|$)'
TRACK_G_CLI_RAW_CAPABILITY_PATTERN='seedCapability|capabilityUrl|EventQueueGet|FetchInventory2|FetchInventoryDescendents2'
TRACK_F_COMMON_FORBIDDEN_PATTERN='java\.|javax\.|okhttp|android\.|System\.|MessageDigest|NetworkInterface|Datagram|ByteBuffer|UUID|Class\.forName|::class\.java'
TRACK_F_PARALLEL_PATH_PATTERN='hostess-core-kmp|hostess-protocol-android|AndroidProtocolLibomvModule|JvmProtocolLibomvModule|GroupReader|CurrentGroupsClient|LoginRuntimeAndroid|Manager|Utils|Helpers|Common'
TRACK_F_PLATFORM_API_PATTERN='okhttp3\.|OkHttpClient|System(::|\.)getenv|NetworkInterface|java\.net\.Datagram|Datagram(Packet|Socket)|javax\.xml|org\.xml\.sax|DocumentBuilderFactory|java\.util\.UUID|MessageDigest|java\.nio\.ByteBuffer'
TRACK_F_OWNER_DECLARATION_PREFIX='^[[:space:]]*(internal[[:space:]]+|private[[:space:]]+|public[[:space:]]+)?(open[[:space:]]+|sealed[[:space:]]+)?(data[[:space:]]+class|class|object|interface|fun[[:space:]]+interface|value[[:space:]]+class)[[:space:]]+'

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
        "hostess-core/src/main/kotlin" \
        "hostess-protocol-libomv/src/main/kotlin" \
        -type f 2>/dev/null || true)

    if [[ "${#matches[@]}" -eq 0 ]]; then
        echo "PASS: Track F old shared production roots absent"
    else
        echo "FAIL: Track F old shared production roots absent"
        printf '%s\n' "${matches[@]}"
        failures=1
    fi
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
    output="$(rg -n -- "${TRACK_F_OWNER_DECLARATION_PREFIX}${owner}([^[:alnum:]_]|$)" "$@" 2>&1)"
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
        "hostess-core/src/commonMain/kotlin/org/hostess/core/services/NoticeDispatchService.kt" \
        "hostess-core/src/main/kotlin/org/hostess/core/services/NoticeDispatchService.kt"

    if [[ "${#files[@]}" -eq 0 ]]; then
        echo "ERROR: Track I notice dispatch overload scan has no scan targets"
        failures=1
        return
    fi

    set +e
    local output
    output="$(perl -0ne 'while (/fun\s+dispatch\s*\((.*?)\)\s*:/sg) { print "$ARGV: dispatch overload keeps notice compliance argument\n" if $1 =~ /NoticeComplianceRequest|compliance\s*:/ }' "${files[@]}" 2>&1)"
    local status="$?"
    set -e

    if [[ "$status" -ne 0 ]]; then
        echo "ERROR: Track D notice dispatch overload scan failed"
        echo "$output"
        failures=1
    elif [[ -n "$output" ]]; then
        echo "FAIL: Track I notice dispatch overload forbids local notice compliance"
        echo "$output"
        failures=1
    else
        echo "PASS: Track I notice dispatch overload forbids local notice compliance"
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

core_targets=()
add_existing core_targets \
    "hostess-core/build.gradle.kts" \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main"

app_cli_targets=()
add_existing app_cli_targets \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

cli_command_targets=()
add_existing cli_command_targets \
    "tools/cli/src/main/kotlin/org/hostess/tools/cli/commands"

production_targets=()
add_existing production_targets \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle/libs.versions.toml" \
    "hostess-core/build.gradle.kts" \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/build.gradle.kts" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "hostess-ui/build.gradle.kts" \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/androidMain" \
    "tools/cli/build.gradle.kts" \
    "tools/cli/src/main" \
    "apps/desktop/build.gradle.kts" \
    "apps/desktop/src/main" \
    "apps/android/build.gradle.kts" \
    "apps/android/src/main"

track_f_all_source_targets=()
add_existing track_f_all_source_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/commonTest" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/jvmTest" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/androidTest" \
    "hostess-core/src/androidUnitTest" \
    "hostess-core/src/androidHostTest" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/commonTest" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/jvmTest" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/androidTest" \
    "hostess-protocol-libomv/src/androidUnitTest" \
    "hostess-protocol-libomv/src/androidHostTest" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "hostess-protocol-libomv/build/generated/sources/libomvPackets/kotlin/commonMain" \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/commonTest" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/jvmTest" \
    "hostess-ui/src/androidMain" \
    "hostess-ui/src/androidTest" \
    "tools/cli/src/main" \
    "tools/cli/src/test" \
    "apps/desktop/src/main" \
    "apps/desktop/src/test" \
    "apps/android/src/main" \
    "apps/android/src/test"

track_f_common_targets=()
add_existing track_f_common_targets \
    "hostess-core/src/commonMain" \
    "hostess-protocol-libomv/src/commonMain"

track_f_owner_targets=()
add_existing track_f_owner_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/androidMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_f_platform_api_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/transport/OkHttpProtocolHttpClient.kt) ;;
        hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/transport/UdpSimulatorDatagramSender.kt) ;;
        hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/runtime/JvmMd5DigestPort.kt) ;;
        hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/runtime/EnvironmentLoginSecretResolver.kt) ;;
        hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/runtime/DefaultHostessHardwareAddressSource.kt) ;;
        apps/desktop/src/main/kotlin/org/hostess/apps/desktop/DesktopVaultComposition.kt) ;;
        apps/desktop/src/main/kotlin/org/hostess/apps/desktop/DesktopPreferenceComposition.kt) ;;
        *) track_f_platform_api_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/androidMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

okhttp_forbidden_targets=()
add_existing okhttp_forbidden_targets \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle/libs.versions.toml" \
    "hostess-core/build.gradle.kts" \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/build.gradle.kts" \
    "tools/cli/build.gradle.kts" \
    "tools/cli/src/main" \
    "apps/desktop/build.gradle.kts" \
    "apps/desktop/src/main" \
    "apps/android/build.gradle.kts" \
    "apps/android/src/main"
for protocol_root in \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main"; do
    while IFS= read -r path; do
        okhttp_forbidden_targets+=("$path")
    done < <(find "$protocol_root" -type f ! -path '*/transport/*' 2>/dev/null || true)
done

track_b_runtime_forbidden_targets=()
add_existing track_b_runtime_forbidden_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_b_ui_targets=()
add_existing track_b_ui_targets \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/androidMain"

track_b_ui_text_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/HostessText.kt") ;;
        *) track_b_ui_text_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/androidMain" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f 2>/dev/null || true)
add_existing track_b_ui_text_forbidden_targets \
    "apps/desktop/build.gradle.kts" \
    "apps/android/build.gradle.kts"

track_b_ui_style_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/org/hostess/ui/design/"*) ;;
        *) track_b_ui_style_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-ui/src/commonMain/kotlin/org/hostess/ui" \
    "hostess-ui/src/jvmMain/kotlin/org/hostess/ui" \
    "hostess-ui/src/androidMain/kotlin/org/hostess/ui" \
    -type f -name '*.kt' 2>/dev/null || true)

track_c_ui_split_login_targets=()
add_existing track_c_ui_split_login_targets \
    "hostess-ui/src/commonMain"

track_c_ui_fake_location_targets=()
add_existing track_c_ui_fake_location_targets \
    "hostess-ui/src/commonMain" \
    "hostess-core/src/commonMain" \
    "hostess-protocol-libomv/src/commonMain"

track_c_ui_scaffold_required_targets=()
add_existing track_c_ui_scaffold_required_targets \
    "hostess-ui/src/commonMain/kotlin/org/hostess/ui/HostessApp.kt" \
    "hostess-ui/src/commonMain/kotlin/org/hostess/ui/components/HostessAppScaffold.kt"

track_c_ui_custom_icon_targets=()
add_existing track_c_ui_custom_icon_targets \
    "hostess-ui/src/commonMain/kotlin/org/hostess/ui/components/HostessIcons.kt"

hs002_track_d_ui_adapter_targets=()
add_existing hs002_track_d_ui_adapter_targets \
    "hostess-ui/build.gradle.kts" \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/androidMain"

hs002_track_d_vault_theme_targets=()
add_existing hs002_track_d_vault_theme_targets \
    "hostess-credential-vault/build.gradle.kts" \
    "hostess-credential-vault/src/commonMain" \
    "hostess-credential-vault/src/jvmMain" \
    "hostess-credential-vault/src/androidMain" \
    "hostess-credential-vault/src/jvmAndroidMain"

hs002_track_d_colour_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/org/hostess/ui/design/"*) ;;
        *) hs002_track_d_colour_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-ui/src/commonMain/kotlin/org/hostess/ui" \
    "hostess-ui/src/jvmMain/kotlin/org/hostess/ui" \
    "hostess-ui/src/androidMain/kotlin/org/hostess/ui" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

hs002_track_d_visible_label_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/HostessText.kt") ;;
        *) hs002_track_d_visible_label_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/androidMain" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

hs002_track_d_prototype_forbidden_targets=()
add_existing hs002_track_d_prototype_forbidden_targets \
    "hostess-ui/src/commonMain" \
    "hostess-ui/src/jvmMain" \
    "hostess-ui/src/androidMain" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

hs002_track_d_root_project_label_targets=()
add_existing hs002_track_d_root_project_label_targets \
    "apps/desktop/build.gradle.kts" \
    "apps/android/build.gradle.kts"

track_c_runtime_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        # Android no-UI load probe may mention protocol owner class names only.
        "apps/android/src/main/kotlin/org/hostess/apps/android/AndroidCompatibilityProbe.kt") ;;
        *) track_c_runtime_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

track_c_env_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/EnvironmentLoginSecretResolver.kt") ;;
        *"/DesktopVaultComposition.kt") ;;
        *"/DesktopPreferenceComposition.kt") ;;
        *) track_c_env_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f 2>/dev/null || true)

track_c_file_route_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/LiveProofInputs.kt") ;;
        *) track_c_file_route_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f 2>/dev/null || true)

track_d_main_roots=(
    "hostess-core"
    "hostess-protocol-libomv"
    "tools"
    "apps"
)

track_d_tools_apps_targets=()
add_existing track_d_tools_apps_targets \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_d_notice_dispatch_targets=()
add_existing track_d_notice_dispatch_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_d_viewer_provider_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        # D-08 Android no-UI load probe must class-load this protocol identity provider.
        "apps/android/src/main/kotlin/org/hostess/apps/android/AndroidCompatibilityProbe.kt") ;;
        *) track_d_viewer_provider_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

track_d_report_key_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/RedactedText.kt") ;;
        *) track_d_report_key_targets+=("$path") ;;
    esac
done < <(find "tools/cli/src/main/kotlin/org/hostess/tools/cli" -type f -name '*.kt' 2>/dev/null || true)

track_d_cap_forbidden_targets=()
add_existing track_d_cap_forbidden_targets \
    "tools/cli/src/main/kotlin/org/hostess/tools/cli/commands/SendNoticeCommand.kt" \
    "tools/cli/src/main/kotlin/org/hostess/tools/cli/commands/LiveProofRunner.kt"

track_ds_direct_owner_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        # DS Android no-UI load probe may mention Track DS owner class names only.
        "apps/android/src/main/kotlin/org/hostess/apps/android/AndroidCompatibilityProbe.kt") ;;
        *) track_ds_direct_owner_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

track_ds_login_package_targets=()
add_existing track_ds_login_package_targets \
    "hostess-protocol-libomv/src/commonMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "hostess-protocol-libomv/src/jvmMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "hostess-protocol-libomv/src/androidMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "hostess-protocol-libomv/src/main/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt"
for protocol_runtime_root in \
    "hostess-protocol-libomv/src/commonMain/kotlin/org/hostess/protocol/libomv/runtime" \
    "hostess-protocol-libomv/src/jvmMain/kotlin/org/hostess/protocol/libomv/runtime" \
    "hostess-protocol-libomv/src/androidMain/kotlin/org/hostess/protocol/libomv/runtime" \
    "hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/runtime" \
    "hostess-protocol-libomv/src/main/kotlin/org/hostess/protocol/libomv/runtime"; do
    while IFS= read -r path; do
        track_ds_login_package_targets+=("$path")
    done < <(find "$protocol_runtime_root" -maxdepth 1 -type f -name 'LoginPackage*.kt' 2>/dev/null || true)
done

track_d_session_service_targets=()
add_existing track_d_session_service_targets \
    "hostess-core/src/commonMain/kotlin/org/hostess/core/services/SessionService.kt" \
    "hostess-core/src/main/kotlin/org/hostess/core/services/SessionService.kt"

track_d_notice_time_targets=()
add_existing track_d_notice_time_targets \
    "hostess-core/src/commonMain/kotlin/org/hostess/core/services/NoticeComplianceService.kt" \
    "hostess-core/src/main/kotlin/org/hostess/core/services/NoticeComplianceService.kt"

track_ds_old_login_targets=()
add_existing track_ds_old_login_targets \
    "hostess-protocol-libomv/src/commonMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "hostess-protocol-libomv/src/jvmMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "hostess-protocol-libomv/src/androidMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt" \
    "hostess-protocol-libomv/src/main/kotlin/org/hostess/protocol/libomv/runtime/ProtocolLoginRuntime.kt"

track_e_kotlin_targets=()
add_existing track_e_kotlin_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/commonTest" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/jvmTest" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/androidTest" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/commonTest" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/jvmTest" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/androidTest" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "tools/cli/src/main" \
    "tools/cli/src/test" \
    "apps/desktop/src/main" \
    "apps/desktop/src/test" \
    "apps/android/src/main" \
    "apps/android/src/test" \
    "apps/android/src/androidTest"

track_e_fake_default_targets=()
add_existing track_e_fake_default_targets \
    "tools/cli/src/main" \
    "tools/cli/src/test"

track_e_local_http_targets=()
add_existing track_e_local_http_targets \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain"

track_e_android_probe_targets=()
add_existing track_e_android_probe_targets \
    "apps/android/src/main"

track_e_packet_generation_targets=()
add_existing track_e_packet_generation_targets \
    "hostess-protocol-libomv/build.gradle.kts"

track_e_production_packet_import_targets=()
add_existing track_e_production_packet_import_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_g_main_targets=()
add_existing track_g_main_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_g_event_queue_targets=()
add_existing track_g_event_queue_targets \
    "hostess-protocol-libomv/src/commonMain/kotlin/org/hostess/protocol/libomv/transport/EventQueueGetClient.kt"

track_g_cli_targets=()
add_existing track_g_cli_targets \
    "tools/cli/src/main"

track_h_cli_live_send_targets=()
add_existing track_h_cli_live_send_targets \
    "tools/cli/src/main" \
    "tools/cli/README.md"

track_h_full_proof_targets=()
add_existing track_h_full_proof_targets \
    "tools/cli/src/main" \
    "tools/cli/src/test"

track_h_notice_stale_targets=()
add_existing track_h_notice_stale_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_h_direct_app_notice_targets=()
while IFS= read -r path; do
    case "$path" in
        # H-08 Android no-UI load probe may mention Track H protocol owner class names only.
        "apps/android/src/main/kotlin/org/hostess/apps/android/AndroidCompatibilityProbe.kt") ;;
        *) track_h_direct_app_notice_targets+=("$path") ;;
    esac
done < <(find \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

track_hs_duplicate_exchange_targets=()
while IFS= read -r path; do
    case "$path" in
        "hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/transport/UdpSimulatorDatagramSender.kt") ;;
        *) track_hs_duplicate_exchange_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

track_hs_direct_archive_targets=()
add_existing track_hs_direct_archive_targets \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_hs_fake_live_targets=()
add_existing track_hs_fake_live_targets \
    "tools/cli/src/main"

track_hs_full_proof_targets=()
add_existing track_hs_full_proof_targets \
    "tools/cli/src/main/kotlin/org/hostess/tools/cli/commands/LiveNoticeSendProofRunner.kt"

track_incident_group_mutation_targets=()
add_existing track_incident_group_mutation_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_android_probe_targets=()
add_existing track_android_probe_targets \
    "apps/android/src/main/kotlin/org/hostess/apps/android/AndroidCompatibilityProbe.kt"

track_a_dependency_targets=()
add_existing track_a_dependency_targets \
    "gradle/libs.versions.toml" \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "hostess-core/build.gradle.kts" \
    "hostess-credential-vault/build.gradle.kts" \
    "hostess-protocol-libomv/build.gradle.kts" \
    "tools/cli/build.gradle.kts" \
    "apps/desktop/build.gradle.kts" \
    "apps/android/build.gradle.kts"

track_a_production_targets=()
add_existing track_a_production_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-credential-vault/src/commonMain" \
    "hostess-credential-vault/src/jvmMain" \
    "hostess-credential-vault/src/androidMain" \
    "hostess-credential-vault/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_a_raw_key_forbidden_targets=()
add_existing track_a_raw_key_forbidden_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_a_app_composition_targets=()
add_existing track_a_app_composition_targets \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_i_stale_notice_compliance_targets=()
add_existing track_i_stale_notice_compliance_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_i_public_doc_targets=()
add_existing track_i_public_doc_targets \
    "README.md" \
    "hostess-core/README.md" \
    "tools/cli/README.md" \
    "apps/desktop/README.md" \
    "apps/android/README.md"

track_j_main_targets=()
add_existing track_j_main_targets \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_j_avatar_source_targets=()
add_existing track_j_avatar_source_targets \
    "hostess-protocol-libomv/src/commonMain/kotlin/org/hostess/protocol/libomv/runtime/ProtocolAvatarAppearanceSource.kt"

track_j_cli_command_targets=()
add_existing track_j_cli_command_targets \
    "tools/cli/src/main/kotlin/org/hostess/tools/cli/commands"

track_j_full_proof_targets=()
add_existing track_j_full_proof_targets \
    "tools/cli/src/main/kotlin/org/hostess/tools/cli/commands/LiveNoticeSendProofRunner.kt"

track_j_simulator_exchange_impl_targets=()
while IFS= read -r path; do
    case "$path" in
        "hostess-protocol-libomv/src/commonMain/kotlin/org/hostess/protocol/libomv/transport/SimulatorPacketExchange.kt") ;;
        "hostess-protocol-libomv/src/jvmAndroidMain/kotlin/org/hostess/protocol/libomv/transport/UdpSimulatorDatagramSender.kt") ;;
        *) track_j_simulator_exchange_impl_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/commonMain" \
    "hostess-core/src/jvmMain" \
    "hostess-core/src/androidMain" \
    "hostess-core/src/jvmAndroidMain" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/src/commonMain" \
    "hostess-protocol-libomv/src/jvmMain" \
    "hostess-protocol-libomv/src/androidMain" \
    "hostess-protocol-libomv/src/jvmAndroidMain" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main" \
    -type f -name '*.kt' 2>/dev/null || true)

check_no_hits \
    "hostess-core forbidden dependencies" \
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
    "Track B direct OkHttp outside protocol transport" \
    "$TRACK_B_OKHTTP_PATTERN" \
    "${okhttp_forbidden_targets[@]}"

check_no_hits \
    "Track B runtime/transport direct calls outside protocol module" \
    "$TRACK_B_RUNTIME_PATTERN" \
    "${track_b_runtime_forbidden_targets[@]}"

check_path_exists \
    "Track B shared UI module exists" \
    "hostess-ui/src/commonMain"

check_no_hits \
    "Track B UI direct protocol/runtime path" \
    "$TRACK_B_UI_DIRECT_PROTOCOL_PATTERN" \
    "${track_b_ui_targets[@]}"

check_no_hits \
    "Track B UI direct vault/native credential path" \
    "$TRACK_B_UI_DIRECT_VAULT_PATTERN" \
    "${track_b_ui_targets[@]}"

check_no_hits \
    "Track B UI direct notice/group send path" \
    "$TRACK_B_UI_DIRECT_NOTICE_PATTERN" \
    "${track_b_ui_targets[@]}"

check_no_hits \
    "Track B UI forbidden generic owner names" \
    "$TRACK_B_UI_FORBIDDEN_OWNER_PATTERN" \
    "${track_b_ui_targets[@]}"

check_no_hits \
    "Track B UI WebView/HTML prototype route" \
    "$TRACK_B_UI_WEBVIEW_PATTERN" \
    "${track_b_ui_targets[@]}" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

check_no_hits \
    "Track B UI unfinished staged code" \
    "$TRACK_B_UI_STAGED_PATTERN" \
    "${track_b_ui_targets[@]}"

check_no_hits \
    "Track B UI visible labels centralized" \
    "$TRACK_B_UI_TEXT_PATTERN" \
    "${track_b_ui_text_forbidden_targets[@]}"

check_no_hits \
    "Track B UI style constants centralized" \
    "$TRACK_B_UI_STYLE_PATTERN" \
    "${track_b_ui_style_forbidden_targets[@]}"

check_no_hits \
    "Track C direct runtime/transport calls outside protocol module" \
    "$TRACK_C_RUNTIME_PATTERN" \
    "${track_c_runtime_forbidden_targets[@]}"

check_no_hits \
    "Track C raw env reads outside resolver" \
    "$TRACK_C_ENV_PATTERN" \
    "${track_c_env_forbidden_targets[@]}"

check_no_hits \
    "Track C unsupported file route outside blocking parser" \
    "$TRACK_C_FILE_ROUTE_PATTERN" \
    "${track_c_file_route_forbidden_targets[@]}"

check_no_hits \
    "Track C unsupported secret stores" \
    "$TRACK_C_UNSUPPORTED_SECRET_PATTERN" \
    "${production_targets[@]}"

check_no_hits \
    "Track C UI old split-login route" \
    "$TRACK_C_UI_SPLIT_LOGIN_PATTERN" \
    "${track_c_ui_split_login_targets[@]}"

check_no_hits \
    "Track C UI fake session location route" \
    "$TRACK_C_UI_FAKE_LOCATION_PATTERN" \
    "${track_c_ui_fake_location_targets[@]}"

check_required_hits \
    "Track C UI scaffold owner present" \
    "$TRACK_C_UI_SCAFFOLD_REQUIRED_PATTERN" \
    "${track_c_ui_scaffold_required_targets[@]}"

check_no_hits \
    "Track C UI custom icon route" \
    "$TRACK_C_UI_CUSTOM_ICON_PATTERN" \
    "${track_c_ui_custom_icon_targets[@]}"

check_no_hits \
    "HS002 Track D UI has no preference adapter dependency" \
    "$HS002_TRACK_D_UI_PREFERENCE_ADAPTER_PATTERN" \
    "${hs002_track_d_ui_adapter_targets[@]}"

check_no_hits \
    "HS002 Track D vault has no theme preference storage" \
    "$HS002_TRACK_D_VAULT_THEME_STORAGE_PATTERN" \
    "${hs002_track_d_vault_theme_targets[@]}"

check_no_hits \
    "HS002 Track D Haccu colours centralized" \
    "$HS002_TRACK_D_HACCU_COLOUR_PATTERN" \
    "${hs002_track_d_colour_forbidden_targets[@]}"

check_no_hits \
    "HS002 Track D visible labels centralized" \
    "$HS002_TRACK_D_VISIBLE_LABEL_PATTERN" \
    "${hs002_track_d_visible_label_forbidden_targets[@]}"

check_no_hits \
    "HS002 Track D prototype runtime not promoted" \
    "$HS002_TRACK_D_PROTOTYPE_RUNTIME_PATTERN" \
    "${hs002_track_d_prototype_forbidden_targets[@]}"

check_required_hits \
    "HS002 Track D brand logo owner present" \
    "$HS002_TRACK_D_LOGO_OWNER_PATTERN" \
    "hostess-ui/src/commonMain/kotlin/org/hostess/ui/components/HostessIcons.kt"

check_required_hits \
    "HS002 Track D theme toggle owner present" \
    "$HS002_TRACK_D_TOGGLE_OWNER_PATTERN" \
    "hostess-ui/src/commonMain/kotlin/org/hostess/ui/components/ThemeModeToggle.kt"

check_required_hits \
    "HS002 Track D palette provider present" \
    "$HS002_TRACK_D_PALETTE_OWNER_PATTERN" \
    "hostess-ui/src/commonMain/kotlin/org/hostess/ui/design/HaccuHostessPaletteProvider.kt"

check_required_hits \
    "HS002 Track D Android app label explicit" \
    "$HS002_TRACK_D_ANDROID_LABEL_PATTERN" \
    "apps/android/build.gradle.kts"

check_required_hits \
    "HS002 Track D desktop vendor explicit" \
    "$HS002_TRACK_D_DESKTOP_VENDOR_PATTERN" \
    "apps/desktop/build.gradle.kts"

check_no_hits \
    "HS002 Track D app labels do not derive from root project name" \
    "$HS002_TRACK_D_ROOT_PROJECT_LABEL_PATTERN" \
    "${hs002_track_d_root_project_label_targets[@]}"

check_no_hits \
    "Track A forbidden vault dependency acquisition" \
    "$TRACK_A_FORBIDDEN_DEP_PATTERN" \
    "${track_a_dependency_targets[@]}"

check_no_hits \
    "Track A stale credential unlock routes" \
    "$TRACK_A_STALE_CREDENTIAL_ROUTE_PATTERN" \
    "${track_a_production_targets[@]}"

check_no_hits \
    "Track A AndroidX crypto route absent" \
    "$TRACK_A_ANDROIDX_CRYPTO_PATTERN" \
    "${track_a_production_targets[@]}" \
    "${track_a_dependency_targets[@]}"

check_no_hits \
    "Track A raw key exposure outside vault module" \
    "$TRACK_A_RAW_KEY_EXPOSURE_PATTERN" \
    "${track_a_raw_key_forbidden_targets[@]}"

check_no_hits \
    "Track A app composition does not use env secret resolver" \
    "$TRACK_A_APP_ENV_RESOLVER_PATTERN" \
    "${track_a_app_composition_targets[@]}"

check_exact_owner_count "Track A single AndroidKeystoreVaultKeySource owner" "AndroidKeystoreVaultKeySource" 1 "${track_a_production_targets[@]}"
check_exact_owner_count "Track A single LocalUserFileVaultKeySource owner" "LocalUserFileVaultKeySource" 1 "${track_a_production_targets[@]}"
check_exact_owner_count "Track A single DesktopVaultPaths owner" "DesktopVaultPaths" 1 "${track_a_production_targets[@]}"
check_exact_owner_count "Track A single CredentialVaultLoginSecretResolver owner" "CredentialVaultLoginSecretResolver" 1 "${track_a_production_targets[@]}"

check_no_old_shared_roots

check_no_hits \
    "Track F commonMain forbidden platform APIs" \
    "$TRACK_F_COMMON_FORBIDDEN_PATTERN" \
    "${track_f_common_targets[@]}"

check_no_hits \
    "Track F no parallel KMP or generic owner paths" \
    "$TRACK_F_PARALLEL_PATH_PATTERN" \
    "${track_f_all_source_targets[@]}"

check_no_hits \
    "Track F platform APIs confined to platform adapters" \
    "$TRACK_F_PLATFORM_API_PATTERN" \
    "${track_f_platform_api_forbidden_targets[@]}"

check_exact_owner_count "Track F single HostessInstant owner" "HostessInstant" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single HostessDelay owner" "HostessDelay" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ClockPort owner" "ClockPort" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ProtocolLibomvModule owner" "ProtocolLibomvModule" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single LibomvPlatformAdapterBundle owner" "LibomvPlatformAdapterBundle" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ProtocolXmlTreeParser owner" "ProtocolXmlTreeParser" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ProtocolXmlElement owner" "ProtocolXmlElement" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single LibomvUuidCodec owner" "LibomvUuidCodec" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single UnsignedLongBitsParser owner" "UnsignedLongBitsParser" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single LibomvPacketCodec owner" "LibomvPacketCodec" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single LibomvBytePacketWriter owner" "LibomvBytePacketWriter" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ProtocolHttpRequestPolicy owner" "ProtocolHttpRequestPolicy" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single Md5DigestPort owner" "Md5DigestPort" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single LoginSecretJsonDecoder owner" "LoginSecretJsonDecoder" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single HostessViewerIdentityBuilder owner" "HostessViewerIdentityBuilder" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ProtocolLoginRuntime owner" "ProtocolLoginRuntime" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ProtocolGroupRuntime owner" "ProtocolGroupRuntime" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ProtocolInventoryRuntime owner" "ProtocolInventoryRuntime" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single ProtocolNoticeRuntime owner" "ProtocolNoticeRuntime" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track F single NoticeDispatchService owner" "NoticeDispatchService" 1 "${track_f_owner_targets[@]}"

check_no_forbidden_files \
    "Track D generic owner file names" \
    "${track_d_main_roots[@]}"

check_no_hits \
    "Track D old SessionService login overload" \
    "$TRACK_D_SESSION_LOGIN_OVERLOAD_PATTERN" \
    "${track_d_session_service_targets[@]}"

check_no_hits \
    "Track D one-argument CLI/app login calls" \
    "$TRACK_D_SESSION_LOGIN_ONE_ARG_PATTERN" \
    "${track_d_tools_apps_targets[@]}"

check_notice_dispatch_overloads

check_no_hits \
    "Track D old notice dispatch single-line route" \
    "$TRACK_D_OLD_NOTICE_CALL_PATTERN" \
    "${track_d_notice_dispatch_targets[@]}"

check_notice_dispatch_call_blocks \
    "Track I notice dispatch calls forbid named compliance" \
    "${track_d_notice_dispatch_targets[@]}"

check_no_hits \
    "Track D direct tools/apps group notice send" \
    "$TRACK_D_SEND_GROUP_NOTICE_CALL_PATTERN" \
    "${track_d_tools_apps_targets[@]}"

check_no_hits \
    "Track D spoofed viewer channel names" \
    "$TRACK_D_SPOOFED_CHANNEL_PATTERN" \
    "${production_targets[@]}"

check_no_hits \
    "Track DS old LLSD login route" \
    "$TRACK_DS_OLD_LOGIN_LLSD_PATTERN" \
    "${track_ds_old_login_targets[@]}"

check_no_hits \
    "Track DS direct package owners outside protocol module" \
    "$TRACK_DS_DIRECT_OWNER_PATTERN" \
    "${track_ds_direct_owner_forbidden_targets[@]}"

check_no_hits \
    "Track DS stale login package fields" \
    "$TRACK_DS_STALE_LOGIN_FIELD_PATTERN" \
    "${track_ds_login_package_targets[@]}"

check_no_hits \
    "Track E stale create/upload symbols" \
    "$TRACK_E_STALE_ATTACHMENT_PATTERN" \
    "${track_e_kotlin_targets[@]}"

check_no_file_path_hits \
    "Track E forbidden attachment owner file names" \
    "$TRACK_E_FORBIDDEN_OWNER_FILE_PATTERN" \
    "${track_e_kotlin_targets[@]}"

check_no_hits \
    "Track E forbidden attachment owner declarations" \
    "$TRACK_E_FORBIDDEN_OWNER_DECL_PATTERN" \
    "${track_e_kotlin_targets[@]}"

check_no_hits \
    "Track E implicit fake defaults" \
    "$TRACK_E_IMPLICIT_FAKE_PATTERN" \
    "${track_e_fake_default_targets[@]}"

check_no_hits \
    "Track E production local HTTP allowance" \
    "$TRACK_E_LOCAL_HTTP_PATTERN" \
    "${track_e_local_http_targets[@]}"

check_no_hits \
    "Track E Android probe injection/status leakage" \
    "$TRACK_E_ANDROID_PROBE_PATTERN" \
    "${track_e_android_probe_targets[@]}"

check_no_hits \
    "Track E generated packet catalogue not wired to commonMain" \
    "$TRACK_E_GENERATED_PACKET_COMMONMAIN_PATTERN" \
    "${track_e_packet_generation_targets[@]}"

check_no_hits \
    "Track E generated packet imports absent from production source" \
    "$TRACK_E_PRODUCTION_PACKET_IMPORT_PATTERN" \
    "${track_e_production_packet_import_targets[@]}"

check_no_hits \
    "Track G forbidden capability/notecard owner names" \
    "$TRACK_G_FORBIDDEN_OWNER_PATTERN" \
    "${track_g_main_targets[@]}"

check_no_hits \
    "Track G old EventQueue seed route" \
    "$TRACK_G_EVENT_QUEUE_SEED_PATTERN" \
    "${track_g_event_queue_targets[@]}"

check_no_hits \
    "Track G CLI direct protocol route" \
    "$TRACK_G_CLI_DIRECT_PROTOCOL_PATTERN" \
    "${track_g_cli_targets[@]}"

check_no_hits \
    "Track G CLI raw capability evidence labels" \
    "$TRACK_G_CLI_RAW_CAPABILITY_PATTERN" \
    "${track_g_cli_targets[@]}"

check_no_hits \
    "Track H stale bounded circuit owner" \
    "$TRACK_H_STALE_CIRCUIT_OWNER_PATTERN" \
    "${track_f_all_source_targets[@]}"

check_no_hits \
    "Track H stale CLI live send path" \
    "$TRACK_H_STALE_CLI_LIVE_SEND_PATTERN" \
    "${track_h_cli_live_send_targets[@]}"

check_no_hits \
    "Track H stale full proof routes" \
    "$TRACK_H_STALE_FULL_PROOF_PATTERN" \
    "${track_h_full_proof_targets[@]}"

check_no_hits \
    "Track H stale notice owner routes" \
    "$TRACK_H_STALE_NOTICE_PATTERN" \
    "${track_h_notice_stale_targets[@]}"

check_no_hits \
    "Track H direct CLI/app notice protocol routes" \
    "$TRACK_H_DIRECT_APP_NOTICE_PATTERN" \
    "${track_h_direct_app_notice_targets[@]}"

check_no_hits \
    "Track HS duplicate simulator exchange outside UDP owner" \
    "$TRACK_HS_DUPLICATE_SIMULATOR_EXCHANGE_PATTERN" \
    "${track_hs_duplicate_exchange_targets[@]}"

check_no_hits \
    "Track HS direct CLI/app archive protocol routes" \
    "$TRACK_HS_DIRECT_ARCHIVE_PATTERN" \
    "${track_hs_direct_archive_targets[@]}"

check_no_hits \
    "Track HS fake live proof pass route" \
    "$TRACK_HS_FAKE_LIVE_PASS_PATTERN" \
    "${track_hs_fake_live_targets[@]}"

check_required_hits \
    "Track HS full proof requires archive read-back" \
    "$TRACK_HS_FULL_PROOF_ARCHIVE_REQUIRED_PATTERN" \
    "${track_hs_full_proof_targets[@]}"

check_no_hits \
    "Incident freeze forbids group membership mutation packets" \
    "$TRACK_INCIDENT_GROUP_MUTATION_PATTERN" \
    "${track_incident_group_mutation_targets[@]}"

check_no_hits \
    "Android compatibility probe forbidden runtime routes" \
    "$TRACK_ANDROID_PROBE_FORBIDDEN_RUNTIME_PATTERN" \
    "${track_android_probe_targets[@]}"

check_no_hits \
    "Track I stale local notice totals production paths" \
    "$TRACK_I_STALE_NOTICE_COMPLIANCE_PATTERN" \
    "${track_i_stale_notice_compliance_targets[@]}"

check_no_hits \
    "Track I stale local notice totals public docs" \
    "$TRACK_I_STALE_NOTICE_COMPLIANCE_PATTERN" \
    "${track_i_public_doc_targets[@]}"

check_no_hits \
    "Track J forbids classic avatar baking production paths" \
    "$TRACK_J_CLASSIC_BAKING_PATTERN" \
    "${track_j_main_targets[@]}"

check_no_hits \
    "Track J forbidden avatar owner names" \
    "$TRACK_J_FORBIDDEN_AVATAR_OWNER_PATTERN" \
    "${track_j_main_targets[@]}"

check_no_hits \
    "Track J avatar appearance source does not seed capabilities directly" \
    "$TRACK_J_DIRECT_AVATAR_SEED_PATTERN" \
    "${track_j_avatar_source_targets[@]}"

check_no_hits \
    "Track J CLI commands do not construct protocol avatar runtime" \
    "$TRACK_J_CLI_DIRECT_AVATAR_PATTERN" \
    "${track_j_cli_command_targets[@]}"

check_no_hits \
    "Track J full proof no stale simulator-presence gate" \
    "$TRACK_J_STALE_FULL_PROOF_SIMULATOR_GATE_PATTERN" \
    "${track_j_full_proof_targets[@]}"

check_no_hits \
    "Track J no extra SimulatorPacketExchange implementations" \
    "$TRACK_J_EXTRA_SIMULATOR_EXCHANGE_IMPL_PATTERN" \
    "${track_j_simulator_exchange_impl_targets[@]}"

check_exact_owner_count "Track G single InventoryPort owner" "InventoryPort" 1 "${track_g_main_targets[@]}"
check_exact_owner_count "Track G single InventoryDirectoryService owner" "InventoryDirectoryService" 1 "${track_g_main_targets[@]}"
check_exact_owner_count "Track G single ProtocolCapabilitySeedClient owner" "ProtocolCapabilitySeedClient" 1 "${track_g_main_targets[@]}"
check_exact_owner_count "Track G single ProtocolCapabilityCacheProvider owner" "ProtocolCapabilityCacheProvider" 1 "${track_g_main_targets[@]}"
check_exact_owner_count "Track H single InventorySelectionService owner" "InventorySelectionService" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track H single LibomvInventoryPermissionMapping owner" "LibomvInventoryPermissionMapping" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track H single ProtocolSimulatorCircuitClient owner" "ProtocolSimulatorCircuitClient" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track H single LibomvNoticePacketCodec owner" "LibomvNoticePacketCodec" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track H single ProtocolNoticeCircuitSource owner" "ProtocolNoticeCircuitSource" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track J single AvatarPort owner" "AvatarPort" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track J single AvatarReadinessService owner" "AvatarReadinessService" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track J single CurrentOutfitVersionSource owner" "CurrentOutfitVersionSource" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track J single LibomvAvatarAdapter owner" "LibomvAvatarAdapter" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track J single ProtocolAvatarRuntime owner" "ProtocolAvatarRuntime" 1 "${track_f_owner_targets[@]}"
check_exact_owner_count "Track J single ProtocolAvatarAppearanceSource owner" "ProtocolAvatarAppearanceSource" 1 "${track_f_owner_targets[@]}"

check_no_hits \
    "Track D viewer identity provider outside protocol module" \
    "$TRACK_D_VIEWER_PROVIDER_PATTERN" \
    "${track_d_viewer_provider_forbidden_targets[@]}"

if [[ "${#track_d_notice_time_targets[@]}" -gt 0 ]]; then
    check_no_hits \
        "Track D NoticeComplianceService direct system time" \
        "$TRACK_D_NOTICE_TIME_PATTERN" \
        "${track_d_notice_time_targets[@]}"
else
    echo "PASS: Track D NoticeComplianceService direct system time owner deleted"
fi

check_no_hits \
    "Track D raw report key leakage" \
    "$TRACK_D_RAW_REPORT_KEY_PATTERN" \
    "${track_d_report_key_targets[@]}"

check_no_hits \
    "Track D direct UUID target UX" \
    "$TRACK_D_UUID_TARGET_PATTERN" \
    "${cli_command_targets[@]}"

check_no_hits \
    "Track D CLI-owned cap literals" \
    "$TRACK_D_CLI_CAP_LITERAL_PATTERN" \
    "${track_d_cap_forbidden_targets[@]}"

check_pattern_matches \
    "self-test core forbidden dependency pattern" \
    "$CORE_FORBIDDEN_PATTERN" \
    'implementation(project(":hostess-protocol-libomv"))'

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

check_pattern_matches \
    "self-test Track B direct OkHttp pattern" \
    "$TRACK_B_OKHTTP_PATTERN" \
    "import okhttp3.${OK_HTTP_CLIENT_SYMBOL}"

check_pattern_matches \
    "self-test Track B runtime/transport direct-call pattern" \
    "$TRACK_B_RUNTIME_PATTERN" \
    "${PROTOCOL_PREFIX}HttpClient.execute(request)"

check_pattern_matches \
    "self-test Track B stale platform pattern" \
    "$FORBIDDEN_PLATFORM_PATTERN" \
    'org.apache.http.client.HttpClient'

check_pattern_matches \
    "self-test Track B UI direct protocol pattern" \
    "$TRACK_B_UI_DIRECT_PROTOCOL_PATTERN" \
    'ProtocolNoticeRuntime(clientSession)'

check_pattern_matches \
    "self-test Track B UI direct vault pattern" \
    "$TRACK_B_UI_DIRECT_VAULT_PATTERN" \
    'CredentialVault.resolve(handle)'

check_pattern_matches \
    "self-test Track B UI direct notice pattern" \
    "$TRACK_B_UI_DIRECT_NOTICE_PATTERN" \
    'targetSet.selectedGroups.forEach { noticePort.sendGroupNotice(session, it, draft, null) }'

check_pattern_matches \
    "self-test Track B UI forbidden owner pattern" \
    "$TRACK_B_UI_FORBIDDEN_OWNER_PATTERN" \
    'class ScreenHelpers'

check_pattern_matches \
    "self-test Track B UI WebView pattern" \
    "$TRACK_B_UI_WEBVIEW_PATTERN" \
    'android.webkit.WebView(context).loadUrl("styles.css")'

check_pattern_matches \
    "self-test Track B UI staged code pattern" \
    "$TRACK_B_UI_STAGED_PATTERN" \
    'error("not implemented")'

check_pattern_matches \
    "self-test Track B UI visible label pattern" \
    "$TRACK_B_UI_TEXT_PATTERN" \
    '"Send notices"'

check_pattern_matches \
    "self-test Track B UI style constant pattern" \
    "$TRACK_B_UI_STYLE_PATTERN" \
    'val gap = 12.dp'

check_pattern_matches \
    "self-test Track C direct runtime pattern" \
    "$TRACK_C_RUNTIME_PATTERN" \
    'EventQueueGetClient()'

check_pattern_matches \
    "self-test Track C raw env pattern" \
    "$TRACK_C_ENV_PATTERN" \
    'System.getenv("HOSTESS_SECRET")'

check_pattern_matches \
    "self-test Track C file route pattern" \
    "$TRACK_C_FILE_ROUTE_PATTERN" \
    '--credential-file'

check_pattern_matches \
    "self-test Track C unsupported secret store pattern" \
    "$TRACK_C_UNSUPPORTED_SECRET_PATTERN" \
    'keychain lookup'

check_pattern_matches \
    "self-test Track C UI split-login pattern" \
    "$TRACK_C_UI_SPLIT_LOGIN_PATTERN" \
    'LoginSavedAccountPanel(state)'

check_pattern_matches \
    "self-test Track C UI fake location pattern" \
    "$TRACK_C_UI_FAKE_LOCATION_PATTERN" \
    'locationLabel = "London City"'

check_pattern_matches \
    "self-test Track C UI scaffold pattern" \
    "$TRACK_C_UI_SCAFFOLD_REQUIRED_PATTERN" \
    'HostessAppScaffold(content = {})'

check_pattern_matches \
    "self-test Track C UI custom icon pattern" \
    "$TRACK_C_UI_CUSTOM_ICON_PATTERN" \
    'Canvas(modifier) { drawLine(color, start, end) }'

check_pattern_matches \
    "self-test HS002 Track D UI preference adapter pattern" \
    "$HS002_TRACK_D_UI_PREFERENCE_ADAPTER_PATTERN" \
    'implementation(project(":hostess-preferences"))'

check_pattern_matches \
    "self-test HS002 Track D vault theme storage pattern" \
    "$HS002_TRACK_D_VAULT_THEME_STORAGE_PATTERN" \
    'ThemePreferenceService(FileThemePreferenceStore(Path.of("ui.properties")))'

check_pattern_matches \
    "self-test HS002 Track D Haccu colour pattern" \
    "$HS002_TRACK_D_HACCU_COLOUR_PATTERN" \
    'val accent = Color(0xFF8B0101)'

check_pattern_matches \
    "self-test HS002 Track D visible label pattern" \
    "$HS002_TRACK_D_VISIBLE_LABEL_PATTERN" \
    '"Ella Hostess"'

check_pattern_matches \
    "self-test HS002 Track D prototype runtime pattern" \
    "$HS002_TRACK_D_PROTOTYPE_RUNTIME_PATTERN" \
    'android.webkit.WebView(context).loadDataWithBaseURL("index-multi", "<html", "text/html", "utf-8", null)'

check_pattern_matches \
    "self-test HS002 Track D logo owner pattern" \
    "$HS002_TRACK_D_LOGO_OWNER_PATTERN" \
    'fun HostessBrandLogoIcon(modifier: Modifier = Modifier)'

check_pattern_matches \
    "self-test HS002 Track D theme toggle owner pattern" \
    "$HS002_TRACK_D_TOGGLE_OWNER_PATTERN" \
    'fun ThemeModeToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit)'

check_pattern_matches \
    "self-test HS002 Track D palette provider pattern" \
    "$HS002_TRACK_D_PALETTE_OWNER_PATTERN" \
    'object HaccuHostessPaletteProvider : HostessPaletteProvider'

check_pattern_matches \
    "self-test HS002 Track D Android label pattern" \
    "$HS002_TRACK_D_ANDROID_LABEL_PATTERN" \
    'manifestPlaceholders["appLabel"] = "Ella Hostess"'

check_pattern_matches \
    "self-test HS002 Track D desktop vendor pattern" \
    "$HS002_TRACK_D_DESKTOP_VENDOR_PATTERN" \
    'vendor = "Ella Hostess"'

check_pattern_matches \
    "self-test HS002 Track D root project label pattern" \
    "$HS002_TRACK_D_ROOT_PROJECT_LABEL_PATTERN" \
    'manifestPlaceholders["appLabel"] = rootProject.name.replaceFirstChar { it.titlecase() }'

check_pattern_matches \
    "self-test Track A forbidden vault dependency pattern" \
    "$TRACK_A_FORBIDDEN_DEP_PATTERN" \
    'implementation("com.example:java-keyring:1.0")'

check_pattern_matches \
    "self-test Track A stale credential route pattern" \
    "$TRACK_A_STALE_CREDENTIAL_ROUTE_PATTERN" \
    'VaultUnlock asks for passphrase'

check_pattern_matches \
    "self-test Track A AndroidX crypto pattern" \
    "$TRACK_A_ANDROIDX_CRYPTO_PATTERN" \
    'EncryptedSharedPreferences.create(...)'

check_pattern_matches \
    "self-test Track A raw key exposure pattern" \
    "$TRACK_A_RAW_KEY_EXPOSURE_PATTERN" \
    'fun exportKey(): SecretKey = key'

check_pattern_matches \
    "self-test Track A app env resolver pattern" \
    "$TRACK_A_APP_ENV_RESOLVER_PATTERN" \
    'EnvironmentLoginSecretResolver()'

check_pattern_matches \
    "self-test Track F common forbidden platform API pattern" \
    "$TRACK_F_COMMON_FORBIDDEN_PATTERN" \
    'java.util.UUID.randomUUID()'

check_pattern_matches \
    "self-test Track F parallel path pattern" \
    "$TRACK_F_PARALLEL_PATH_PATTERN" \
    'class AndroidProtocolLibomvModule'

check_pattern_matches \
    "self-test Track F platform API confinement pattern" \
    "$TRACK_F_PLATFORM_API_PATTERN" \
    'import java.security.MessageDigest'

check_pattern_matches \
    "self-test Track D generic owner pattern" \
    "$TRACK_D_GENERIC_OWNER_PATTERN" \
    'src/main/kotlin/org/hostess/core/LoginCompliance.kt'

check_pattern_matches \
    "self-test Track D old login overload pattern" \
    "$TRACK_D_SESSION_LOGIN_OVERLOAD_PATTERN" \
    'fun login(request: LoginRequest): SessionLoginResult'

check_pattern_matches \
    "self-test Track D one-argument login call pattern" \
    "$TRACK_D_SESSION_LOGIN_ONE_ARG_PATTERN" \
    'runtime.sessionService.login(request)'

check_pattern_matches \
    "self-test Track D old notice route pattern" \
    "$TRACK_D_OLD_NOTICE_CALL_PATTERN" \
    'runtime.noticeDispatchService.dispatch(session, draft)'

check_pattern_matches \
    "self-test Track D direct group notice send pattern" \
    "$TRACK_D_SEND_GROUP_NOTICE_CALL_PATTERN" \
    'runtime.noticePort.sendGroupNotice(session, group, draft, null)'

check_pattern_matches \
    "self-test Track D viewer provider placement pattern" \
    "$TRACK_D_VIEWER_PROVIDER_PATTERN" \
    'HostessViewerIdentityProvider'

check_pattern_matches \
    "self-test Track D spoofed channel pattern" \
    "$TRACK_D_SPOOFED_CHANNEL_PATTERN" \
    'channel = "Firestorm"'

check_pattern_matches \
    "self-test Track D notice time pattern" \
    "$TRACK_D_NOTICE_TIME_PATTERN" \
    'Instant.now()'

check_pattern_matches \
    "self-test Track D raw report key pattern" \
    "$TRACK_D_RAW_REPORT_KEY_PATTERN" \
    '"seedCapability" to identity.seedCapability'

check_pattern_matches \
    "self-test Track D UUID target UX pattern" \
    "$TRACK_D_UUID_TARGET_PATTERN" \
    '--group-uuid'

check_pattern_matches \
    "self-test Track D CLI cap literal pattern" \
    "$TRACK_D_CLI_CAP_LITERAL_PATTERN" \
    'if (count > 4_500) return blocked'

check_pattern_matches \
    "self-test Track DS old LLSD login route pattern" \
    "$TRACK_DS_OLD_LOGIN_LLSD_PATTERN" \
    'body = ProtocolHttpBody.TextBody(loginBody(secret), "application/llsd+xml")'

check_pattern_matches \
    "self-test Track DS direct package owner pattern" \
    "$TRACK_DS_DIRECT_OWNER_PATTERN" \
    'LoginPackageBuilder().build(secret, viewer, machine)'

check_pattern_matches \
    "self-test Track DS stale login field pattern" \
    "$TRACK_DS_STALE_LOGIN_FIELD_PATTERN" \
    'field("platform_string", value)'

check_pattern_matches \
    "self-test Track E stale create upload pattern" \
    "$TRACK_E_STALE_ATTACHMENT_PATTERN" \
    'body = ProtocolHttpBody.BinaryUploadBody(bytes, "application/octet-stream")'

check_pattern_matches \
    "self-test Track E forbidden owner file pattern" \
    "$TRACK_E_FORBIDDEN_OWNER_FILE_PATTERN" \
    'src/main/kotlin/org/hostess/core/AttachmentUploadManager.kt'

check_pattern_matches \
    "self-test Track E forbidden owner declaration pattern" \
    "$TRACK_E_FORBIDDEN_OWNER_DECL_PATTERN" \
    'class LandmarkAttachmentService'

check_pattern_matches \
    "self-test Track E implicit fake default pattern" \
    "$TRACK_E_IMPLICIT_FAKE_PATTERN" \
    'CommandMode.parse(null)'

check_pattern_matches \
    "self-test Track E production local HTTP pattern" \
    "$TRACK_E_LOCAL_HTTP_PATTERN" \
    'fun isLocalTestServer() = host == "127.0.0.1"'

check_pattern_matches \
    "self-test Track E Android probe hook pattern" \
    "$TRACK_E_ANDROID_PROBE_PATTERN" \
    'class AndroidCompatibilityProbe internal constructor(private val protocolLoadProbe: () -> State)'

check_pattern_matches \
    "self-test Track E generated packet commonMain pattern" \
    "$TRACK_E_GENERATED_PACKET_COMMONMAIN_PATTERN" \
    'generated/sources/libomvPackets/kotlin/commonMain'

check_pattern_matches \
    "self-test Track E production generated packet import pattern" \
    "$TRACK_E_PRODUCTION_PACKET_IMPORT_PATTERN" \
    'import libomv.packets.AssetUploadRequestPacket'

check_pattern_matches \
    "self-test Track G forbidden owner pattern" \
    "$TRACK_G_FORBIDDEN_OWNER_PATTERN" \
    'class InventoryBrowserService'

check_pattern_matches \
    "self-test Track G old EventQueue seed pattern" \
    "$TRACK_G_EVENT_QUEUE_SEED_PATTERN" \
    'private fun seedBody(requested: Set<String>) = requested.joinToString()'

check_pattern_matches \
    "self-test Track G CLI direct protocol pattern" \
    "$TRACK_G_CLI_DIRECT_PROTOCOL_PATTERN" \
    'ProtocolInventoryRuntime().listItems(session)'

check_pattern_matches \
    "self-test Track G CLI raw capability label pattern" \
    "$TRACK_G_CLI_RAW_CAPABILITY_PATTERN" \
    '"FetchInventoryDescendents2" to capabilityUrl'

check_pattern_matches \
    "self-test Track H stale bounded circuit owner pattern" \
    "$TRACK_H_STALE_CIRCUIT_OWNER_PATTERN" \
    'class BoundedSimulatorCircuitClient'

check_pattern_matches \
    "self-test Track H stale CLI live send pattern" \
    "$TRACK_H_STALE_CLI_LIVE_SEND_PATTERN" \
    'sessionProvider = { fakeSession(active = false) }'

check_pattern_matches \
    "self-test Track H stale full proof pattern" \
    "$TRACK_H_STALE_FULL_PROOF_PATTERN" \
    'private fun runBulkProof() = "bulk-notice"'

check_pattern_matches \
    "self-test Track H stale notice owner pattern" \
    "$TRACK_H_STALE_NOTICE_PATTERN" \
    'class NoticeSender { fun send() = GroupNoticeAdd }'

check_pattern_matches \
    "self-test Track H direct CLI app notice pattern" \
    "$TRACK_H_DIRECT_APP_NOTICE_PATTERN" \
    'ProtocolNoticeCircuitSource(transport).send(packet)'

check_pattern_matches \
    "self-test Track HS duplicate simulator exchange pattern" \
    "$TRACK_HS_DUPLICATE_SIMULATOR_EXCHANGE_PATTERN" \
    'class ExtraSimulatorSocket { val socket = DatagramSocket() }'

check_pattern_matches \
    "self-test Track HS direct archive pattern" \
    "$TRACK_HS_DIRECT_ARCHIVE_PATTERN" \
    'ProtocolGroupNoticeArchiveSource(client).read(group)'

check_pattern_matches \
    "self-test Track HS fake live pass pattern" \
    "$TRACK_HS_FAKE_LIVE_PASS_PATTERN" \
    'CommandMode.FAKE returns ProofReportStatus.PASSED'

check_pattern_matches \
    "self-test incident group mutation pattern" \
    "$TRACK_INCIDENT_GROUP_MUTATION_PATTERN" \
    'EjectGroupMemberRequestPacket()'

check_pattern_matches \
    "self-test Android probe forbidden runtime route pattern" \
    "$TRACK_ANDROID_PROBE_FORBIDDEN_RUNTIME_PATTERN" \
    'val resolver = EnvironmentLoginSecretResolver()'

check_pattern_matches \
    "self-test Track I stale notice compliance pattern" \
    "$TRACK_I_STALE_NOTICE_COMPLIANCE_PATTERN" \
    'NoticeComplianceService; NoticeSubmissionLedgerPort; noticeSubmissionProjectionStatus; noticeLedgerConfigured'

check_pattern_matches \
    "self-test Track J classic baking pattern" \
    "$TRACK_J_CLASSIC_BAKING_PATTERN" \
    'client.Appearance.RequestSetAppearance(true); AgentSetAppearance'

check_pattern_matches \
    "self-test Track J forbidden avatar owner pattern" \
    "$TRACK_J_FORBIDDEN_AVATAR_OWNER_PATTERN" \
    'class AvatarManager'

check_pattern_matches \
    "self-test Track J direct avatar seed pattern" \
    "$TRACK_J_DIRECT_AVATAR_SEED_PATTERN" \
    'identity.seedCapability'

check_pattern_matches \
    "self-test Track J CLI direct avatar pattern" \
    "$TRACK_J_CLI_DIRECT_AVATAR_PATTERN" \
    'ProtocolAvatarRuntime(clientSession).ensureReady(session)'

check_pattern_matches \
    "self-test Track J stale full-proof simulator gate pattern" \
    "$TRACK_J_STALE_FULL_PROOF_SIMULATOR_GATE_PATTERN" \
    'LiveProofSimulatorPresenceVerifier(groupDirectoryService).verify(session)'

check_pattern_matches \
    "self-test Track J extra simulator exchange implementation pattern" \
    "$TRACK_J_EXTRA_SIMULATOR_EXCHANGE_IMPL_PATTERN" \
    ') : SimulatorPacketExchange {'

if [[ "$failures" -ne 0 ]]; then
    exit 1
fi

echo "Hostess boundary checks passed."
