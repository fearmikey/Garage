# Privacy Policy

**Last Updated: September 19, 2026**

## 1. Introduction
Garage ("we", "our", or "us") is committed to protecting your privacy. This Privacy Policy explains our practices regarding the collection, use, and disclosure of information when you use the Garage Android application.

## 2. Data Collection and Usage
**We do not collect, transmit, or store your personal data.**

Garage is designed as an offline-first, privacy-respecting application. All data you enter into the application—including vehicle details, maintenance logs, fuel records, and any other personal information—is stored locally on your device using a local SQLite database (Room).

### Camera Permission
The app requests access to your device's camera strictly for the purpose of scanning Vehicle Identification Numbers (VINs) and taking photos of your vehicles. All image processing (such as text recognition for VIN scanning) is performed on-device. Images and extracted text are stored locally on your device and are never transmitted to any external servers.

### Network Access
The app requires network access for the following specific, limited purposes:
1.  **NHTSA API:** To decode VINs and look up safety recalls. Only the VIN and vehicle make/model/year are sent to the official National Highway Traffic Safety Administration (NHTSA) public API.
2.  **WebDAV Cloud Backup:** If you explicitly enable and configure the WebDAV backup feature, the app will connect to the server you provide to sync your encrypted backup files. We do not host or have access to these servers.

We do not use any third-party analytics, telemetry, or advertising SDKs (such as Google Analytics or AdMob).

## 3. Data Retention
Because your data is stored locally on your device, you have complete control over its retention. You can delete individual records or clear all app data at any time through your device's system settings. Uninstalling the application will also remove all associated local data.

## 4. Third-Party Services
While we do not integrate proprietary third-party tracking, the app does interact with the following external service based on your actions:
*   **NHTSA API:** When you decode a VIN or check for recalls, the app communicates with the NHTSA. Their privacy policy governs the data they receive during these requests.

## 5. Changes to This Privacy Policy
We may update this Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page and updating the "Last Updated" date.

## 6. Contact Us
If you have any questions or suggestions about this Privacy Policy, please open an issue on our GitHub repository.
