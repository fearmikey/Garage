# 🚗 Garage

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Garage is a comprehensive vehicle management application designed to help owners track maintenance schedules, monitor vehicle health, and maintain accurate service records. The application provides a centralized platform for managing multiple vehicles, ensuring that important maintenance tasks are never overlooked.

---

## ✨ Features

### 🚘 Vehicle Management
* **Comprehensive Profiles:** Maintain detailed information for each vehicle, including specifications and unique identifiers.
* **VIN Scanning:** Utilize integrated camera capabilities to scan Vehicle Identification Numbers (VIN) and automatically retrieve vehicle information.
* **Vehicle Catalog:** A structured dashboard for easy access to all vehicles in your collection.

### 🛠️ Maintenance Tracking
* **Service Records:** Log and track all maintenance activities, including date, type, and details of the service performed.
* **Maintenance Timeline:** A visual representation of the maintenance history for each vehicle.
* **Smart Suggestions:** Receive intelligent maintenance recommendations based on vehicle usage and historical data.
* **Automated Reminders:** Stay informed with proactive notifications for upcoming maintenance tasks and service intervals.

### 🔒 Data Management & Security
* **Local Database:** All vehicle and maintenance data is stored securely within a local Room database, ensuring privacy and offline accessibility.
* **Backup and Restore:** Robust mechanisms to export and import your vehicle data, facilitating easy transitions between devices and preventing data loss.

---

## 🛠️ Technical Stack

The app is built using modern Android development best practices:

| Layer | Technology |
| :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) |
| **Local Persistence** | [Room Persistence Library](https://developer.android.com/training/data-storage/room) |
| **Dependency Injection** | [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) |
| **Asynchronous** | [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-overview.html) |
| **Background Tasks** | [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) |
| **Camera** | [CameraX](https://developer.android.com/training/camerax) |
| **Networking** | [Retrofit](https://square.github.io/retrofit/) |

---

## 🚀 Installation

To build and run the Garage application:

1. **Clone the repository**
   ```bash
   git clone https://github.com/fearmikey/Garage.git
   ```
2. **Open in Android Studio**
   Open the project folder in the latest version of Android Studio.
3. **Sync Gradle**
   Wait for the project to sync with the Gradle files.
4. **Run the App**
   Select a supported Android device or emulator and click **Run**.

---

## ⚖️ License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

## 📜 Terms of Service

Please review our [Terms of Service](https://github.com/fearmikey/Garage/blob/main/TERMS.md) before using the application.
