# Android App

This Android app is built with Jetpack Compose, Material 3, and Retrofit.

## Features

- Fetches the latest room status from the backend
- Pull-to-refresh
- Three modern cards:
  - chip temperature
  - light level with a local history graph
  - device status
- Last updated timestamp
- App icon and splash screen
- In-app ESP32 Wi-Fi provisioning over the ESP32 setup hotspot

## Setup

Open `local.properties` in the `android/` folder and add:

```properties
roomMonitor.baseUrl=https://your-render-service.onrender.com/
roomMonitor.apiKey=your-shared-secret
```

## Notes

- The app calls `GET /status` and sends `x-api-key`.
- `local.properties` is ignored by Git, so your local API values stay out of the public repo.
- ESP32 setup uses the local hotspot at `http://192.168.4.1/`, so the app can provision Wi-Fi without exposing personal credentials in Git.
- DHT11 ambient temperature and humidity are not displayed yet because the hardware is intentionally not enabled in this MVP.

## ESP32 Device Setup Flow

1. Power on the ESP32 with no saved Wi-Fi
2. Connect your phone to `ESP32-RoomMonitor-Setup`
3. Open the app and tap `Device Setup`
4. Check the ESP32 connection, scan nearby Wi-Fi names, and send the home SSID/password
5. The ESP32 restarts and joins your home Wi-Fi
