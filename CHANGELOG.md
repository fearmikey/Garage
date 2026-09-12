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
* **Terms of Service & License:** Added formal Terms of Service documentation and MIT License.

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
