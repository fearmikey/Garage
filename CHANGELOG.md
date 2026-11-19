## [1.4.3] - 2026-11-19

### Added
* **Parts & Supplies:** The Parts tab is now "Parts & Supplies". Below the parts quick reference, keep a list of parts and fluids you've bought or have in stock (description, part number, supplier, quantity, cost, purchase date). Oil filter, spark plug and air filter part numbers have an "Add as supply" shortcut. Supplies don't count toward cost of ownership, so repairs aren't counted twice (DB v35 → v36).
* **LubeLogger Supplies & Notes Sync:** Supplies sync with LubeLogger's Supplies, and Knowledge notes sync with LubeLogger's vehicle Notes (DB v36 → v37). Servers without these features are skipped.
* **LubeLogger Purchase Info Sync:** The purchase date and price sync both ways with the LubeLogger vehicle. Garage can only send changes when you sign in with a username and password. With an API key, the server's values are pulled but Garage edits stay local.
* **More Synced Details:** Maintenance category, task and deferral settings, fuel grade, EV charger details, and mod categories and product links now sync to LubeLogger as "Garage ..." extra fields. Your own extra fields are kept.
* **Urdu:** Garage is now available in Urdu (اردو), with right-to-left layout.
* **Pakistani Rupee (PKR):** Added to the currency picker.

### Fixed
* **LubeLogger Data Loss on Edit:** Editing a synced record in Garage no longer wipes its notes, tags, attachments, your own extra fields, the missed fill-up flag or equipment links on the LubeLogger server. LubeLogger clears every field an update leaves out, so Garage now sends them back unchanged.
* **Sync Restarting Mid-Run:** A LubeLogger sync no longer restarts every time it saves a record, which could also leave server records unlinked. Edits made during a sync are sent in one follow-up sync.
* **Wrong Record Links:** Two jobs logged at the same date and mileage (e.g. an oil change and a tire rotation) are no longer linked to each other's server record. Two vehicles of the same year, make and model with different VINs or plates are no longer merged.
* **Mods in LubeLogger:** A mod's title and description are now sent as LubeLogger's description and notes, instead of being joined as "Title - Description". Records sent by older versions are split back automatically, as are maintenance records saved by older versions as "Description (Task)".
* **EV Fuel Type:** New LubeLogger vehicles are created with fuel type Electric or Diesel based on the specs, so charging sessions are read as kWh.

## [1.4.2] - 2026-11-18

### Added
* **Simplified Chinese:** Garage is now available in Simplified Chinese (简体中文), including the F-Droid store listing. Pick it in Settings, during onboarding, or from Android's per-app language settings.
* **Chinese Yuan (CNY):** Added to the currency picker.

### Changed
* **Translated PDF Report:** The maintenance history PDF is now generated in your app language. Long labels no longer overlap: values follow the label, and table cells are shortened to fit their column.
* **Translated Maintenance Names:** Built-in task names (e.g. oil change, tire rotation) and the preset schedule templates, with their titles, descriptions and notes, now show in your language in the timeline, suggestions, templates, notifications and the home-screen widget. They're still stored in English, so schedules and LubeLogger sync are unaffected.
* **Translated OBD-II Scanner:** Connection progress and errors, sensor/PID names, readiness monitors and module names are now translated. Trouble-code descriptions are still English.
* **Translated Messages:** Document renewal notifications, the trip-tracking notification, and VIN decoding, recall, backup, WebDAV and LubeLogger error messages now appear in your language, as does the LubeLogger duplicate-cleanup confirmation (with proper plural forms).

## [1.4.1] - 2026-11-17

