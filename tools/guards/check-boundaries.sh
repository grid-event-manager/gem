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
        "$pattern" "$@" 2>&1)"
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
    output="$(printf '%s\n' "$sample" | rg -n "$pattern" 2>&1)"
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

if [[ "$failures" -ne 0 ]]; then
    exit 1
fi

echo "Hostess boundary checks passed."
