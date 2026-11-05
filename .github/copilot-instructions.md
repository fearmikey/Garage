# Garage - Copilot & Coding Assistant Instructions

## AGENT NAVIGATION & CODE MAP
- Before attempting any code modification, you must consult `.agent_manifest.json` to identify the architectural pattern and the specific files responsible for data flow. Do not guess file paths.
- At the end of each session where code files, features, or architectural patterns were added, modified, or deleted, update `.agent_manifest.json` to keep the code map accurate.

## ARCHITECTURAL CONSTRAINTS

- **100% FOSS & F-Droid Compliance:** Garage is strictly an Open Source (FOSS) application.
- **DO NOT** suggest or add Google Play Services (`com.google.android.gms:*`), AdMob / Google Mobile Ads, ML Kit, Firebase, Google Play Billing, or any proprietary SDKs.
- **DO NOT** add build flavors or feature code that depend on non-free binaries.
- All features must rely exclusively on open-source Android Jetpack (`androidx.*`), standard Kotlin, or permissive FOSS libraries.
- Maintain offline-first, privacy-respecting architecture.
