#!/usr/bin/env bash
# Sign locally with the existing production key. Passwords stay in apksigner's
# prompt or the existing release environment; the script never prints them.
set -euo pipefail
if [[ $# != 2 ]]; then
  echo 'Usage: bash distribution/fdroid/sign-release.sh UNSIGNED_APK OUTPUT_APK' >&2
  exit 2
fi
input=$1
output=$2
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
sdk_dir=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
if [[ -z "$sdk_dir" || ! -x "$sdk_dir/build-tools/35.0.0/apksigner" ]]; then
  echo 'Set ANDROID_HOME to an Android SDK with build-tools 35.0.0.' >&2
  exit 2
fi
if [[ ! -f "$input" || -e "$output" || -e "$output.part" ]]; then
  echo 'Input must exist; output and output.part must not already exist.' >&2
  exit 2
fi
package=$("$sdk_dir/build-tools/35.0.0/aapt" dump badging "$input" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
if [[ "$package" != dev.notechat ]]; then
  echo 'Expected the dev.notechat release APK; the isolated preview must not be signed for release.' >&2
  exit 2
fi
python3 "$script_dir/check-apk.py" "$input"
python3 "$script_dir/../check-native-libraries.py" "$input"
if [[ -z "${ANDROID_KEYSTORE_PATH:-}" ]]; then
  read -r -p 'Existing production keystore path: ' ANDROID_KEYSTORE_PATH
fi
if [[ -z "${ANDROID_KEY_ALIAS:-}" ]]; then
  read -r -p 'Key alias: ' ANDROID_KEY_ALIAS
fi
args=(--ks "$ANDROID_KEYSTORE_PATH" --ks-key-alias "$ANDROID_KEY_ALIAS" --v4-signing-enabled false)
if [[ -n "${ANDROID_KEYSTORE_PASSWORD:-}" ]]; then args+=(--ks-pass env:ANDROID_KEYSTORE_PASSWORD); fi
if [[ -n "${ANDROID_KEY_PASSWORD:-}" ]]; then args+=(--key-pass env:ANDROID_KEY_PASSWORD); fi
trap 'rm -f -- "$output.part"' EXIT
"$sdk_dir/build-tools/35.0.0/apksigner" sign "${args[@]}" --out "$output.part" "$input"
certificate=$("$sdk_dir/build-tools/35.0.0/apksigner" verify --print-certs "$output.part" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
expected=$(tr -d '\r\n' < "$script_dir/production-cert-sha256.txt")
if [[ "$certificate" != "$expected" ]]; then
  echo 'Signing certificate differs from published v1.5.0. No release APK was saved.' >&2
  exit 1
fi
mv -- "$output.part" "$output"
printf 'Signed and verified: %s\nCertificate SHA-256: %s\n' "$output" "$certificate"
