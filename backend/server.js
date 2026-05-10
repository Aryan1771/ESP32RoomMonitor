const express = require("express");
const cors = require("cors");
const dotenv = require("dotenv");

dotenv.config();

const app = express();
const port = process.env.PORT || 3000;
const apiKey = process.env.API_KEY;

app.use(cors());
app.use(express.json());

let currentStatus = {
  statusAvailable: false,
  message: "No sensor data yet",
  chipTemperatureC: null,
  lightRaw: null,
  lightPercent: null,
  temperatureC: null,
  humidity: null,
  deviceId: null,
  sleepIntervalMinutes: null,
  deviceSentAt: null,
  serverReceivedAt: null,
  dhtEnabled: false
};

function requireApiKey(req, res, next) {
  if (!apiKey) {
    return res.status(500).json({
      error: "API_KEY is not configured on the server"
    });
  }

  const providedKey = req.header("x-api-key");
  if (providedKey !== apiKey) {
    return res.status(401).json({ error: "Unauthorized" });
  }

  next();
}

function isValidNumber(value) {
  return typeof value === "number" && Number.isFinite(value);
}

function normalizePayload(body) {
  const {
    chipTemperatureC,
    lightRaw,
    lightPercent,
    deviceId,
    sleepIntervalMinutes,
    deviceSentAt,
    dhtEnabled,
    temperatureC = null,
    humidity = null
  } = body;

  if (
    !isValidNumber(chipTemperatureC) ||
    !Number.isInteger(lightRaw) ||
    !Number.isInteger(lightPercent) ||
    typeof deviceId !== "string" ||
    deviceId.trim().length === 0 ||
    !isValidNumber(sleepIntervalMinutes) ||
    typeof deviceSentAt !== "string" ||
    typeof dhtEnabled !== "boolean"
  ) {
    return null;
  }

  if (temperatureC !== null && !isValidNumber(temperatureC)) {
    return null;
  }

  if (humidity !== null && !isValidNumber(humidity)) {
    return null;
  }

  return {
    statusAvailable: true,
    message: "Latest sensor payload",
    chipTemperatureC,
    lightRaw,
    lightPercent,
    temperatureC,
    humidity,
    deviceId: deviceId.trim(),
    sleepIntervalMinutes,
    deviceSentAt,
    serverReceivedAt: new Date().toISOString(),
    dhtEnabled
  };
}

app.get("/ping", (_req, res) => {
  res.json({
    ok: true,
    service: "esp32-room-monitor-backend",
    timestamp: new Date().toISOString()
  });
});

app.post("/update", requireApiKey, (req, res) => {
  const normalized = normalizePayload(req.body);

  if (!normalized) {
    return res.status(400).json({
      error: "Invalid payload",
      requiredFields: [
        "chipTemperatureC",
        "lightRaw",
        "lightPercent",
        "deviceId",
        "sleepIntervalMinutes",
        "deviceSentAt",
        "dhtEnabled"
      ]
    });
  }

  currentStatus = normalized;

  // Persistence upgrade example:
  // 1. Replace the in-memory assignment above with a write to Vercel KV or Supabase.
  // 2. In /status, read the latest saved record instead of the local currentStatus object.
  // Example sketch:
  // await kv.set("room-monitor:latest", JSON.stringify(normalized));
  // or
  // await supabase.from("room_status").upsert({ id: 1, ...normalized });

  return res.json({
    ok: true,
    status: currentStatus
  });
});

app.get("/status", requireApiKey, (_req, res) => {
  res.json(currentStatus);
});

app.listen(port, () => {
  console.log(`ESP32 Room Monitor backend listening on port ${port}`);
});
