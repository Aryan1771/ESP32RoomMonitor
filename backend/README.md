# Backend

This is the Render-ready Node.js Express API for the ESP32 Room Monitor project.

## Endpoints

- `GET /ping` for Uptime Robot
- `POST /update` for the ESP32
- `GET /status` for the Android app

`POST /update` and `GET /status` both require the `x-api-key` header.

## Local Setup

1. Copy `.env.example` to `.env`
2. Set `API_KEY`
3. Run:

```bash
npm install
npm start
```

## Render Deployment

1. Create a new Web Service on Render from the `backend/` directory.
2. Set the build command to `npm install`.
3. Set the start command to `npm start`.
4. Add environment variables:

```env
API_KEY=your-shared-secret
PORT=10000
```

## Uptime Robot

Use your deployed `/ping` URL in Uptime Robot so the Render service stays warm.

## Persistence Upgrade

The current implementation uses in-memory storage for the latest reading.

Inside `server.js` there is a commented section showing where to replace that with:

- Vercel KV
- Supabase
