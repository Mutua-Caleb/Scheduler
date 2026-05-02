const $ = (sel) => document.querySelector(sel);

const listSection = $("#listSection");
const editorSection = $("#editorSection");
const callList = $("#callList");
const emptyMsg = $("#emptyMsg");
const editorTitle = $("#editorTitle");
const form = $("#callForm");
const statusEl = $("#status");
const deleteBtn = $("#deleteBtn");

let currentEditId = null;
let calls = [];

function uuid() {
    if (crypto && crypto.randomUUID) return crypto.randomUUID();
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === "x" ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}

function setStatus(msg, isError = false) {
    statusEl.textContent = msg;
    statusEl.style.color = isError ? "var(--danger)" : "var(--muted)";
    if (msg) {
        clearTimeout(setStatus._t);
        setStatus._t = setTimeout(() => (statusEl.textContent = ""), 3000);
    }
}

function populateZones() {
    const zoneSelect = $("#zoneId");
    let zones;
    try {
        zones = Intl.supportedValuesOf("timeZone");
    } catch {
        zones = ["UTC", Intl.DateTimeFormat().resolvedOptions().timeZone];
    }
    const local = Intl.DateTimeFormat().resolvedOptions().timeZone;
    zoneSelect.innerHTML = zones
        .map(
            (z) =>
                `<option value="${z}" ${
                    z === local ? "selected" : ""
                }>${z}</option>`
        )
        .join("");
}

function formatLocalDateTime(localDateTime, zoneId) {
    // localDateTime is "YYYY-MM-DDTHH:mm" — render in its stored zone.
    try {
        const [d, t] = localDateTime.split("T");
        const [y, mo, day] = d.split("-").map(Number);
        const [h, mi] = t.split(":").map(Number);
        const date = new Date(Date.UTC(y, mo - 1, day, h, mi));
        // We render the wall-clock as-is; the zone is shown beside it.
        return new Intl.DateTimeFormat(undefined, {
            weekday: "short",
            month: "short",
            day: "numeric",
            year: "numeric",
            hour: "numeric",
            minute: "2-digit",
            timeZone: "UTC",
        }).format(date);
    } catch {
        return localDateTime;
    }
}

function recurrenceLabel(value) {
    return {
        NONE: "Once",
        DAILY: "Every day",
        WEEKDAYS: "Weekdays",
        WEEKLY: "Every week",
    }[value] || value;
}

function renderList() {
    const visible = calls
        .filter((c) => !c.tombstone)
        .sort((a, b) => a.localDateTime.localeCompare(b.localDateTime));

    callList.innerHTML = "";
    if (visible.length === 0) {
        emptyMsg.classList.remove("hidden");
        return;
    }
    emptyMsg.classList.add("hidden");

    for (const call of visible) {
        const li = document.createElement("li");
        li.className = "call-item";
        li.tabIndex = 0;

        const title = document.createElement("h3");
        title.textContent = call.contactName || call.phoneNumber;
        const meta = document.createElement("div");
        meta.className = "meta";
        meta.textContent = `${formatLocalDateTime(
            call.localDateTime,
            call.zoneId
        )} • ${call.zoneId} • ${recurrenceLabel(call.recurrence)}`;

        li.appendChild(title);
        li.appendChild(meta);

        if (call.notes) {
            const notes = document.createElement("div");
            notes.className = "notes";
            notes.textContent = call.notes;
            li.appendChild(notes);
        }

        const open = () => openEditor(call);
        li.addEventListener("click", open);
        li.addEventListener("keydown", (e) => {
            if (e.key === "Enter") open();
        });

        callList.appendChild(li);
    }
}

async function fetchCalls() {
    const res = await fetch("/api/calls");
    if (!res.ok) {
        setStatus("Failed to load calls", true);
        return;
    }
    calls = await res.json();
    renderList();
}

function defaultLocalDateTime() {
    const d = new Date();
    d.setMinutes(0, 0, 0);
    d.setHours(d.getHours() + 1);
    const pad = (n) => String(n).padStart(2, "0");
    return {
        date: `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`,
        time: `${pad(d.getHours())}:${pad(d.getMinutes())}`,
    };
}

function openEditor(call) {
    currentEditId = call ? call.syncId : null;
    editorTitle.textContent = call ? "Edit call" : "New call";
    deleteBtn.classList.toggle("hidden", !call);

    if (call) {
        $("#contactName").value = call.contactName || "";
        $("#phoneNumber").value = call.phoneNumber;
        const [date, time] = call.localDateTime.split("T");
        $("#date").value = date;
        $("#time").value = time.slice(0, 5);
        $("#zoneId").value = call.zoneId;
        $("#recurrence").value = call.recurrence;
        $("#notes").value = call.notes || "";
    } else {
        const def = defaultLocalDateTime();
        $("#contactName").value = "";
        $("#phoneNumber").value = "";
        $("#date").value = def.date;
        $("#time").value = def.time;
        $("#zoneId").value = Intl.DateTimeFormat().resolvedOptions().timeZone;
        $("#recurrence").value = "NONE";
        $("#notes").value = "";
    }

    listSection.classList.add("hidden");
    editorSection.classList.remove("hidden");
    setTimeout(() => $("#contactName").focus(), 0);
}

function closeEditor() {
    listSection.classList.remove("hidden");
    editorSection.classList.add("hidden");
    currentEditId = null;
}

async function saveCall(e) {
    e.preventDefault();
    const localDateTime = `${$("#date").value}T${$("#time").value}:00`;
    const payload = {
        syncId: currentEditId || uuid(),
        contactName: $("#contactName").value.trim(),
        phoneNumber: $("#phoneNumber").value.trim(),
        localDateTime,
        zoneId: $("#zoneId").value,
        notes: $("#notes").value.trim(),
        recurrence: $("#recurrence").value,
        triggered: false,
        tombstone: false,
        updatedAt: Date.now(),
    };

    if (currentEditId) {
        const existing = calls.find((c) => c.syncId === currentEditId);
        if (existing) payload.triggered = existing.triggered;
    }

    const res = await fetch(`/api/calls/${payload.syncId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    });
    if (!res.ok) {
        setStatus("Save failed", true);
        return;
    }
    setStatus("Saved");
    await fetchCalls();
    closeEditor();
}

async function deleteCurrent() {
    if (!currentEditId) return;
    if (!confirm("Delete this scheduled call?")) return;
    const res = await fetch(`/api/calls/${currentEditId}`, { method: "DELETE" });
    if (!res.ok) {
        setStatus("Delete failed", true);
        return;
    }
    setStatus("Deleted");
    await fetchCalls();
    closeEditor();
}

function attachEvents() {
    $("#newBtn").addEventListener("click", () => openEditor(null));
    $("#cancelBtn").addEventListener("click", closeEditor);
    deleteBtn.addEventListener("click", deleteCurrent);
    form.addEventListener("submit", saveCall);

    document.addEventListener("keydown", (e) => {
        const inEditor = !editorSection.classList.contains("hidden");
        if (e.key === "n" && !inEditor && document.activeElement.tagName !== "INPUT") {
            e.preventDefault();
            openEditor(null);
        }
        if (e.key === "Escape" && inEditor) {
            closeEditor();
        }
    });
}

(async () => {
    populateZones();
    attachEvents();
    await fetchCalls();
    setInterval(fetchCalls, 15000);
})();
