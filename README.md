# Garage

Garage is a comprehensive vehicle management application designed to help owners track maintenance schedules, monitor vehicle health, and maintain accurate service records. The application provides a centralized platform for managing multiple vehicles, ensuring that important maintenance tasks are never overlooked.

## Features

### Vehicle Management
* **Comprehensive Profiles:** Maintain detailed information for each vehicle, including specifications and unique identifiers.
* **VIN Scanning:** Utilize integrated camera capabilities to scan Vehicle Identification Numbers (VIN) and automatically retrieve vehicle information.

* **Vehicle Catalog:** A structured dashboard for easy access to all vehicles in your collection.

### Maintenance Tracking
* **Service Records:** Log and track all maintenance activities, including date, type, and details of the service performed.
* **Maintenance Timeline:** A visual representation of the maintenance history for each vehicle.
* **Smart Suggestions:** Receive intelligent maintenance recommendations based on vehicle usage and historical data.
* **Automated Reminders:** Stay informed with proactive notifications for upcoming maintenance tasks and service intervals.

### Data Management & Security
* **Local Database:** All vehicle and maintenance data is stored securely within a local Room database, ensuring privacy and offline accessibility.
* **Backup and Restore:** Robust mechanisms to export and import your vehicle data, facilitating easy transitions between devices and preventing data loss.

## Technical Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose
* **Local Persistence:** Room Persistence Library
* **Dependency Injection:** Hilt
* **Asynchronous Processing:** Kotlin Coroutines & Flow
* **Background Tasks:** WorkManager
* **Image Processing:** CameraX
* **Networking:** Retrofit (for VIN decoding)

## Installation

To build and run the Garage application:

1. Clone the repository to your local machine.
2. Open the project in Android Studio.
3. Ensure you have the latest Android SDK and Build Tools installed.
4. Sync the project with Gradle files.
5. Select a supported Android device or emulator.
6. Click **Run**.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
