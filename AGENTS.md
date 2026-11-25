# Garage - Guidelines for AI Agents & Developers

## AGENT WORKFLOW & CODE MAP (.agent_manifest.json)

### MANDATORY CODE MAP CONSULTATION
Before attempting any code modification, agents **MUST** consult `.agent_manifest.json` to identify the architectural pattern, data flow, and specific files responsible for the feature. Do not guess file paths.

### CONTINUOUS MANIFEST MAINTENANCE
At the end of each session, if any files, features, data flows, or package structures were added, renamed, deleted, or altered, agents **MUST** update `.agent_manifest.json` to keep the codebase map accurate and up-to-date for future sessions.

### GIT COMMIT ATTRIBUTION
* **NO AI Co-Author Trailers:** Agents **MUST NOT** add `Co-Authored-By:` trailers (e.g., `Co-Authored-By: Claude ... <noreply@anthropic.com>`) or any other AI attribution lines (such as "Generated with ...") to commit messages, tags, or pull request descriptions. All commits are authored solely by the repository owner.

## CRITICAL ARCHITECTURAL CONSTRAINTS

### 1. STRICT 100% FOSS POLICY (F-DROID COMPLIANCE)
* **Zero Proprietary SDKs:** Garage is strictly Free and Open Source Software (FOSS), targeted for distribution on F-Droid and other open-source Android repositories.
* **FORBIDDEN DEPENDENCIES:**
  * **NO** Google Play Services (`com.google.android.gms:*`)
  * **NO** Google Mobile Ads / AdMob (`com.google.android.gms:play-services-ads`)
  * **NO** Google Play Billing (`com.android.billingclient:*`)
  * **NO** ML Kit or closed-source vision/speech SDKs (`com.google.mlkit:*`)
  * **NO** Firebase SDKs that pull in non-free binaries or Play Services requirements
  * **NO** Analytics, tracking, or proprietary telemetry libraries
* **ALLOWED DEPENDENCIES:**
  * Android Jetpack libraries (`androidx.*`)
  * Kotlin standard/coroutines/serialization libraries
  * Open-source third-party dependencies with permissive/copyleft FOSS licenses (Apache 2.0, MIT, BSD, GPL, MPL)
* **NO PRODUCT FLAVORS FOR PROPRIETARY PLUGINS:** Do not add product flavor dimensions or build variants that rely on non-free or Google Play-specific dependencies.

### 2. PRIVACY & OFFLINE FIRST
* The application must remain fully functional offline.
* All user data (vehicle records, maintenance logs, fuel data, reminders) must stay local on device or sync via user-configured open protocols (e.g., WebDAV).

## RELEASE WORKFLOW (GITHUB + F-DROID REPRODUCIBLE BUILDS)

### F-Droid state: D (merged, reproducible). Last checked 2026-11-25.
* Recipe: `metadata/com.fearmikey.garage.yml` on fdroiddata `master` (https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.fearmikey.garage.yml). This repo does **not** contain a copy. The inclusion MR (!49352) was merged on 2026-10-02; never push to or reuse its branch.
* `Binaries: https://github.com/fearmikey/Garage/releases/download/v%v/app-release.apk`
* `AllowedAPKSigningKeys: 4171af965565fe07585f8fd7d2f936f5724d8d9fabe70f1d9a6c7cee8a8a327b`
* `AutoUpdateMode: Version`, `UpdateCheckMode: Tags`. Build entries use `subdir: app` and `gradle: yes`, with no prebuild/rm/sudo/gradleprops.
* How it works: F-Droid's checkupdates bot adds a build entry for each new `vX.Y.Z` tag. F-Droid then builds it, checks that its build equals our GitHub `app-release.apk` apart from the signature, and ships **our** signed APK. If the GitHub asset is missing, misnamed, or built differently, the F-Droid build fails.

**Agents MUST re-run these checks at the start of every release, because the state can change:**
1. `git -C ~/fdroid/fdroiddata fetch origin master` (fetch only: the checkout is shared, so never switch its branch), then `git -C ~/fdroid/fdroiddata show origin/master:metadata/com.fearmikey.garage.yml`. Confirm that `Binaries:` and `AllowedAPKSigningKeys` are unchanged and that no build steps were added.
2. `curl -s "https://gitlab.com/api/v4/projects/fdroid%2Ffdroiddata/merge_requests?state=opened&search=com.fearmikey.garage&in=title,description"`. If a bot update or any other MR for this app is open, stop and tell the user.
3. `curl -s https://f-droid.org/api/v1/packages/com.fearmikey.garage`, download `https://f-droid.org/repo/com.fearmikey.garage_<newest versionCode>.apk` to a temp dir, and run `apksigner verify --print-certs` on it. The signer must equal `AllowedAPKSigningKeys`.

