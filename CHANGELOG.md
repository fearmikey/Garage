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
