#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -d .venv ]; then
    python3 -m venv .venv
fi
source .venv/bin/activate

pip install -q --upgrade pip
pip install -q -r requirements.txt

HOST="${SCHEDULER_HOST:-0.0.0.0}"
PORT="${SCHEDULER_PORT:-8765}"

echo "Call Scheduler server starting on http://${HOST}:${PORT}"
exec python -m scheduler_server --host "$HOST" --port "$PORT"
