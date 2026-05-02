import os
import sqlite3
import threading
from contextlib import contextmanager
from typing import Iterable

DB_PATH = os.environ.get(
    "SCHEDULER_DB",
    os.path.join(os.path.dirname(os.path.dirname(__file__)), "scheduler.db"),
)

_lock = threading.Lock()


@contextmanager
def _conn():
    with _lock:
        conn = sqlite3.connect(DB_PATH)
        conn.row_factory = sqlite3.Row
        try:
            yield conn
            conn.commit()
        finally:
            conn.close()


def init_db() -> None:
    with _conn() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS calls (
                sync_id        TEXT PRIMARY KEY,
                contact_name   TEXT NOT NULL,
                phone_number   TEXT NOT NULL,
                local_datetime TEXT NOT NULL,
                zone_id        TEXT NOT NULL,
                notes          TEXT NOT NULL,
                recurrence     TEXT NOT NULL,
                triggered      INTEGER NOT NULL,
                tombstone      INTEGER NOT NULL DEFAULT 0,
                updated_at     INTEGER NOT NULL
            )
            """
        )
        conn.execute("CREATE INDEX IF NOT EXISTS calls_updated_at ON calls(updated_at)")


def _row_to_call(row: sqlite3.Row) -> dict:
    return {
        "syncId": row["sync_id"],
        "contactName": row["contact_name"],
        "phoneNumber": row["phone_number"],
        "localDateTime": row["local_datetime"],
        "zoneId": row["zone_id"],
        "notes": row["notes"],
        "recurrence": row["recurrence"],
        "triggered": bool(row["triggered"]),
        "tombstone": bool(row["tombstone"]),
        "updatedAt": row["updated_at"],
    }


def list_active_calls() -> list[dict]:
    with _conn() as conn:
        rows = conn.execute(
            "SELECT * FROM calls WHERE tombstone = 0 ORDER BY local_datetime ASC"
        ).fetchall()
    return [_row_to_call(r) for r in rows]


def list_changes_since(since_ms: int) -> list[dict]:
    with _conn() as conn:
        rows = conn.execute(
            "SELECT * FROM calls WHERE updated_at > ? ORDER BY updated_at ASC",
            (since_ms,),
        ).fetchall()
    return [_row_to_call(r) for r in rows]


def get_call(sync_id: str) -> dict | None:
    with _conn() as conn:
        row = conn.execute(
            "SELECT * FROM calls WHERE sync_id = ?", (sync_id,)
        ).fetchone()
    return _row_to_call(row) if row else None


def upsert_calls(changes: Iterable[dict]) -> None:
    """Apply incoming changes with last-write-wins on updated_at."""
    rows = list(changes)
    if not rows:
        return
    with _conn() as conn:
        for c in rows:
            existing = conn.execute(
                "SELECT updated_at FROM calls WHERE sync_id = ?", (c["syncId"],)
            ).fetchone()
            if existing and existing["updated_at"] >= c["updatedAt"]:
                continue
            conn.execute(
                """
                INSERT INTO calls (
                    sync_id, contact_name, phone_number, local_datetime, zone_id,
                    notes, recurrence, triggered, tombstone, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(sync_id) DO UPDATE SET
                    contact_name   = excluded.contact_name,
                    phone_number   = excluded.phone_number,
                    local_datetime = excluded.local_datetime,
                    zone_id        = excluded.zone_id,
                    notes          = excluded.notes,
                    recurrence     = excluded.recurrence,
                    triggered      = excluded.triggered,
                    tombstone      = excluded.tombstone,
                    updated_at     = excluded.updated_at
                """,
                (
                    c["syncId"],
                    c["contactName"],
                    c["phoneNumber"],
                    c["localDateTime"],
                    c["zoneId"],
                    c["notes"],
                    c["recurrence"],
                    1 if c["triggered"] else 0,
                    1 if c["tombstone"] else 0,
                    c["updatedAt"],
                ),
            )


def delete_call(sync_id: str, updated_at: int) -> bool:
    """Soft-delete a call (tombstone). Returns True if a row matched."""
    with _conn() as conn:
        cur = conn.execute(
            "UPDATE calls SET tombstone = 1, updated_at = ? "
            "WHERE sync_id = ? AND updated_at < ?",
            (updated_at, sync_id, updated_at),
        )
        return cur.rowcount > 0
