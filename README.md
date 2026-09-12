# 🚗 Garage

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.1.5-blue.svg)](CHANGELOG.md)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-34-brightgreen.svg)](https://developer.android.com/about/versions/14)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-purple.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)

**Garage** is a modern, privacy-focused, comprehensive vehicle management application for Android. Built using Clean Architecture and Jetpack Compose, Garage enables vehicle owners to track maintenance schedules, monitor fuel efficiency, analyze cost of ownership, check safety recalls, and maintain detailed service records for all their vehicles in one centralized hub.

---

## 📋 Table of Contents

- [Features](#-features)
- [100% FOSS & F-Droid First](#-100-foss--f-droid-first)
- [Tech Stack & Architecture](#-tech-stack--architecture)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Building & Testing](#-building--testing)
- [Changelog](#-changelog)
- [License](#-license)
- [Terms of Service](#-terms-of-service)

---

## 🛡️ 100% FOSS & F-Droid First

Garage is strictly **Free and Open Source Software (FOSS)** built for distribution on F-Droid and open-source app repositories:
* **Zero Google Play Services:** No `com.google.android.gms` dependencies.
* **Zero Ads or Analytics:** No AdMob, tracking, or proprietary telemetry.
* **100% Open Source:** Built exclusively with Android Jetpack, Kotlin, and permissively licensed open-source libraries.

---

## ✨ Features

### 🚘 Vehicle Management & VIN Lookup
* **Comprehensive Garage Profiles:** Maintain complete specs for every vehicle in your fleet—including make, model, year, trim, mileage, license plate, VIN, fuel type, and custom vehicle images.
* **NHTSA VIN Decoder:** Automatically populate vehicle specifications and metadata using official NHTSA web service integrations.

### 🛠️ Service & Maintenance Tracking
* **Detailed Service Logs:** Log maintenance activities with date, mileage, service cost, category, service provider, and notes.
* **Maintenance Timeline:** Visual chronological history of all past and scheduled service events per vehicle.
* **Intelligent Maintenance Engine:** Smart recommendations based on current mileage, vehicle age, and custom usage rules.
* **Custom Maintenance Rules:** Create personalized service rules and interval thresholds tailored to specific vehicle requirements.
* **Automated Notifications:** Background workers powered by **WorkManager** deliver timely reminders for upcoming and overdue maintenance.

### ⛽ Fuel Economy & Logging
* **Fuel Fill-Up Logs:** Record odometer readings, fuel amounts, price per unit, and total cost during fill-ups.
* **Multi-Unit Efficiency Calculations:** Automatically computes fuel economy in **MPG (US/UK)**, **L/100km**, or **km/L**.
* **Fuel Consumption History:** Monitor fuel expense patterns and fuel efficiency over time.

### 📊 Cost of Ownership Analytics
* **Total Expense Breakdown:** In-depth visual breakdown comparing maintenance, fuel, and part expenses.
* **Cost Metrics:** Calculate precise cost-per-mile / cost-per-kilometer metrics and operational cost trends over time.

### ⚠️ NHTSA Safety Recalls
* **Recall Lookup:** Query the official **NHTSA Recall API** to receive real-time alerts regarding open safety recalls for your specific vehicle make, model, year, and VIN.

### ⚙️ Vehicle Parts Directory & Estimator
* **Parts Cheat Sheet:** Track exact part numbers and specifications for essential components—such as oil filters, air filters, cabin filters, spark plugs, wiper blades, tire sizes, battery types, and fluid capacities.
* **Cost Estimator:** Estimate parts costs and plan upcoming routine replacement budgets accurately.

### 📄 PDF Report Export
* **Exportable Maintenance Histories:** Generate professional, formatted PDF service records ready for personal archiving, insurance, or vehicle resale value verification.

### 📲 Home Screen App Widget
* **Android Glance Widget:** View upcoming and overdue maintenance status directly on your home screen with quick-action shortcuts for logging fuel fill-ups or service records.

### 🔒 Privacy & Data Backup
* **Local-First Storage:** Fully functional offline data storage backed by **Room Persistence Library**.
* **JSON Import & Export:** Transfer your entire garage dataset across devices using simple JSON backup files.
* **Secure WebDAV Cloud Backup:** Schedule or run automated cloud backups via WebDAV, backed by **AndroidX Security Crypto** for credential protection.

---

## 🛠️ Technical Stack & Architecture

Garage is engineered according to modern Android development standards following Clean Architecture and MVVM patterns:

| Layer / Library | Technology | Description |
| :--- | :--- | :--- |
| **Language** | [Kotlin 2.4](https://kotlinlang.org/) | Modern concise language for Android development |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/) | Declarative UI toolkit with dynamic color support |
| **Architecture** | MVVM + Clean Architecture | Unidirectional data flow and clear separation of concerns |
| **Dependency Injection** | [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) | Standardized compile-time dependency injection framework |
| **Database** | [Room](https://developer.android.com/training/data-storage/room) | Local SQLite persistence library with schema migrations |
| **Preferences & Crypto** | [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) + [Security Crypto](https://developer.android.com/topic/security/data) | Reactive settings storage and encrypted credentials |
| **Asynchronous Programming** | [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-overview.html) | Asynchronous stream processing and state management |
| **Background Tasks** | [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | Reliable periodic background task scheduling |
| **Camera & Vision** | [CameraX](https://developer.android.com/training/camerax) + [ML Kit](https://developers.google.com/ml-kit) | Live camera feed and on-device text recognition for VIN scanning |
| **App Widgets** | [Android Glance](https://developer.android.com/jetpack/compose/glance) | Declarative Compose-based home screen app widgets |
| **Networking** | [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/) + [Gson](https://github.com/google/gson) | Type-safe HTTP client for NHTSA API & WebDAV communication |
| **Image Loading** | [Coil 3](https://coil-kt.github.io/coil/) | Kotlin-first image loading library for Compose |

---

## 📂 Project Structure

```text
Garage/
├── app/
│   ├── src/main/
│   │   ├── java/com/fearmikey/garage/
│   │   │   ├── data/             # Local Room DB, Entities, DAOs, Repositories, DTOs & Fuel Engine
│   │   │   │   ├── fuel/         # Fuel economy calculator & logs
│   │   │   │   ├── local/        # Room database, converters & preferences
│   │   │   │   ├── remote/       # Retrofit APIs (NHTSA VIN Decoder & Recalls)
│   │   │   │   ├── repository/   # Data repositories & WebDAV cloud sync
│   │   │   │   └── schedule/     # Maintenance schedule rules engine
│   │   │   ├── di/               # Hilt Dependency Injection modules
│   │   │   ├── notification/     # WorkManager background workers & notifications
│   │   │   ├── ui/               # Jetpack Compose UI screens, ViewModels & Material 3 theme
│   │   │   │   ├── components/   # Reusable UI components
│   │   │   │   ├── cost/         # Cost of ownership screen & analytics
│   │   │   │   ├── dashboard/    # Fleet overview dashboard
│   │   │   │   ├── fuel/         # Fuel economy & logging UI
│   │   │   │   ├── maintenance/  # Timeline, suggestions & PDF export
│   │   │   │   ├── recall/       # NHTSA safety recall screen
│   │   │   │   ├── reminder/     # Maintenance reminders UI
│   │   │   │   ├── settings/     # App settings & cloud backup options
│   │   │   │   ├── vehicle/      # Vehicle detail, specs, parts & VIN scanner
│   │   │   │   └── theme/        # Material Design 3 colors, typography & theme
│   │   │   ├── util/             # Helpers (VIN validator, formatters, unit converters)
│   │   │   └── widget/           # Android Glance home screen widget & receiver
│   │   └── res/                  # Android resources (Strings, Drawables, XML)
│   └── src/test/                 # Unit tests for repositories, viewmodels & rules engine
├── gradle/                       # Gradle wrapper & Version Catalog (libs.versions.toml)
├── CHANGELOG.md                  # Version release history
├── LICENSE                       # MIT License
├── README.md                     # Project documentation
└── TERMS.md                      # Terms of Service
```

---

## 🚀 Getting Started

### Prerequisites

* **Android Studio:** Ladybug (2024.2.1) or newer recommended
* **JDK:** Java 11 or higher
* **Android SDK:** Min SDK 34 (Android 14) / Target SDK 37

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/fearmikey/Garage.git
   cd Garage
   ```
2. **Open in Android Studio**
   Open the `Garage` project directory in Android Studio.
3. **Sync Gradle**
   Allow Gradle to download dependencies and sync project files.
4. **Run the Application**
   Select a connected device or emulator running Android 14+ (API 34+) and click **Run** (`Shift + F10`).

---

## 🧪 Building & Testing

### Assemble Debug Build
```bash
./gradlew assembleDebug
```

### Run Unit Tests
```bash
./gradlew test
```

---

## 📜 Changelog

See the [CHANGELOG.md](CHANGELOG.md) file for details on version updates and release history.

---

## ⚖️ License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for full details.

---

## 📄 Terms of Service

Please review our [Terms of Service](TERMS.md) before using the application.
