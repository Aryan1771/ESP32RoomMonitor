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

## Setup

Open `local.properties` in the `android/` folder and add:

```properties
roomMonitor.baseUrl=https://your-render-service.onrender.com/
roomMonitor.apiKey=your-shared-secret
```

## Notes

- The app calls `GET /status` and sends `x-api-key`.
- `local.properties` is ignored by Git, so your local API values stay out of the public repo.
- DHT11 ambient temperature and humidity are not displayed yet because the hardware is intentionally not enabled in this MVP.
