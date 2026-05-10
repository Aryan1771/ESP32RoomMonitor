# Android App

This Android app is built with Jetpack Compose, Material 3, and Retrofit.

## Features

- Fetches the latest room status from the backend
- Pull-to-refresh
- Three modern cards:
  - chip temperature
  - light level
  - device status
- Last updated timestamp

## Setup

Open `Constants.kt` and update:

```kotlin
object Constants {
    const val BASE_URL = "https://your-render-service.onrender.com/"
    const val API_KEY = "your-shared-secret"
}
```

## Notes

- The app calls `GET /status` and sends `x-api-key`.
- DHT11 ambient temperature and humidity are not displayed yet because the hardware is intentionally not enabled in this MVP.
