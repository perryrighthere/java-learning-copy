const state = {
  accessToken: "",
  refreshToken: ""
};

const week4State = {
  streams: {
    a: null,
    b: null
  }
};

function pretty(data) {
  if (typeof data === "string") {
    return data;
  }
  return JSON.stringify(data, null, 2);
}

function setOutput(id, value) {
  document.getElementById(id).textContent = pretty(value);
}

async function callApi(method, url, body, withAccessToken = false, extraHeaders = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...extraHeaders
  };

  if (withAccessToken && state.accessToken) {
    headers.Authorization = `Bearer ${state.accessToken}`;
  }

  const response = await fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  });

  const raw = await response.text();
  let payload = raw;
  try {
    payload = raw ? JSON.parse(raw) : {};
  } catch (err) {
    payload = raw;
  }

  return {
    status: response.status,
    ok: response.ok,
    payload
  };
}

function initWeekTabs() {
  const tabs = document.querySelectorAll(".week-tab");
  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      const selectedWeek = tab.dataset.week;
      tabs.forEach((button) => {
        const active = button.dataset.week === selectedWeek;
        button.classList.toggle("active", active);
        button.setAttribute("aria-selected", String(active));
      });

      document.querySelectorAll(".week-panel").forEach((panel) => {
        panel.classList.toggle("active", panel.id === selectedWeek);
      });
    });
  });
}

