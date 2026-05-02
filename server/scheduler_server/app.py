import time
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field

from . import db


def now_ms() -> int:
    return int(time.time() * 1000)


class Call(BaseModel):
    syncId: str
    contactName: str = ""
    phoneNumber: str
    localDateTime: str
    zoneId: str
    notes: str = ""
    recurrence: str = "NONE"
    triggered: bool = False
    tombstone: bool = False
    updatedAt: int = Field(default_factory=now_ms)


class SyncRequest(BaseModel):
    since: int = 0
    changes: list[Call] = Field(default_factory=list)


class SyncResponse(BaseModel):
    serverTime: int
    changes: list[Call]


db.init_db()

app = FastAPI(title="Call Scheduler", version="1.0.0")

STATIC_DIR = Path(__file__).parent / "static"


@app.get("/api/calls")
def list_calls() -> list[Call]:
    return [Call(**c) for c in db.list_active_calls()]


@app.get("/api/calls/{sync_id}")
def get_call(sync_id: str) -> Call:
    row = db.get_call(sync_id)
    if not row:
        raise HTTPException(status_code=404, detail="Not found")
    return Call(**row)


@app.put("/api/calls/{sync_id}")
def upsert_call(sync_id: str, call: Call) -> Call:
    if call.syncId != sync_id:
        raise HTTPException(status_code=400, detail="syncId mismatch")
    call.updatedAt = now_ms()
    db.upsert_calls([call.model_dump()])
    refreshed = db.get_call(sync_id)
    assert refreshed is not None
    return Call(**refreshed)


@app.delete("/api/calls/{sync_id}")
def delete_call(sync_id: str) -> dict:
    ts = now_ms()
    existing = db.get_call(sync_id)
    if not existing:
        raise HTTPException(status_code=404, detail="Not found")
    tombstoned = {**existing, "tombstone": True, "updatedAt": ts}
    db.upsert_calls([tombstoned])
    return {"syncId": sync_id, "tombstone": True, "updatedAt": ts}


@app.post("/api/calls/sync")
def sync(req: SyncRequest) -> SyncResponse:
    server_now = now_ms()
    incoming = []
    for c in req.changes:
        # Stamp incoming changes with server time so updatedAt is monotonic
        # and clock-skew-tolerant. Clients should treat the response as authoritative.
        c_dict = c.model_dump()
        c_dict["updatedAt"] = server_now
        incoming.append(c_dict)
    db.upsert_calls(incoming)
    server_changes = db.list_changes_since(req.since)
    return SyncResponse(
        serverTime=server_now,
        changes=[Call(**c) for c in server_changes],
    )


@app.get("/health")
def health() -> dict:
    return {"ok": True, "serverTime": now_ms()}


# Static web UI mounted at / (must be last)
app.mount("/", StaticFiles(directory=STATIC_DIR, html=True), name="static")
