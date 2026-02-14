const state = {
  accessToken: "",
  refreshToken: ""
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

async function callApi(method, url, body, withAccessToken = false) {
  const headers = {
    "Content-Type": "application/json"
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

initWeekTabs();
initWeek1();
initWeek2();
renderAuthState();