function initWeek1() {
  document.getElementById("week1-health-btn").addEventListener("click", async () => {
    try {
      const result = await callApi("GET", "/api/health");
      setOutput("week1-health-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week1-health-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week1-user-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const body = {
      email: document.getElementById("week1-email").value,
      displayName: document.getElementById("week1-display-name").value,
      password: document.getElementById("week1-password").value
    };

    try {
      const result = await callApi("POST", "/api/v1/users", body);
      setOutput("week1-user-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week1-user-output", `Request failed: ${error.message}`);
    }
  });
}

function renderAuthState() {
  const label = state.accessToken ? "Authenticated" : "Not logged in";
  document.getElementById("week2-auth-state").textContent = label;
}

function initWeek2() {
  document.querySelectorAll(".preset-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.getElementById("week2-email").value = btn.dataset.email;
      document.getElementById("week2-password").value = "Password123!";
    });
  });

  document.getElementById("week2-login-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const body = {
      email: document.getElementById("week2-email").value,
      password: document.getElementById("week2-password").value
    };

    try {
      const result = await callApi("POST", "/api/v1/auth/login", body);
      if (result.ok) {
        state.accessToken = result.payload.accessToken;
        state.refreshToken = result.payload.refreshToken;
      }
      renderAuthState();
      setOutput("week2-auth-output", {
        status: result.status,
        body: result.payload,
        tokenPreview: state.accessToken ? `${state.accessToken.slice(0, 30)}...` : null
      });
    } catch (error) {
      setOutput("week2-auth-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week2-refresh-btn").addEventListener("click", async () => {
    if (!state.refreshToken) {
      setOutput("week2-auth-output", "Login first to get a refresh token.");
      return;
    }

    try {
      const result = await callApi("POST", "/api/v1/auth/refresh", { refreshToken: state.refreshToken });
      if (result.ok) {
        state.accessToken = result.payload.accessToken;
        state.refreshToken = result.payload.refreshToken;
      }
      renderAuthState();
      setOutput("week2-auth-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week2-auth-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week2-board-btn").addEventListener("click", async () => {
    const boardId = document.getElementById("week2-board-id").value;
    try {
      const result = await callApi("GET", `/api/v1/boards/${boardId}`, null, true);
      setOutput("week2-board-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week2-board-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week2-members-btn").addEventListener("click", async () => {
    const boardId = document.getElementById("week2-board-id").value;
    try {
      const result = await callApi("GET", `/api/v1/boards/${boardId}/members`, null, true);
      setOutput("week2-board-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week2-board-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week2-member-write-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const boardId = document.getElementById("week2-board-id").value;
    const body = {
      userId: Number(document.getElementById("week2-target-user").value),
      role: document.getElementById("week2-role").value
    };

    try {
      const result = await callApi("POST", `/api/v1/boards/${boardId}/members`, body, true);
      setOutput("week2-board-output", {
        status: result.status,
        expected: "Guest user should receive 403 here",
        body: result.payload
      });
    } catch (error) {
      setOutput("week2-board-output", `Request failed: ${error.message}`);
    }
  });
}

function setCardState(cardPayload) {
  if (!cardPayload || !cardPayload.id) {
    return;
  }
  document.getElementById("week3-card-id").value = cardPayload.id;
  document.getElementById("week3-column-id").value = cardPayload.columnId;
  document.getElementById("week3-card-version").value = cardPayload.version;
  document.getElementById("week3-card-title").value = cardPayload.title ?? "";
  document.getElementById("week3-card-description").value = cardPayload.description ?? "";
}

function initWeek3() {
  document.getElementById("week3-search-boards-btn").addEventListener("click", async () => {
    const query = document.getElementById("week3-board-query").value.trim();
    const qPart = query ? `&q=${encodeURIComponent(query)}` : "";
    try {
      const result = await callApi("GET", `/api/v1/boards?page=0&size=10${qPart}`, null, true);
      setOutput("week3-board-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-board-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-list-columns-btn").addEventListener("click", async () => {
    const boardId = document.getElementById("week3-board-id").value;
    try {
      const result = await callApi("GET", `/api/v1/boards/${boardId}/columns?page=0&size=20`, null, true);
      setOutput("week3-board-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-board-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-create-column-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const boardId = document.getElementById("week3-board-id").value;
    const body = { name: document.getElementById("week3-column-name").value };

    try {
      const result = await callApi("POST", `/api/v1/boards/${boardId}/columns`, body, true);
      if (result.ok && result.payload.id) {
        document.getElementById("week3-column-id").value = result.payload.id;
      }
      setOutput("week3-board-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-board-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-create-card-btn").addEventListener("click", async () => {
    const columnId = document.getElementById("week3-column-id").value;
    const body = {
      title: document.getElementById("week3-card-title").value,
      description: document.getElementById("week3-card-description").value
    };

    try {
      const result = await callApi("POST", `/api/v1/columns/${columnId}/cards`, body, true);
      if (result.ok) {
        setCardState(result.payload);
      }
      setOutput("week3-card-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-card-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-get-card-btn").addEventListener("click", async () => {
    const columnId = document.getElementById("week3-column-id").value;
    const cardId = document.getElementById("week3-card-id").value;

    if (!cardId) {
      setOutput("week3-card-output", "Create a card first or input an existing card ID.");
      return;
    }

    try {
      const result = await callApi("GET", `/api/v1/columns/${columnId}/cards/${cardId}`, null, true);
      if (result.ok) {
        setCardState(result.payload);
      }
      setOutput("week3-card-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-card-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-update-card-btn").addEventListener("click", async () => {
    const columnId = document.getElementById("week3-column-id").value;
    const cardId = document.getElementById("week3-card-id").value;
    const version = Number(document.getElementById("week3-card-version").value);

    if (!cardId || Number.isNaN(version)) {
      setOutput("week3-card-output", "Card ID and version are required.");
      return;
    }

    const body = {
      title: document.getElementById("week3-card-title").value,
      description: document.getElementById("week3-card-description").value,
      version
    };

    try {
      const result = await callApi("PUT", `/api/v1/columns/${columnId}/cards/${cardId}`, body, true);
      if (result.ok) {
        setCardState(result.payload);
      }
      setOutput("week3-card-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-card-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-stale-update-btn").addEventListener("click", async () => {
    const columnId = document.getElementById("week3-column-id").value;
    const cardId = document.getElementById("week3-card-id").value;
    const currentVersion = Number(document.getElementById("week3-card-version").value);
    const staleVersion = currentVersion - 1;

    if (!cardId || Number.isNaN(currentVersion) || staleVersion < 0) {
      setOutput("week3-card-output", "Need a card with version >= 1. Create and update once first.");
      return;
    }

    const body = {
      title: `${document.getElementById("week3-card-title").value} (stale)`,
      description: document.getElementById("week3-card-description").value,
      version: staleVersion
    };

    try {
      const result = await callApi("PUT", `/api/v1/columns/${columnId}/cards/${cardId}`, body, true);
      setOutput("week3-card-output", {
        status: result.status,
        expected: "Should be 409 conflict with latest payload",
        body: result.payload
      });
    } catch (error) {
      setOutput("week3-card-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-delete-card-btn").addEventListener("click", async () => {
    const columnId = document.getElementById("week3-column-id").value;
    const cardId = document.getElementById("week3-card-id").value;

    if (!cardId) {
      setOutput("week3-card-output", "Card ID is required.");
      return;
    }

    try {
      const result = await callApi("DELETE", `/api/v1/columns/${columnId}/cards/${cardId}`, null, true);
      setOutput("week3-card-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-card-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-create-comment-btn").addEventListener("click", async () => {
    const cardId = document.getElementById("week3-card-id").value;
    if (!cardId) {
      setOutput("week3-card-output", "Card ID is required.");
      return;
    }

    const body = { body: document.getElementById("week3-comment-body").value };
    try {
      const result = await callApi("POST", `/api/v1/cards/${cardId}/comments`, body, true);
      setOutput("week3-card-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-card-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week3-list-comments-btn").addEventListener("click", async () => {
    const cardId = document.getElementById("week3-card-id").value;
    if (!cardId) {
      setOutput("week3-card-output", "Card ID is required.");
      return;
    }
    try {
      const result = await callApi("GET", `/api/v1/cards/${cardId}/comments?page=0&size=20`, null, true);
      setOutput("week3-card-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week3-card-output", `Request failed: ${error.message}`);
    }
  });
}

function week4ClientElements(clientKey) {
  const upper = clientKey.toUpperCase();
  return {
    stateId: `week4-client-${clientKey}-state`,
    outputId: `week4-client-${clientKey}-output`,
    label: `Client ${upper}`
  };
}

function setWeek4ClientState(clientKey, text, mode = "offline") {
  const { stateId } = week4ClientElements(clientKey);
  const node = document.getElementById(stateId);
  node.textContent = text;
  node.classList.remove("offline", "connecting", "live");
  node.classList.add(mode);
}

function appendWeek4Log(clientKey, payload) {
  const { outputId } = week4ClientElements(clientKey);
  const target = document.getElementById(outputId);
  const stamp = new Date().toLocaleTimeString();
  const line = `[${stamp}] ${pretty(payload)}`;
  target.textContent = `${target.textContent}\n${line}`.trim();
  target.scrollTop = target.scrollHeight;
}

function parseSseEventChunk(chunk) {
  const lines = chunk.split(/\r?\n/);
  let id = "";
  let event = "message";
  const dataLines = [];

  lines.forEach((line) => {
    if (line.startsWith("id:")) {
      id = line.slice(3).trim();
      return;
    }
    if (line.startsWith("event:")) {
      event = line.slice(6).trim();
      return;
    }
    if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trim());
    }
  });

  const dataRaw = dataLines.join("\n");
  let data = dataRaw;
  try {
    data = dataRaw ? JSON.parse(dataRaw) : {};
  } catch (error) {
    data = dataRaw;
  }

  return { id, event, data };
}

async function closeWeek4Stream(clientKey, silent = false) {
  const stream = week4State.streams[clientKey];
  if (!stream) {
    setWeek4ClientState(clientKey, `${week4ClientElements(clientKey).label}: Disconnected`, "offline");
    return;
  }

  stream.controller.abort();
  try {
    if (stream.reader) {
      await stream.reader.cancel();
    }
  } catch (error) {
    // no-op: reader may already be closed
  }

  week4State.streams[clientKey] = null;
  setWeek4ClientState(clientKey, `${week4ClientElements(clientKey).label}: Disconnected`, "offline");
  if (!silent) {
    appendWeek4Log(clientKey, "Disconnected");
  }
}

async function openWeek4Stream(clientKey) {
  if (!state.accessToken) {
    setOutput("week4-action-output", "Week 4 requires login first (use Week 2 panel).");
    return;
  }

  const boardId = Number(document.getElementById("week4-board-id").value);
  const clientAId = document.getElementById("week4-client-a-id").value.trim();
  const clientBId = document.getElementById("week4-client-b-id").value.trim();
  const clientId = clientKey === "a" ? clientAId : clientBId;

  if (!boardId || !clientId) {
    setOutput("week4-action-output", "Board ID and client IDs are required.");
    return;
  }

  await closeWeek4Stream(clientKey, true);

  const controller = new AbortController();
  week4State.streams[clientKey] = { controller };
  setWeek4ClientState(clientKey, `${week4ClientElements(clientKey).label}: Connecting`, "connecting");
  appendWeek4Log(clientKey, `Connecting to /api/v1/boards/${boardId}/events as ${clientId}`);

  try {
    const response = await fetch(`/api/v1/boards/${boardId}/events?clientId=${encodeURIComponent(clientId)}`, {
      method: "GET",
      headers: {
        Accept: "text/event-stream",
        Authorization: `Bearer ${state.accessToken}`
      },
      signal: controller.signal
    });

    if (!response.ok || !response.body) {
      const text = await response.text();
      setWeek4ClientState(clientKey, `${week4ClientElements(clientKey).label}: Disconnected`, "offline");
      appendWeek4Log(clientKey, { status: response.status, body: text || "(empty response)" });
      week4State.streams[clientKey] = null;
      return;
    }

    setWeek4ClientState(clientKey, `${week4ClientElements(clientKey).label}: Connected`, "live");

    const reader = response.body.getReader();
    week4State.streams[clientKey].reader = reader;
    const decoder = new TextDecoder("utf-8");
    let buffer = "";

    while (!controller.signal.aborted) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      buffer = buffer.replace(/\r\n/g, "\n");

      let breakIndex = buffer.indexOf("\n\n");
      while (breakIndex >= 0) {
        const rawEvent = buffer.slice(0, breakIndex).trim();
        buffer = buffer.slice(breakIndex + 2);

        if (rawEvent) {
          const parsed = parseSseEventChunk(rawEvent);
          appendWeek4Log(clientKey, parsed);
        }

        breakIndex = buffer.indexOf("\n\n");
      }
    }
  } catch (error) {
    if (!controller.signal.aborted) {
      appendWeek4Log(clientKey, `Stream error: ${error.message}`);
    }
  } finally {
    const activeStream = week4State.streams[clientKey];
    if (activeStream && activeStream.controller === controller) {
      week4State.streams[clientKey] = null;
      setWeek4ClientState(clientKey, `${week4ClientElements(clientKey).label}: Disconnected`, "offline");
      if (!controller.signal.aborted) {
        appendWeek4Log(clientKey, "Stream ended");
      }
    }
  }
}

async function createWeek4Card(sourceClientKey) {
  if (!state.accessToken) {
    setOutput("week4-action-output", "Week 4 requires login first (use Week 2 panel).");
    return;
  }

  const columnId = Number(document.getElementById("week4-column-id").value);
  const clientAId = document.getElementById("week4-client-a-id").value.trim();
  const clientBId = document.getElementById("week4-client-b-id").value.trim();
  const clientId = sourceClientKey === "a" ? clientAId : clientBId;
  const titlePrefix = document.getElementById("week4-card-title-prefix").value.trim() || "Week4 Realtime Card";

  if (!columnId || !clientId) {
    setOutput("week4-action-output", "Column ID and client IDs are required.");
    return;
  }

  const body = {
    title: `${titlePrefix} ${new Date().toLocaleTimeString()}`,
    description: `Created from Week 4 demo as ${clientId}`
  };

  try {
    const result = await callApi(
      "POST",
      `/api/v1/columns/${columnId}/cards`,
      body,
      true,
      { "X-Client-Id": clientId }
    );
    setOutput("week4-action-output", {
      status: result.status,
      sourceClient: clientId,
      expected: "Only the opposite client stream should receive card.created.",
      body: result.payload
    });
  } catch (error) {
    setOutput("week4-action-output", `Request failed: ${error.message}`);
  }
}

function initWeek4() {
  setOutput("week4-client-a-output", "Client A events will appear here.");
  setOutput("week4-client-b-output", "Client B events will appear here.");
  setOutput("week4-action-output", "Connect both clients, then POST a card from one client.");
  setWeek4ClientState("a", "Client A: Disconnected", "offline");
  setWeek4ClientState("b", "Client B: Disconnected", "offline");

  document.getElementById("week4-connect-a-btn").addEventListener("click", () => openWeek4Stream("a"));
  document.getElementById("week4-connect-b-btn").addEventListener("click", () => openWeek4Stream("b"));
  document.getElementById("week4-connect-both-btn").addEventListener("click", () => {
    openWeek4Stream("a");
    openWeek4Stream("b");
  });
  document.getElementById("week4-disconnect-all-btn").addEventListener("click", async () => {
    await closeWeek4Stream("a");
    await closeWeek4Stream("b");
  });

  document.getElementById("week4-create-from-a-btn").addEventListener("click", () => createWeek4Card("a"));
  document.getElementById("week4-create-from-b-btn").addEventListener("click", () => createWeek4Card("b"));
  document.getElementById("week4-clear-logs-btn").addEventListener("click", () => {
    setOutput("week4-client-a-output", "Client A events will appear here.");
    setOutput("week4-client-b-output", "Client B events will appear here.");
  });

  window.addEventListener("beforeunload", () => {
    closeWeek4Stream("a", true);
    closeWeek4Stream("b", true);
  });
}

initWeekTabs();
initWeek1();
initWeek2();
initWeek3();
initWeek4();
renderAuthState();
