# 🚗 Garage

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0.1-blue.svg)](CHANGELOG.md)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-34-brightgreen.svg)](https://developer.android.com/about/versions/14)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-purple.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)

**Garage** is a modern, comprehensive vehicle management application for Android designed to help owners track maintenance schedules, monitor vehicle health, and maintain accurate service records. The application provides a centralized platform for managing multiple vehicles, ensuring that important maintenance tasks are never overlooked.

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack & Architecture](#-tech-stack--architecture)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Building & Testing](#-building--testing)
- [Changelog](#-changelog)
- [License](#-license)
- [Terms of Service](#-terms-of-service)

---

## ✨ Features

### 🚘 Vehicle Management
* **Comprehensive Profiles:** Maintain detailed information for each vehicle, including specifications, fuel type, mileage, and unique identifiers.
* **VIN Scanning:** Utilize integrated camera capabilities powered by **CameraX** and **ML Kit** to scan Vehicle Identification Numbers (VIN) and automatically retrieve vehicle specs.
* **Vehicle Catalog:** A structured dashboard for quick access to all vehicles in your garage.

### 🛠️ Maintenance Tracking
* **Service Records:** Log and track all maintenance activities, including dates, mileage, cost, category, and service details.
* **Maintenance Timeline:** A visual representation of historical and upcoming maintenance tasks for each vehicle.
* **Smart Suggestions:** Intelligent recommendations based on vehicle mileage, age, and historical usage patterns.
* **Automated Reminders:** Background notifications via **WorkManager** to keep you informed of upcoming maintenance intervals.

### 🔒 Data Management & Security
* **Local Persistence:** Secure offline data storage using a local **Room** database with migrations and schema versioning.
* **Backup and Restore:** Robust JSON import/export mechanisms to transfer vehicle data across devices safely.

---

## 🛠️ Technical Stack & Architecture

Built with modern Android development standards following Clean Architecture and MVVM design patterns:

| Layer / Library | Technology |
| :--- | :--- |
| **Language** | [Kotlin 2.4](https://kotlinlang.org/) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/) |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection** | [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) |
| **Database** | [Room Persistence Library](https://developer.android.com/training/data-storage/room) |
| **Preferences** | [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) |
| **Asynchronous & Flows** | [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-overview.html) |
| **Background Scheduling** | [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) |
| **Camera & Vision** | [CameraX](https://developer.android.com/training/camerax) + [ML Kit](https://developers.google.com/ml-kit) |
| **Networking** | [Retrofit](https://square.github.io/retrofit/) + [Gson](https://github.com/google/gson) |
| **Image Loading** | [Coil 3](https://coil-kt.github.io/coil/) |

---

## 📂 Project Structure

```text
Garage/
├── app/
│   ├── src/main/java/com/fearmikey/garage/
│   │   ├── data/             # Local database, entities, DAOs, repositories & network DTOs
│   │   ├── di/               # Hilt Dependency Injection modules
│   │   ├── notification/     # WorkManager background reminder workers & notifications
│   │   ├── ui/               # Jetpack Compose UI screens, components, theme & ViewModels
│   │   └── util/             # Utility classes and validators (e.g. VIN validator)
│   └── src/test/             # Unit tests for repositories, viewmodels & rules engine
├── gradle/                   # Gradle wrapper & Version Catalog (libs.versions.toml)
├── CHANGELOG.md              # Version release history
├── LICENSE                   # MIT License
├── README.md                 # Project documentation
└── TERMS.md                  # Terms of Service
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
   Open the `Garage` project folder in Android Studio.
3. **Sync Gradle**
   Allow Gradle to download dependencies and sync the project.
4. **Run the Application**
   Select a supported Android device or emulator running Android 14+ and click **Run** (`Shift + F10`).

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
