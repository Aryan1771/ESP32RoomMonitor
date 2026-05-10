#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Preferences.h>
#include <WebServer.h>
#include <time.h>
#include "arduino_secrets.h"

// Uncomment this section when you add a DHT11 sensor later.
// #include <DHT.h>
// #define DHT_PIN 4
// #define DHT_TYPE DHT11
// DHT dht(DHT_PIN, DHT_TYPE);

namespace {
constexpr int LDR_PIN = 34;
constexpr unsigned long SLEEP_INTERVAL_SECONDS = 30;
constexpr unsigned long UPLOAD_INTERVAL_MS = SLEEP_INTERVAL_SECONDS * 1000UL;
constexpr char DEVICE_ID[] = "esp32-room-01";
constexpr unsigned long WIFI_TIMEOUT_MS = 20000;
constexpr long GMT_OFFSET_SECONDS = 0;
constexpr int DAYLIGHT_OFFSET_SECONDS = 0;
constexpr bool DEBUG_CONTINUOUS_MODE = true;
constexpr char PREFERENCES_NAMESPACE[] = "wifi-config";
constexpr char PREFERENCES_SSID_KEY[] = "ssid";
constexpr char PREFERENCES_PASS_KEY[] = "pass";
constexpr char SETUP_AP_SSID[] = "ESP32-RoomMonitor-Setup";
constexpr char SETUP_AP_PASSWORD[] = "";
const IPAddress LOCAL_AP_IP(192, 168, 4, 1);
}

Preferences wifiPreferences;
WebServer provisioningServer(80);
unsigned long lastUploadAtMs = 0;
bool shouldRestartAfterProvisioning = false;

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

void sendJsonResponse(int statusCode, const JsonDocument& doc) {
  String response;
  serializeJson(doc, response);
  provisioningServer.sendHeader("Access-Control-Allow-Origin", "*");
  provisioningServer.sendHeader("Access-Control-Allow-Headers", "Content-Type");
  provisioningServer.send(statusCode, "application/json", response);
}

String normalizeSsid(const String& value) {
  String trimmed = value;
  trimmed.trim();
  return trimmed;
}

bool loadStoredCredentials(String& ssid, String& password) {
  wifiPreferences.begin(PREFERENCES_NAMESPACE, true);
  ssid = wifiPreferences.getString(PREFERENCES_SSID_KEY, "");
  password = wifiPreferences.getString(PREFERENCES_PASS_KEY, "");
  wifiPreferences.end();

  ssid = normalizeSsid(ssid);
  return !ssid.isEmpty();
}

bool loadFallbackCredentials(String& ssid, String& password) {
  ssid = normalizeSsid(String(SECRET_SSID));
  password = String(SECRET_PASS);
  return !ssid.isEmpty();
}

bool resolveWifiCredentials(String& ssid, String& password) {
  if (loadStoredCredentials(ssid, password)) {
    return true;
  }

  return loadFallbackCredentials(ssid, password);
}

void saveProvisionedCredentials(const String& ssid, const String& password) {
  wifiPreferences.begin(PREFERENCES_NAMESPACE, false);
  wifiPreferences.putString(PREFERENCES_SSID_KEY, normalizeSsid(ssid));
  wifiPreferences.putString(PREFERENCES_PASS_KEY, password);
  wifiPreferences.end();
}

String getIsoTimestamp() {
  struct tm timeInfo;
  if (!getLocalTime(&timeInfo, 5000)) {
    return "2000-01-01T00:00:00Z";
  }

  char timestampBuffer[25];
  strftime(timestampBuffer, sizeof(timestampBuffer), "%Y-%m-%dT%H:%M:%SZ", &timeInfo);
  return String(timestampBuffer);
}

bool connectToWifi() {
  String ssid;
  String password;

  if (!resolveWifiCredentials(ssid, password)) {
    Serial.println("No Wi-Fi credentials saved yet.");
    return false;
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid.c_str(), password.c_str());
  Serial.printf("Connecting to Wi-Fi SSID: %s\n", ssid.c_str());

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < WIFI_TIMEOUT_MS) {
    delay(500);
    Serial.print(".");
  }

  return WiFi.status() == WL_CONNECTED;
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
  doc["sleepIntervalMinutes"] = SLEEP_INTERVAL_SECONDS / 60.0;
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

bool uploadReadingCycle() {
  float chipTemperatureC = readChipTemperatureC();
  int lightRaw = readLightRaw();
  int lightPercent = convertLightToPercent(lightRaw);

  Serial.printf("Chip temperature: %.2f C\n", chipTemperatureC);
  Serial.printf("Light raw: %d, light percent: %d%%\n", lightRaw, lightPercent);

  if (!connectToWifi()) {
    Serial.println("Wi-Fi connection failed or credentials are missing.");
    WiFi.disconnect(true, true);
    WiFi.mode(WIFI_OFF);
    return false;
  }

  Serial.println("Wi-Fi connected.");
  configTime(GMT_OFFSET_SECONDS, DAYLIGHT_OFFSET_SECONDS, "pool.ntp.org", "time.nist.gov");
  bool sent = postReading(chipTemperatureC, lightRaw, lightPercent);
  Serial.println(sent ? "Reading uploaded successfully." : "Upload failed.");

  WiFi.disconnect(true, true);
  WiFi.mode(WIFI_OFF);
  return true;
}