If anything differs from the above, stop and ask the user before releasing, and update this section.

### Trigger
The user saying "do a release" or "new release build" approves committing, tagging, pushing `master` and the tag, running the build script, and creating the GitHub release. It does **not** approve pushing anything to fdroiddata or GitLab.

### Steps
1. Re-check the F-Droid state (above).
2. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`. Keep `vcsInfo { include = false }` and `dependenciesInfo { includeInApk = false }`.
3. Add a new `## [X.Y.Z] - YYYY-MM-DD` section at the top of `CHANGELOG.md`. Never add to an already released section.
4. Create `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` with concise notes (500 characters max).
5. Update the version badge in `README.md` (and other README content if needed), plus `.agent_manifest.json` for any code changes.
6. Make **one** commit, `Release X.Y.Z: <short summary>` (no AI attribution), then create an annotated tag on it: `git tag -a vX.Y.Z -m "Release X.Y.Z"`. There's no follow-up "recipe" commit: the tag points at the release commit.
7. `git push origin master && git push origin vX.Y.Z`
8. Run `scripts/build-release.sh vX.Y.Z` in the background (about 5-10 min). It clones the pushed tag from GitHub into a temp dir, runs `git clean -dffx`, builds with `./gradlew --no-daemon assembleRelease` using Android Studio's JBR, signs with `apksigner`, and writes `dist/vX.Y.Z/app-release.apk`.
9. Check the printed `Certificate SHA-256`: it must equal `AllowedAPKSigningKeys`. The script deletes the APK and fails if it doesn't. Stop if anything differs.
10. Create the GitHub release: write the version's CHANGELOG section to a temp file, then run `gh release create vX.Y.Z dist/vX.Y.Z/app-release.apk --title "Garage X.Y.Z" --notes-file <file>`. If `gh` is unavailable, ask the user to upload `app-release.apk` in the browser, keeping that exact file name.
11. F-Droid follow-up: **none.** The checkupdates bot picks up the tag. Don't open version-bump MRs or edit fdroiddata.

### Signing secrets
The script reads `KEYSTORE`, `KS_PASS`, and optionally `KEY_PASS` and `KEY_ALIAS` from `~/.config/garage-release/release.env` (mode 600), and prompts for anything that's missing. Passwords reach `keytool`/`apksigner` only through environment variables. Agents must **never** read, print, or grep that file, and never ask for passwords in chat. If the file doesn't exist, give the user these commands:
```bash
mkdir -p ~/.config/garage-release && chmod 700 ~/.config/garage-release
read -rsp 'Garage keystore password: ' p; echo
( umask 077; { printf 'KEYSTORE=%q\n' "$HOME/Documents/Keystores/Garage-Release.jks"; printf 'KS_PASS=%q\n' "$p"; } > ~/.config/garage-release/release.env ); unset p
```
(Add `KEY_PASS=` the same way only if the key password differs from the keystore password.)

### Rules
* Never build release APKs from the working tree or Android Studio. Incremental builds can produce a different `classes.dex`.
* Never move, delete, or re-point a pushed tag, and never rewrite pushed history. If a release is broken, fix forward with a new version.
* Never change the signing key, the signing flags (v2 only: `--alignment-preserved true --v1-signing-enabled false --v2-signing-enabled true --v3-signing-enabled false --v4-signing-enabled false`), or the asset name `app-release.apk`. Renaming the asset requires an fdroiddata MR merged **first**.
* Never add path-, time-, or machine-dependent build inputs (build timestamps, absolute paths, git hashes in BuildConfig, hostnames). If you change the build setup (AGP/Kotlin major bumps, new Gradle plugins, compiled native code), prove reproducibility before releasing: build the tag from two fresh clones at different paths, then run F-Droid's check from `~/fdroid/fdroiddata` with `java` on `PATH`. It must print `None`:
  ```bash
  PATH=~/android-studio/jbr/bin:$PATH ~/fdroid/venv/bin/python -c "import tempfile, fdroidserver.common as c; c.read_config(); print(c.verify_apks('<signed app-release.apk>', '<independent app-release-unsigned.apk>', tempfile.mkdtemp()))"
  ```
* Only touch fdroiddata when the recipe itself must change (new build steps, a URL change), and ask the user first. Then branch from a fresh `origin/master` in a new worktree (`~/fdroid/fdroiddata-garage`), make the smallest possible change, and run `fdroid rewritemeta` and `fdroid lint`.
* If a shell command times out, check whether it actually ran (`git reflog`, `git tag -l`, `pgrep`, `gh release view`) before retrying.