### Fixed
* **Spec Weights in Your Units:** Curb weight, GVWR, GCWR and towing capacities entered as plain numbers now show and edit in kg for metric/UK users and lbs for imperial users, instead of always "lbs". GVWR now gets a unit too. Values that already include a unit (e.g. NHTSA's "4480 lbs") are shown as stored.
* **Motorcycle Displacement in cc:** Motorcycle specs show and edit engine displacement in cc (e.g. "452 cc" instead of "0.452 L"). It's still stored in litres, so parts estimates are unchanged.
* **Comma Decimals in Specs:** Spec values typed with a comma decimal separator (e.g. "1,6") are now recognized as numbers and get their unit.

## [1.4.0] - 2026-11-14

### Added
* **Fuel Grade:** Fill-ups can record the fuel grade: Regular, Mid-grade, Premium, Diesel, E85, or your own text (e.g. "98 RON", "E10"). The last grade you used is pre-selected, and LubeLogger syncs keep the grade you entered locally (DB v34 → v35).
* **Translated Store Listing:** The F-Droid title and descriptions are now available in all 10 supported languages, and the English description lists the app's full feature set.

### Changed
* **Compact Mod Cards:** Mod photos now show as a small thumbnail (with a "+N" badge for extra photos) instead of a large banner, so more mods fit on screen.
* **Precise Location for GPS Trips:** If only approximate location is allowed, Garage now explains that GPS trips need precise location instead of starting a trip that can't track.

### Fixed
* **Registration & Insurance Data Loss:** Opening the Registration & Insurance editor before your saved details had loaded could show an empty form, and saving it erased the stored documents. The editor now waits for your data to load, and keeps your edits when you rotate the screen.
* **Safer Backups:** Backups are taken from a consistent database snapshot, so they can no longer be corrupted when the app writes data during a backup. A failed restore is rolled back to your previous data and asks you to restart the app.
* **WebDAV Auto-Backup Retries:** When sync isn't set up or the server rejects your credentials, the daily backup no longer retries over and over. Network and server errors are still retried.
* **EV Detection:** Vehicles like Chevrolet models or the Range Rover Evoque are no longer mistaken for EVs or plug-in hybrids. Opening the fuel log from a widget or tile now opens the correct fuel or charging form.
* **No-Browser Crashes:** Opening links (license, recalls, support, settings) no longer crashes when no web browser is installed.
* **Widget Deep Links:** Rotating the screen or changing the theme no longer re-runs the widget or notification action that opened the app.
* **Document Reminders:** Turning off document reminders now also clears expiry notifications that are already showing.
* **GPS Trip Service:** A trip that fails to start no longer leaves a broken tracking notification, and stale pause/resume actions no longer fake an active trip.
* **OBD-II Parsing:** A truncated multi-frame response from the adapter no longer fails the whole read.

## [1.3.9] - 2026-11-10

### Added
* **Android 13 Support:** Garage now installs on Android 13 (API 33) as well as Android 14 and newer. The Quick Settings tiles use the launch method each version supports.
* **More Brand Emblems:** Alfa Romeo, Jaguar and Land Rover vehicles now show their logos (the artwork was bundled but never connected).

### Changed
* **Privacy: No Google Cloud Backup:** Garage data is no longer uploaded to Google's Android backup. Use the built-in local backup or WebDAV sync instead. Direct phone-to-phone transfers still copy your data, but saved WebDAV and LubeLogger passwords must be re-entered afterwards.
* **HTTPS for NHTSA:** VIN decoding and recall lookups can only use HTTPS. Plain HTTP stays available for self-hosted LubeLogger/WebDAV servers.
* **Leaner App:** Removed the AppCompat and Material Components libraries (the VIN scanner now uses plain Android views), shared one HTTP client across all network features, and updated libraries (OkHttp 5, AndroidX Core, DataStore, Room, WorkManager, Compose, and others).
* **Full Translations:** Roughly 600 pieces of on-screen text that were hard-coded in English are now translated into all 10 supported languages (English, German, French, Spanish, Italian, Portuguese, Dutch, Polish, Russian, Turkish). This covers Settings, Add/Edit Vehicle, Documents, Fuel & Charging (including trend charts), Mods, Parts, the Maintenance timeline, filters, schedule and templates, Cost of Ownership, Trips, Knowledge, Recalls, the Dashboard, home-screen widgets, dialogs, toasts and accessibility labels.
* **Translated Labels:** Vehicle types, drivetrain, maintenance and mod categories, charger types, trip categories, accent colors, theme and unit choices now show in your language (saved data is unchanged). Currency names use the system's localized names, and counts such as "1 day / 5 days" use proper plural forms (including Polish and Russian).
* **Localized Dates:** Dates follow your language's format (e.g. "10.11.2026" in German, "10 нояб. 2026 г." in Russian) instead of always using the US "Nov 10, 2026" order. The "since last full tank" fuel badge is translated too.
* **German Tone:** All German text now consistently uses the formal "Sie", matching the onboarding screens.
* **Translations (earlier):** Added missing "Knowledge" and "Trips" tab names in all languages and the missing French onboarding text.

### Fixed
* **EV Charging Form Crash:** Opening the charging form no longer crashes in English, French, Spanish, Portuguese, Italian, Russian and Turkish (a stray `%` in the "Est. Range @ 100% SoC" label).
* **Startup Hang:** App start no longer blocks on reading cloud-backup settings.
* **Credential Restore Crash:** If saved WebDAV/LubeLogger credentials can't be decrypted (for example after restoring from an old backup), they're reset instead of crashing the app.

## [1.3.8] - 2026-11-08

### Added
* **Editable Vehicle Specs:** An Edit button on the Specs tab lets you correct any VIN-decoded spec (blank hides it) and add your own name/value rows, e.g. for vehicles the NHTSA decoder only partially covers (DB v32 → v33).

### Fixed
* **Recall Check HTTP 400:** NHTSA answers "no recalls for this year/make/model" (including makes it doesn't know, such as many non-US motorcycles) with HTTP 400. This is now shown as "no open recalls" instead of an error, and network/server failures get readable messages.
* **Recall Error Flicker:** Pressing Retry no longer flashes the error message away before it can be read.
* **Kilometer Round-Trips:** Distances are stored as fractional miles, so km values no longer come back ±1 km (DB v33 → v34). LubeLogger sync and deferred-maintenance reminders convert units correctly.
* **Tire Pressure & Wiper Units:** The Parts edit screen now shows and saves values in your unit (bar/PSI, mm/in), matching the Parts list.
* **GPS Trip Drift:** Trip logging ignores inaccurate or stale GPS fixes and jitter, saves the trip on Stop & Save (including from the notification), and shows distance in your configured unit.
* **Tab Lists Scroll Position:** Lists jump back to the top when a new entry is added, instead of hiding it above the viewport.

## [1.3.7] - 2026-11-04

### Added
* **Updated Document Management:** Track custom vehicle documents, receipts, certificates, and files with dedicated document storage and management UI.
* **Foreground Trip Tracking Service:** Dedicated background service for real-time trip and mileage tracking.
* **Advanced Timeline Filtering:** Interactive timeline filter bar and category chips for filtering maintenance records and vehicle events.
* **Customizable Vehicle Tabs & Smart Alerts:** Personalized vehicle tab configuration and dynamic tab notification badges.
* **Database Migrations (Schemas 30-32):** Room database schema updates supporting custom documents and enhanced trip records.
* **Expanded Test Suite:** Comprehensive unit test coverage for document expiration engines, trip repositories, and timeline filters.
* **GPS trip logging:** App can now log expensable trips with GPS enabled.
* **Enhanced onboarding:** App now features a better onboarding workflow.

## [1.3.6] - 2026-10-26

### Added
* **Trip & Mileage Tracking:** Log vehicle trips, record business vs. personal mileage, start/end odometers, trip categories, and expense tags.
* **Vehicle Knowledge Base & Torque Specs Directory:** Built-in factory torque specification catalog, custom vehicle notes, technical cheat sheets, and component torque lookup.
* **Redesigned Modular Settings & Accent Themes:** Overhauled Settings interface with sub-screens (Appearance, Backup, Reminders) and customizable vehicle color theme palettes.
* **Motorcycle & Powersports Brand Emblems:** Added vector emblems for Harley-Davidson, Honda Motorcycle, Kawasaki, Piaggio, Triumph, and Yamaha.
* **Expanded Test Suite:** Unit test coverage for `MaintenanceScheduleEngineTest`, `StartupViewModelTest`, `DashboardViewModelTest`, `VehicleDetailViewModelTest`, `SettingsViewModelTest`, and `MainViewModelTest`.

## [1.3.5] - 2026-10-10

### Added
* **Vehicle Manufacturer Brand Emblems:** Added vector brand logos for over 60 vehicle makes (Ford, Toyota, Honda, BMW, Mercedes, Porsche, Chevrolet, Audi, Tesla, Volvo, etc.) displayed on vehicle dashboard cards and creation screens.
* **LubeLogger Vehicle Details & Gas DTO Sync:** Expanded LubeLogger DTO mapper for full vehicle metadata and fuel log synchronization.

## [1.3.4] - 2026-10-09

### Added
* **Number & Currency Formatting Refinements:** Improved number and financial formatting utilities (`NumberFormatting`, `UnitConverter`) across odometer updates, fuel logs, modification details, and vehicle registration screens.
* **LubeLogger DTO & Mapping Enhancements:** Refined LubeLogger vehicle and gas record DTO mapping for robust cloud synchronization.
* **Expanded Test Suite:** Updated `NumberFormattingTest` and `LubeLoggerMapperTest` unit test suites.

## [1.3.3] - 2026-10-08

### Added
* **Parts Directory & Estimator Localization:** Fully localized vehicle parts catalog, parts cost estimator, parts details, and vehicle specs screens across all supported languages.
* **Unit Converter Refinements:** Enhanced unit conversion formatting and localized labels for distance, volume, and fuel efficiency metrics.
* **Comprehensive ViewModel Test Suite:** Expanded unit test coverage across `AddEditVehicleViewModelTest`, `VehicleDetailViewModelTest`, `RegistrationInsuranceViewModelTest`, `MaintenanceExportViewModelTest`, `UnitConverterTest`, and `DashboardViewModelTest`.

## [1.3.2] - 2026-10-08

### Added
* **Comprehensive String Localization:** Expanded localized string translations across German, Spanish, French, Italian, Dutch, Polish, Portuguese, Russian, and Turkish.
* **Localized Maintenance Task Formatter (`TaskNameFormatter`):** Dynamic localization for default maintenance tasks and suggestion labels across timeline, vehicle tabs, cost analytics, and maintenance templates.

## [1.3.1] - 2026-10-07

### Added
* **Multi-Language Localization Support:** Added full internationalization and native translations for German (`de`), Spanish (`es`), French (`fr`), Italian (`it`), Dutch (`nl`), Polish (`pl`), Portuguese (`pt`), Russian (`ru`), and Turkish (`tr`).
* **Per-App Language Preferences:** Integrated `locales_config.xml` support allowing users to select their preferred language directly in system or in-app settings.

## [1.3.0] - 2026-10-07

### Added
* **Enhanced Background Sync & State Management:** Refined LubeLogger background sync worker, credentials manager, and ViewModel state synchronization across fuel, timeline, and suggestions screens.
* **OBD2 Telemetry & Scanner Refinements:** Improved live diagnostic scanner stability, OBD2 reading state flows, and unit test coverage.
* **Expanded Unit Test Suite:** Updated test suites for `ObdViewModelTest` and `LubeLoggerMapperTest`.

## [1.2.9] - 2026-10-05

### Added
* **Enhanced LubeLogger Integration & Bidirectional Sync:** Full synchronization support for service logs, repairs, upgrades, fuel records, and EV charging sessions with self-hosted LubeLogger instances.
* **LubeLogger Duplicate Resolver:** Dedicated screen and ViewModel for detecting, reviewing, and resolving duplicate records between local storage and LubeLogger.
* **LubeLogger Pending Deletion Tracking:** Local pending delete queue to safely propagate record deletions across sync cycles.
* **Database Migration (Schema 25):** Room database updates for LubeLogger sync status metadata and pending delete tracking entities.
* **Expanded Test Suite:** Unit test coverage for `LubeLoggerRecordSyncTest`, `LubeLoggerDuplicatesViewModelTest`, and updated sync ViewModels.

## [1.2.8] - 2026-10-05

### Added
* **LubeLogger Self-Hosted Sync:** Sync vehicle service logs and fuel fill-up records directly with your self-hosted LubeLogger instance.
* **Interactive Fuel Trend Analytics:** Added interactive fuel economy trend charts, cost breakdown graphs, and efficiency analytics across US/UK MPG, L/100km, and km/L.
* **Expanded OBD2 Diagnostics & DTC Lookup:** Upgraded OBD2 Bluetooth/Wi-Fi diagnostic scanner with full Freeze Frame decoding, comprehensive offline DTC trouble code definitions, live PID readings, and adapter device selection.
* **F-Droid Store Screenshots:** Updated high-resolution store screenshots for F-Droid listings in Fastlane metadata.

### Changed
* **F-Droid Reproducible Build Compliance:** Disabled VCS info in Gradle release builds to ensure deterministic, reproducible APK generation for F-Droid.
* **Expanded Test Suite:** Added unit test coverage for `ObdScannerContentTest`, `LubeLoggerMapperTest`, `TrendStatsTest`, `FuelTrendsTest`, and `ObdViewModelTest`.

## [1.2.6] - 2026-09-29

### Added
* **Engine & Cabin Air Filter Tracking:** Added dedicated specification fields and parts directory support for engine air filter and cabin air filter part numbers.
* **OBD2 Odometer Logging:** Automatically records vehicle odometer readings from OBD2 Bluetooth diagnostic scans directly to the vehicle timeline.
* **Database Migration (Schema 23):** Room database schema update adding engine and cabin filter fields to vehicle parts info.

## [1.2.5] - 2026-09-29

### Added
* **OBD2 Bluetooth Diagnostics:** Added live diagnostic scanner support for reading vehicle engine telemetry (RPM, speed, coolant temperature, fuel level, VIN) and scanning diagnostic trouble codes (DTCs) via Bluetooth OBD2 adapters.
* **Recall Campaign Status Tracking:** Track and persist individual recall campaign resolution statuses per vehicle (Open, Serviced/Completed, Does Not Affect Vehicle).
* **Database Migration (Schema 22):** Room database migration adding `recall_campaign_states` entity for recall status tracking.
* **Expanded Test Suite:** Unit test coverage for `ObdParserTest` and `RecallStateRepository`.

## [1.2.4] - 2026-09-29

### Added
* **Initial Mileage & New Vehicle Tracking:** Added support for recording a vehicle's initial mileage and whether it was purchased new.
* **Dashboard Preferences:** Added customizable dashboard settings to toggle the visibility of the Fuel Trend Graph and Fleet Overview sections.

## [1.2.3] - 2026-09-19

### Added
* **EV Charging Logging & Cost Calculator:** Added full support for electric vehicle charging sessions, energy tracking (kWh), charge rates, and cost estimations.
* **VIN Camera Barcode Scanner:** Added integrated camera barcode scanner for quick VIN scanning when adding vehicles.
* **Document Expiration Alerts:** Added registration and insurance document expiration tracking with proactive reminder notifications.
* **Home Screen Quick Action Widgets & Shortcuts:** Added new Glance widgets for logging fuel and updating vehicle odometers, plus launcher app shortcuts.
* **Database Migrations (Schemas 17-19):** Schema updates supporting EV charging records, document tracking, and custom rules.
* **Expanded Test Coverage:** Added unit test suites for charging calculations, document expiration logic, export utilities, and dashboard views.

## [1.2.2] - 2026-09-19

### Added
* **Updated Build & Dependency Settings:** Updated dependencies and resolved F-Droid build configuration compliance checks.

## [1.2.1] - 2026-09-19

### Added
* **Cost of Ownership Analytics:** Overhauled cost analytics screen with detailed expense breakdown charts and category filtering.
* **Expanded Test Coverage:** Added comprehensive unit tests for `CostOfOwnershipViewModelTest` and `MaintenanceScheduleEngineTest`.

### Changed
* **Maintenance Schedule Engine:** Refined suggestion logic and interval calculation thresholds.
* **Parts Estimator UI:** Improved parts estimation layout and calculations.

## [1.2.0] - 2026-09-19

### Added
* **Enhanced Vehicle Modifications & Upgrades:** Overhauled modification logging with expanded category selection, filtering, detailed cost/part stats, and custom modification entries.
* **Custom Scrollbar Component:** Added smooth custom-styled scrollbars across major screens for enhanced navigation.
* **Database Migrations (Schemas 13-16):** Database schema updates for vehicle specifications, modification fields, and reminder data handling.
* **Expanded Test Suite:** Added unit test coverage for `NumberFormattingTest`, `AddEditVehicleViewModelTest`, and `ModsViewModelTest`.

### Changed
* **Integrated Reminders System:** Streamlined maintenance reminder logic directly into the schedule engine and notification workers.
* **UI & Widget Refinements:** Enhanced vehicle cards, fuel fill-up logs, maintenance suggestions screen, and home screen Glance app widget.

## [1.1.9] - 2026-09-12

### Added
* **Privacy Policy (`PRIVACY.md`):** Added a dedicated offline-first Privacy Policy document outlining local-only data storage and zero-telemetry practices.
* **F-Droid Release Assets:** Added high-resolution store screenshots and F-Droid compliant metadata.

### Changed
* **Help & FAQ Link:** Updated Settings screen Help/FAQ entry to point directly to GitHub Discussions for community support.
* **Repository Cleanups:** Updated `.gitignore` rules to exclude release APK outputs and temporary build artifacts.

## [1.1.8] - 2026-09-12

### Added
* **Fleet Summary Header:** Added an aggregate fleet overview header on the Dashboard screen displaying total vehicles, active reminder alerts, overall fuel efficiency, and fleet health metrics.
* **Maintenance Schedule Templates:** Added preset maintenance schedule templates (Standard, Severe Service, High Mileage) with a 1-click apply bottom sheet.
* **FileProvider Support for Media Sharing:** Added `file_paths.xml` FileProvider configuration for secure cross-app photo and document sharing.
* **Database Migration Schema 12:** Room database migration supporting template metadata and service record attachments.

### Changed
* **Dashboard & Timeline UI:** Refined vehicle cards, service logging flow, and timeline rendering.

## [1.1.7] - 2026-09-12

### Added
* **Vehicle Modifications & Upgrades Tracking:** Track aftermarket parts, performance modifications, tuning, installation dates, costs, and categorizations.
* **Vehicle Registration & Insurance Management:** Manage registration renewal dates, insurance policy details, provider information, policy expiration reminders, and premium costs.
* **Tabbed Vehicle Interface:** Re-architected vehicle detail view with intuitive tabs for Specs, Maintenance, Fuel, Mods, and Registration/Insurance.
* **Support Open Source Dialog:** Added "Buy Me a Coffee" support dialog in Settings for optional developer appreciation.
* **Database Migration Schemas (9, 10, 11):** Room database updates supporting modification logs, registration details, and policy tracking.
* **Expanded Test Suite:** Added unit test coverage for `ModsViewModelTest`, `RegistrationInsuranceViewModelTest`, `DateFormattingTest`, and `MainViewModelTest`.

### Changed
* **Cost Analytics Integration:** Integrated modification and insurance expense metrics into the overall cost of ownership calculations.

## [1.1.6] - 2026-09-12

### Added
* **Expanded Vehicle Specifications (Database Schema 8):** Added support for extended technical specs including engine displacement, cylinder count, drive type, transmission type, fuel type details, GVWR, and manufacturer info.
* **Enhanced NHTSA VIN Decoding:** Improved automated specs extraction from NHTSA API responses.
* **Comprehensive Test Suite Expansion:** Added unit test coverage for `VehicleRepositoryTest`, `VehicleDetailViewModelTest`, `ReminderCheckWorkerTest`, and `MaintenanceScheduleEngineTest`.

### Changed
* **Maintenance Schedule & Reminders Engine:** Refined reminder check worker algorithms and notification dispatch logic.

## [1.1.5] - 2026-09-12

### Added
* **Multi-Currency Support:** Added currency selector dialog allowing users to choose their preferred currency (USD, EUR, GBP, CAD, AUD, JPY, etc.) across financial screens and cost tracking metrics.
* **100% FOSS Distribution:** Consolidated application structure to be strictly open-source (FOSS) for stores like F-Droid, removing proprietary Google Play dependencies and ad SDKs.
* **Expanded Unit Tests:** Added unit test coverage for unit converters, cost calculation ViewModels, and notification scheduling.

### Changed
* **Unit Converter:** Refactored unit conversions and formatting utilities for enhanced accuracy across distance and volume metrics.

## [1.1.4] - 2026-09-11

### Added
* **Product Flavor Dimensions (`foss` & `play`):** Introduced build flavors to support both open-source F-Droid builds and Google Play builds.
* **Fastlane Automation:** Added Fastlane configuration for automated app deployments and release management.
* **Fuel Economy Tests:** Added comprehensive unit test coverage for fuel economy calculations and widget UI updates.

### Changed
* **Modularized VIN Scanning:** Separated CameraX and ML Kit VIN scanning into the Play distribution flavor.

## [1.1.3] - 2026-09-08

### Added
* **Automated Local Backups:** Scheduled automatic local database and media backups with configurable retention policies and automatic cleanup.
* **WebDAV Backup Schedules:** Configurable auto-backup intervals (daily, weekly, monthly) for WebDAV cloud sync in Settings.
* **Widget Auto-Refresher:** Real-time home screen Glance widget updates whenever vehicles, service records, or fuel logs change.
* **Expanded Test Suite:** Added unit tests for DashboardViewModel, SettingsViewModel, CostOfOwnershipViewModel, and StartupViewModel.

### Changed
* **Backup Repository Improvements:** Enhanced backup ZIP archiving and restore validation logic for local and cloud operations.

## [1.1.2] - 2026-09-08

### Added
* **Notification Permission Management:** Added notification permission status indicators and direct system settings launcher in the Settings screen.
* **Send Test Notification:** Added a "Send Test Notification" feature in Settings to verify push notification delivery and channel setup.

### Changed
* **Lifecycle State Sync:** Automatically refreshes notification permission status when returning to the Settings screen.

## [1.1.1] - 2026-09-08

### Added
* **Report a Bug & Feedback Popup:** Updated Settings menu option with an interactive popup dialog linking directly to GitHub Issues and GitHub Discussions.
* **Benchmark Build Variant:** Added a `benchmark` build configuration with R8 optimizations enabled for realistic performance evaluation.

### Changed
* **Image Processing & Performance:** Integrated `androidx.exifinterface` to downsample vehicle photos on ingest while preserving EXIF orientation, eliminating scroll and navigation jank.
* **Photo Deletion Safety:** Deferred deletion of replaced vehicle photos until changes are explicitly saved, preventing accidental photo loss if editing is canceled.

## [1.1.0] - 2026-09-08

### Added
* **Fuel Tracking & Economy Analytics:** Log fuel fill-ups and track fuel efficiency with automatic calculations in MPG, L/100km, or km/L.
* **Cost of Ownership Dashboard:** Detailed cost analysis and financial breakdown comparing maintenance vs. fuel expenses over time and per distance.
* **NHTSA Safety Recalls Integration:** Real-time lookup of active manufacturer safety recalls by VIN, make, model, and year.
* **Vehicle Parts Catalog & Estimator:** Track replacement part numbers (filters, spark plugs, fluids, tire sizes) and estimate servicing costs.
* **PDF Maintenance Report Export:** Export complete service history and maintenance logs to clean, shareable PDF documents.
* **Home Screen App Widget:** Android Glance home screen widget displaying vehicle status and maintenance reminders with quick-log shortcuts.
* **WebDAV Cloud Backup & Sync:** Automated and manual cloud backups via WebDAV with encrypted credential storage.
* **Custom Maintenance Rules:** Personalized maintenance interval rules and thresholds per vehicle.
* **License:** Added formal MIT License.

### Changed
* **App Icon:** Added dedicated launcher icon branding.
* **Maintenance Schedule Engine:** Enhanced rules engine with improved interval algorithms and custom user rule mapping.
* **Documentation & README:** Completely revamped project documentation and GitHub repository overview.

## [1.0.1] - 2024-05-22

### Changed
* Bumped version from 1.0 to 1.0.1.

## [1.0.0] - 2024-05-22

### Added
* Initial release of the Garage application.
* Support for vehicle profile creation and management.
* VIN scanning functionality using CameraX.
* Maintenance history tracking and timeline view.
* Automated maintenance reminders via WorkManager.
* Local data persistence using Room.
* Data backup and restoration capabilities.
* Dashboard for overview of all managed vehicles.