void handleProvisioningStatus() {
  JsonDocument doc;
  doc["ok"] = true;
  doc["mode"] = "provisioning";
  doc["deviceId"] = DEVICE_ID;
  doc["apSsid"] = SETUP_AP_SSID;
  doc["apPasswordRequired"] = String(SETUP_AP_PASSWORD).length() >= 8;
  doc["provisioningUrl"] = "http://192.168.4.1/";
  sendJsonResponse(200, doc);
}

void handleProvisioningScan() {
  JsonDocument doc;
  JsonArray networks = doc["networks"].to<JsonArray>();

  int networkCount = WiFi.scanNetworks();
  if (networkCount < 0) {
    doc["ok"] = false;
    doc["message"] = "Wi-Fi scan failed";
    sendJsonResponse(500, doc);
    return;
  }

  doc["ok"] = true;
  for (int index = 0; index < networkCount; ++index) {
    String ssid = normalizeSsid(WiFi.SSID(index));
    if (ssid.isEmpty()) {
      continue;
    }

    bool alreadyAdded = false;
    for (JsonVariant existing : networks) {
      if (existing.as<String>() == ssid) {
        alreadyAdded = true;
        break;
      }
    }

    if (!alreadyAdded) {
      networks.add(ssid);
    }
  }

  WiFi.scanDelete();
  sendJsonResponse(200, doc);
}

void handleProvisioningConfigure() {
  if (!provisioningServer.hasArg("plain")) {
    JsonDocument doc;
    doc["ok"] = false;
    doc["message"] = "Missing JSON body";
    sendJsonResponse(400, doc);
    return;
  }

  JsonDocument requestDoc;
  DeserializationError error = deserializeJson(requestDoc, provisioningServer.arg("plain"));
  if (error) {
    JsonDocument doc;
    doc["ok"] = false;
    doc["message"] = "Invalid JSON body";
    sendJsonResponse(400, doc);
    return;
  }

  String ssid = normalizeSsid(requestDoc["ssid"] | "");
  String password = String(requestDoc["password"] | "");

  if (ssid.isEmpty()) {
    JsonDocument doc;
    doc["ok"] = false;
    doc["message"] = "SSID is required";
    sendJsonResponse(400, doc);
    return;
  }

  saveProvisionedCredentials(ssid, password);

  JsonDocument doc;
  doc["ok"] = true;
  doc["message"] = "Credentials saved. Restarting ESP32.";
  sendJsonResponse(200, doc);

  shouldRestartAfterProvisioning = true;
}

void handleProvisioningOptions() {
  provisioningServer.sendHeader("Access-Control-Allow-Origin", "*");
  provisioningServer.sendHeader("Access-Control-Allow-Headers", "Content-Type");
  provisioningServer.sendHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  provisioningServer.send(204);
}

void setupProvisioningServer() {
  provisioningServer.on("/status", HTTP_GET, handleProvisioningStatus);
  provisioningServer.on("/scan", HTTP_GET, handleProvisioningScan);
  provisioningServer.on("/configure", HTTP_POST, handleProvisioningConfigure);
  provisioningServer.on("/status", HTTP_OPTIONS, handleProvisioningOptions);
  provisioningServer.on("/scan", HTTP_OPTIONS, handleProvisioningOptions);
  provisioningServer.on("/configure", HTTP_OPTIONS, handleProvisioningOptions);
  provisioningServer.begin();
}

void startProvisioningMode() {
  Serial.println("Starting provisioning hotspot mode.");
  WiFi.disconnect(true, true);
  WiFi.mode(WIFI_AP_STA);

  bool apStarted = String(SETUP_AP_PASSWORD).length() >= 8
                       ? WiFi.softAP(SETUP_AP_SSID, SETUP_AP_PASSWORD)
                       : WiFi.softAP(SETUP_AP_SSID);

  if (!apStarted) {
    Serial.println("Failed to start provisioning hotspot.");
    return;
  }

  Serial.printf("Connect your phone to the Wi-Fi network: %s\n", SETUP_AP_SSID);
  Serial.println("Then open the Android app setup screen and use Scan Nearby Wi-Fi.");
  Serial.print("Provisioning server IP: ");
  Serial.println(WiFi.softAPIP());

  setupProvisioningServer();

  while (true) {
    provisioningServer.handleClient();

    if (shouldRestartAfterProvisioning) {
      delay(1000);
      ESP.restart();
    }

    delay(10);
  }
}

void goToDeepSleep() {
  Serial.println("Entering deep sleep.");
  esp_sleep_enable_timer_wakeup(SLEEP_INTERVAL_SECONDS * 1000000ULL);

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
  Serial.println(DEBUG_CONTINUOUS_MODE
                     ? "Debug continuous mode enabled. Device will stay awake and post on an interval."
                     : "Deep sleep mode enabled. setup() runs again after every wake-up.");

  if (uploadReadingCycle()) {
    lastUploadAtMs = millis();

    if (!DEBUG_CONTINUOUS_MODE) {
      goToDeepSleep();
    }
    return;
  }

  startProvisioningMode();
}

void loop() {
  if (!DEBUG_CONTINUOUS_MODE) {
    return;
  }

  if (millis() - lastUploadAtMs >= UPLOAD_INTERVAL_MS) {
    if (uploadReadingCycle()) {
      lastUploadAtMs = millis();
    }
  }

  delay(250);
}
