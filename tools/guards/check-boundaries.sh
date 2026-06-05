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
TRACK_C_RUNTIME_PATTERN='EnvironmentLoginSecretResolver|BoundedSimulatorCircuitClient|AgentDataUpdateRequestTransport|EventQueueGetClient'
TRACK_C_ENV_PATTERN='System(::|\.)getenv'
TRACK_C_FILE_ROUTE_PATTERN='credential-file'
TRACK_C_UNSUPPORTED_SECRET_PATTERN='keychain|Keychain|KeyStore|plaintext|plain-text|plain text'
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
    done < <(find "$@" -path '*/src/main/*' -type f \
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

check_notice_dispatch_overloads() {
    local file="hostess-core/src/main/kotlin/org/hostess/core/services/NoticeDispatchService.kt"
    local output

    set +e
    output="$(perl -0ne 'while (/fun\s+dispatch\s*\((.*?)\)\s*:/sg) { print "$ARGV: dispatch overload omits NoticeComplianceRequest\n" if $1 !~ /NoticeComplianceRequest/ }' "$file" 2>&1)"
    local status="$?"
    set -e

    if [[ "$status" -ne 0 ]]; then
        echo "ERROR: Track D notice dispatch overload scan failed"
        echo "$output"
        failures=1
    elif [[ -n "$output" ]]; then
        echo "FAIL: Track D notice dispatch overload requires NoticeComplianceRequest"
        echo "$output"
        failures=1
    else
        echo "PASS: Track D notice dispatch overload requires NoticeComplianceRequest"
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
        if ! printf '%s\n' "$block" | rg -q 'compliance[[:space:]]*='; then
            output+="${path}:${line}: noticeDispatchService.dispatch call omits named compliance argument"$'\n'
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
    "hostess-core/src/main" \
    "hostess-protocol-libomv/build.gradle.kts" \
    "hostess-protocol-libomv/src/main" \
    "tools/cli/build.gradle.kts" \
    "tools/cli/src/main" \
    "apps/desktop/build.gradle.kts" \
    "apps/desktop/src/main" \
    "apps/android/build.gradle.kts" \
    "apps/android/src/main"

okhttp_forbidden_targets=()
add_existing okhttp_forbidden_targets \
    "settings.gradle.kts" \
    "build.gradle.kts" \
    "gradle/libs.versions.toml" \
    "hostess-core/build.gradle.kts" \
    "hostess-core/src/main" \
    "hostess-protocol-libomv/build.gradle.kts" \
    "tools/cli/build.gradle.kts" \
    "tools/cli/src/main" \
    "apps/desktop/build.gradle.kts" \
    "apps/desktop/src/main" \
    "apps/android/build.gradle.kts" \
    "apps/android/src/main"
while IFS= read -r path; do
    okhttp_forbidden_targets+=("$path")
done < <(find "hostess-protocol-libomv/src/main" -type f ! -path '*/transport/*' 2>/dev/null || true)

track_b_runtime_forbidden_targets=()
add_existing track_b_runtime_forbidden_targets \
    "hostess-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_c_runtime_forbidden_targets=()
add_existing track_c_runtime_forbidden_targets \
    "hostess-core/src/main" \
    "tools/cli/src/main" \
    "apps/desktop/src/main" \
    "apps/android/src/main"

track_c_env_forbidden_targets=()
while IFS= read -r path; do
    case "$path" in
        *"/EnvironmentLoginSecretResolver.kt") ;;
        *) track_c_env_forbidden_targets+=("$path") ;;
    esac
done < <(find \
    "hostess-core/src/main" \
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
    "hostess-core/src/main" \
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

check_no_forbidden_files \
    "Track D generic owner file names" \
    "${track_d_main_roots[@]}"

check_no_hits \
    "Track D old SessionService login overload" \
    "$TRACK_D_SESSION_LOGIN_OVERLOAD_PATTERN" \
    "hostess-core/src/main/kotlin/org/hostess/core/services/SessionService.kt"

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
    "Track D notice dispatch calls require named compliance" \
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
    "Track D viewer identity provider outside protocol module" \
    "$TRACK_D_VIEWER_PROVIDER_PATTERN" \
    "${track_d_viewer_provider_forbidden_targets[@]}"

check_no_hits \
    "Track D NoticeComplianceService direct system time" \
    "$TRACK_D_NOTICE_TIME_PATTERN" \
    "hostess-core/src/main/kotlin/org/hostess/core/services/NoticeComplianceService.kt"

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

if [[ "$failures" -ne 0 ]]; then
    exit 1
fi

echo "Hostess boundary checks passed."
