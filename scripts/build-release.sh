#!/usr/bin/env bash
# Build and sign the Garage release APK for a pushed tag, exactly the way
# F-Droid builds it, so F-Droid's reproducible-build check can copy this
# APK's signature onto its own build and ship it with our key.
#
#   scripts/build-release.sh v1.4.4
#
# Output: dist/<tag>/app-release.apk. The file name is fixed: the merged
# fdroiddata recipe downloads
#   https://github.com/fearmikey/Garage/releases/download/v%v/app-release.apk
# Renaming it would need an fdroiddata MR merged first.
#
# Secrets come from ~/.config/garage-release/release.env (chmod 600):
#   KEYSTORE=...  KS_PASS=...  KEY_PASS=... (optional)  KEY_ALIAS=... (optional)
# Missing passwords are prompted for. They're passed to keytool/apksigner
# through environment variables only, never on the command line.
#
# Overrides (for testing): REPO_URL, DIST_DIR, RELEASE_ENV, EXPECTED_CERT,
# KEEP_WORK=1, JAVA_HOME, ANDROID_HOME.
set -euo pipefail

die()  { echo "error: $*" >&2; exit 1; }
warn() { echo "warning: $*" >&2; }

TAG=${1:-}
[[ $TAG =~ ^v([0-9]+\.[0-9]+\.[0-9]+)$ ]] || die "usage: $0 vX.Y.Z"
VERSION=${BASH_REMATCH[1]}

# Signing certificate that F-Droid accepts (AllowedAPKSigningKeys in fdroiddata).
EXPECTED_CERT=${EXPECTED_CERT:-4171af965565fe07585f8fd7d2f936f5724d8d9fabe70f1d9a6c7cee8a8a327b}
REPO_URL=${REPO_URL:-https://github.com/fearmikey/Garage.git}
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
DIST_DIR=${DIST_DIR:-$ROOT/dist/$TAG}
RELEASE_ENV=${RELEASE_ENV:-$HOME/.config/garage-release/release.env}

export JAVA_HOME=${JAVA_HOME:-$HOME/android-studio/jbr}
export ANDROID_HOME=${ANDROID_HOME:-$HOME/Android/Sdk}
export PATH="$JAVA_HOME/bin:$PATH"   # apksigner needs java on PATH
[[ -x $JAVA_HOME/bin/java ]] || die "no java at JAVA_HOME=$JAVA_HOME"
BUILD_TOOLS=$(ls -d "$ANDROID_HOME"/build-tools/*/ 2>/dev/null | sort -V | tail -1)
APKSIGNER=${BUILD_TOOLS%/}/apksigner
[[ -x $APKSIGNER ]] || die "apksigner not found under $ANDROID_HOME/build-tools"

# --- Secrets ---------------------------------------------------------------
if [[ -f $RELEASE_ENV ]]; then
  perms=$(stat -c %a "$RELEASE_ENV")
  [[ $perms =~ ^[4-7]00$ ]] || warn "$RELEASE_ENV is readable by others (mode $perms); run: chmod 600 '$RELEASE_ENV'"
  set -a
  # shellcheck disable=SC1090
  source "$RELEASE_ENV"
  set +a
else
  warn "$RELEASE_ENV not found; prompting for secrets"
fi
KEYSTORE=${KEYSTORE:-$HOME/Documents/Keystores/Garage-Release.jks}
[[ -f $KEYSTORE ]] || die "keystore not found: $KEYSTORE"
if [[ -z ${KS_PASS:-} ]]; then
  read -rsp "Keystore password: " KS_PASS; echo >&2
fi
export KS_PASS
SIGN_ARGS=(--ks "$KEYSTORE" --ks-pass env:KS_PASS)
if [[ -n ${KEY_ALIAS:-} ]]; then SIGN_ARGS+=(--ks-key-alias "$KEY_ALIAS"); fi
if [[ -n ${KEY_PASS:-} ]]; then export KEY_PASS; SIGN_ARGS+=(--key-pass env:KEY_PASS); fi

# Fail on a wrong password now, not after a several-minute build.
keytool -list -keystore "$KEYSTORE" -storepass:env KS_PASS >/dev/null 2>&1 \
  || die "cannot open $KEYSTORE (wrong KS_PASS?)"

# --- Build from a fresh clone of the tag (mirrors F-Droid) -----------------
WORK=$(mktemp -d "${TMPDIR:-/tmp}/garage-release.XXXXXX")
cleanup() { if [[ ${KEEP_WORK:-0} == 1 ]]; then echo "work dir kept: $WORK"; else rm -rf "$WORK"; fi; }
trap cleanup EXIT

echo "==> Cloning $REPO_URL at $TAG"
git clone --quiet "$REPO_URL" "$WORK/src"
cd "$WORK/src"
git -c advice.detachedHead=false checkout --quiet "refs/tags/$TAG"
COMMIT=$(git rev-parse HEAD)
git reset --quiet --hard
git clean -dffx --quiet
grep -q "versionName = \"$VERSION\"" app/build.gradle.kts \
  || die "app/build.gradle.kts at $TAG does not have versionName \"$VERSION\""

# The merged recipe has no prebuild/rm/sudo/gradleprops steps
# (subdir: app, gradle: yes), so there is nothing to apply here.

echo "==> Building $TAG ($COMMIT)"
./gradlew --no-daemon assembleRelease
UNSIGNED=app/build/outputs/apk/release/app-release-unsigned.apk
[[ -f $UNSIGNED ]] || die "build produced no $UNSIGNED"

# --- Sign: only add the APK Signing Block ----------------------------------
# Must match what's already published: v2 only (minSdk 33), entries untouched.
mkdir -p "$DIST_DIR"
OUT=$DIST_DIR/app-release.apk
rm -f "$OUT"
echo "==> Signing"
"$APKSIGNER" sign "${SIGN_ARGS[@]}" \
  --alignment-preserved true \
  --v1-signing-enabled false --v2-signing-enabled true \
  --v3-signing-enabled false --v4-signing-enabled false \
  --out "$OUT" "$UNSIGNED"

VERIFY=$("$APKSIGNER" verify --print-certs -v "$OUT")
CERT=$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' <<<"$VERIFY")
[[ -z $CERT ]] && CERT=$(sed -n 's/^.*Signer: certificate SHA-256 digest: //p' <<<"$VERIFY" | head -1)
grep -q 'Verified using v2 scheme (APK Signature Scheme v2): true' <<<"$VERIFY" \
  || die "APK is not v2-signed"

echo
echo "Tag:     $TAG"
echo "Commit:  $COMMIT"
echo "APK:     $OUT"
echo "SHA-256 (file): $(sha256sum "$OUT" | cut -d' ' -f1)"
echo "Certificate SHA-256: $CERT"
if [[ $CERT != "$EXPECTED_CERT" ]]; then
  rm -f "$OUT"
  die "certificate does not match the expected $EXPECTED_CERT; APK deleted"
fi
echo "Certificate matches AllowedAPKSigningKeys."
