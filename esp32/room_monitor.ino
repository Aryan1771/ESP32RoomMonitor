#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>
#include "arduino_secrets.h"

// Uncomment this section when you add a DHT11 sensor later.
// #include <DHT.h>
// #define DHT_PIN 4
// #define DHT_TYPE DHT11
// DHT dht(DHT_PIN, DHT_TYPE);

namespace {
constexpr int LDR_PIN = 34;
constexpr unsigned long SLEEP_INTERVAL_MINUTES = 10;
constexpr char DEVICE_ID[] = "esp32-room-01";
constexpr unsigned long WIFI_TIMEOUT_MS = 20000;
constexpr long GMT_OFFSET_SECONDS = 0;
constexpr int DAYLIGHT_OFFSET_SECONDS = 0;
}

float readChipTemperatureC() {
  return temperatureRead();
}

int readLightRaw() {
  return analogRead(LDR_PIN);
}

int convertLightToPercent(int rawValue) {
  int percent = map(rawValue, 4095, 0, 0, 100);
  return constrain(percent, 0, 100);
}

bool connectToWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(SECRET_SSID, SECRET_PASS);

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < WIFI_TIMEOUT_MS) {
    delay(500);
    Serial.print(".");
  }

  return WiFi.status() == WL_CONNECTED;
}

String getIsoTimestamp() {
  struct tm timeInfo;
  if (!getLocalTime(&timeInfo, 5000)) {
    return "1970-01-01T00:00:00Z";
  }

  char timestampBuffer[25];
  strftime(timestampBuffer, sizeof(timestampBuffer), "%Y-%m-%dT%H:%M:%SZ", &timeInfo);
  return String(timestampBuffer);
}

bool postReading(float chipTemperatureC, int lightRaw, int lightPercent) {
  HTTPClient http;
  String endpoint = String(SECRET_API_URL) + "/update";

  http.begin(endpoint);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("x-api-key", SECRET_API_KEY);

  JsonDocument doc;
  doc["chipTemperatureC"] = chipTemperatureC;
  doc["lightRaw"] = lightRaw;
  doc["lightPercent"] = lightPercent;
  doc["deviceId"] = DEVICE_ID;
  doc["sleepIntervalMinutes"] = SLEEP_INTERVAL_MINUTES;
  doc["deviceSentAt"] = getIsoTimestamp();
  doc["dhtEnabled"] = false;
  doc["temperatureC"] = nullptr;
  doc["humidity"] = nullptr;

  // Future DHT11 support:
  // float temperatureC = dht.readTemperature();
  // float humidity = dht.readHumidity();
  // doc["temperatureC"] = temperatureC;
  // doc["humidity"] = humidity;
  // doc["dhtEnabled"] = true;

  String payload;
  serializeJson(doc, payload);

  int responseCode = http.POST(payload);
  Serial.printf("POST response code: %d\n", responseCode);

  if (responseCode > 0) {
    Serial.println(http.getString());
  }

  http.end();
  return responseCode > 0 && responseCode < 300;
}

void goToDeepSleep() {
  Serial.println("Entering deep sleep.");
  esp_sleep_enable_timer_wakeup(SLEEP_INTERVAL_MINUTES * 60ULL * 1000000ULL);

  // Some power banks shut off when the ESP32 current draw gets too low during sleep.
  // If that happens, use a USB keep-alive module or a small pulsed dummy load circuit.
  esp_deep_sleep_start();
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  analogReadResolution(12);

  // Uncomment this line when you add the DHT11 later.
  // dht.begin();

  Serial.println("Booting ESP32 room monitor.");

  float chipTemperatureC = readChipTemperatureC();
  int lightRaw = readLightRaw();
  int lightPercent = convertLightToPercent(lightRaw);

  Serial.printf("Chip temperature: %.2f C\n", chipTemperatureC);
  Serial.printf("Light raw: %d, light percent: %d%%\n", lightRaw, lightPercent);

  if (connectToWifi()) {
    Serial.println("Wi-Fi connected.");
    configTime(GMT_OFFSET_SECONDS, DAYLIGHT_OFFSET_SECONDS, "pool.ntp.org", "time.nist.gov");
    bool sent = postReading(chipTemperatureC, lightRaw, lightPercent);
    Serial.println(sent ? "Reading uploaded successfully." : "Upload failed.");
  } else {
    Serial.println("Wi-Fi connection failed.");
  }

  WiFi.disconnect(true);
  WiFi.mode(WIFI_OFF);
  goToDeepSleep();
}

void loop() {
}
