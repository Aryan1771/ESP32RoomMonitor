# ESP32 Firmware

This sketch is designed for a DOIT ESP32 board and works immediately with:

- an LDR
- the ESP32 internal chip temperature

DHT11 support is scaffolded but commented out until you buy the sensor.

## Files

- `room_monitor.ino`
- `arduino_secrets.example.h`

Create a real `arduino_secrets.h` file from the example before uploading.

## Arduino Libraries

Install these libraries in Arduino IDE:

- `ArduinoJson`

The active MVP does not require the DHT library yet because that code is commented out.

## Wiring

### Current MVP

| Component | ESP32 Pin | Notes |
| --- | --- | --- |
| LDR analog output | GPIO 34 | Use a voltage divider and keep voltage within ESP32 ADC range |

### Future DHT11 Upgrade

When you buy a DHT11 later, use:

| Component | ESP32 Pin | Notes |
| --- | --- | --- |
| DHT11 data | GPIO 4 | Update if you choose another pin |

Then uncomment the clearly marked DHT block in `room_monitor.ino`.

## Secret File

Create `arduino_secrets.h`:

```cpp
#define SECRET_SSID "your-wifi"
#define SECRET_PASS "your-password"
#define SECRET_API_URL "https://your-render-service.onrender.com"
#define SECRET_API_KEY "your-shared-secret"
```

## Power Notes

The device sends one reading every 10 minutes and then enters deep sleep using `esp_sleep_enable_timer_wakeup`.

If your power bank shuts off during sleep because current draw is too low, add a USB keep-alive module or a simple pulsed load circuit.
