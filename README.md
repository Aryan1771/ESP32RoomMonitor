# ESP32 Room Monitor

An IoT room monitoring system built for a public GitHub repository with clean secret handling.

This MVP works today with:

- ESP32 internal chip temperature
- LDR-based light sensing
- Render-hosted Node.js backend
- Jetpack Compose Android app

Ambient temperature and humidity support through a DHT11 is already scaffolded in the ESP32 code, but intentionally commented out until you have the sensor.

## Architecture

```mermaid
flowchart LR
    A["ESP32 + LDR"] -->|"POST /update every 10 minutes"| B["Render Express API"]
    B -->|"GET /status"| C["Kotlin Android App"]
    D["Uptime Robot"] -->|"GET /ping"| B
```

## Repository Structure

```text
ESP32RoomMonitor/
├─ .gitignore
├─ README.md
├─ backend/
├─ esp32/
└─ android/
```

## Current Sensor Payload

```json
{
  "chipTemperatureC": 34.8,
  "lightRaw": 1460,
  "lightPercent": 64,
  "deviceId": "esp32-room-01",
  "sleepIntervalMinutes": 10,
  "deviceSentAt": "2026-05-10T08:30:00Z",
  "serverReceivedAt": "2026-05-10T08:30:02Z",
  "dhtEnabled": false,
  "temperatureC": null,
  "humidity": null
}
```

## Setup Order

1. Configure and run the backend in `backend/`.
2. Deploy the backend to Render and copy the public URL.
3. Add your Wi-Fi credentials and backend URL to `esp32/arduino_secrets.h`.
4. Flash the ESP32 sketch from `esp32/room_monitor.ino`.
5. Set the same backend URL and API key in the Android `Constants` object.
6. Run the Android app and pull to refresh.

## Step-by-Step Implementation

### 1. Backend on Render

1. Open `backend/.env.example` and create a local `.env`.
2. Set the same shared `API_KEY` you plan to use in the ESP32 and Android app.
3. Run the backend locally with `npm install` and `npm start`.
4. Test:
   - `GET /ping`
   - `POST /update` with `x-api-key`
   - `GET /status` with `x-api-key`
5. Deploy the `backend/` folder to Render.

### 2. ESP32 Firmware

1. Copy `esp32/arduino_secrets.example.h` to `esp32/arduino_secrets.h`.
2. Fill in:
   - Wi-Fi SSID
   - Wi-Fi password
   - Render base URL
   - shared API key
3. Wire the LDR to GPIO 34 through a safe voltage divider.
4. Flash `esp32/room_monitor.ino`.
5. Confirm the serial monitor shows:
   - Wi-Fi connected
   - chip temperature
   - light reading
   - successful POST
6. The ESP32 then sleeps for 10 minutes to protect the power bank battery.

### 3. Android App

1. Open the `android/` folder in Android Studio.
2. Update `Constants.kt` with your Render URL and API key.
3. Sync Gradle and run the app on a phone or emulator.
4. Use pull-to-refresh to fetch the latest reading.
5. Confirm the app shows:
   - chip temperature card
   - light card
   - device status card
   - last updated timestamp

### 4. Later DHT11 Upgrade

1. Install the DHT library in Arduino IDE.
2. Wire the DHT11 to the documented pin in `esp32/README.md`.
3. Uncomment the DHT11 blocks in `esp32/room_monitor.ino`.
4. Change `dhtEnabled` to `true`.
5. Optionally extend the Android UI to show ambient temperature and humidity.

## Security Notes

- `backend/.env` is ignored by Git.
- `esp32/arduino_secrets.h` is ignored by Git.
- `android` uses a `Constants` object for demo simplicity.

For a hackathon demo that is fine, but for production you would move Android secrets out of the client.

## Future DHT11 Upgrade

When you buy the DHT11 later:

1. Wire the DHT11 to the documented GPIO pin in `esp32/README.md`.
2. Install the DHT library in Arduino IDE.
3. Uncomment the marked DHT11 code blocks in `esp32/room_monitor.ino`.
4. Change `dhtEnabled` to `true`.
5. Optionally show ambient temperature and humidity in the Android UI.
