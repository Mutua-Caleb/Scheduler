# Call Scheduler — desktop server

A FastAPI server that hosts:

1. A web-based desktop UI for editing scheduled calls (open `http://localhost:8765/`).
2. A REST API the Android app syncs against.

The Android app polls this server periodically and on launch, mirroring the
server's state into its local Room database and re-arming alarms.

## Run

```bash
./run.sh
```

The script creates a `.venv`, installs dependencies, and starts uvicorn on
`0.0.0.0:8765`. Override the host or port:

```bash
SCHEDULER_HOST=127.0.0.1 SCHEDULER_PORT=9000 ./run.sh
```

The SQLite database is stored at `server/scheduler.db` by default; override
with `SCHEDULER_DB=/some/path.db`.

## Connect the phone

1. Find your laptop's LAN IP (`ip addr` / `ifconfig`). It's usually
   `192.168.x.x`.
2. In the Android app, tap the **Settings** icon (top of the list screen) and
   enter `http://192.168.x.x:8765`.
3. Tap **Sync now** once to verify connectivity. The phone will poll
   automatically every 15 minutes thereafter (or on launch).

If your phone needs to reach the server while you're not at home, run the
server somewhere reachable (a small VM) or expose it via Tailscale / ngrok.

## API summary

| Method | Path                      | Description                              |
|--------|---------------------------|------------------------------------------|
| GET    | `/api/calls`              | List active (non-deleted) calls          |
| GET    | `/api/calls/{syncId}`     | Get one call                             |
| PUT    | `/api/calls/{syncId}`     | Create or update a call                  |
| DELETE | `/api/calls/{syncId}`     | Soft-delete a call (tombstones the row)  |
| POST   | `/api/calls/sync`         | Bidirectional delta sync (used by phone) |
| GET    | `/health`                 | Health probe                             |

The sync endpoint accepts `{ "since": <ms>, "changes": [...] }` and returns
`{ "serverTime": <ms>, "changes": [...] }`. Last-write-wins on `updatedAt`.

## Schema

```jsonc
{
  "syncId":        "uuid string",        // primary cross-device id
  "contactName":   "string",
  "phoneNumber":   "string",
  "localDateTime": "YYYY-MM-DDTHH:mm:ss", // wall-clock time
  "zoneId":        "IANA tz, e.g. America/Los_Angeles",
  "notes":         "string",
  "recurrence":    "NONE | DAILY | WEEKDAYS | WEEKLY",
  "triggered":     false,
  "tombstone":     false,
  "updatedAt":     1735603200000          // server-stamped ms epoch
}
```
