const state = {
  accessToken: "",
  refreshToken: "",
  currentEmail: "",
  currentWeek: "week1"
};

const week4State = {
  streams: {
    a: null,
    b: null
  }
};

const weekMeta = {
  week1: {
    label: "Week 1",
    title: "Skeleton, validation, and domain model",
    summary: "Boot the service, validate requests cleanly, and prove the schema is migration-driven from the start.",
    tags: ["Flyway", "JPA", "ProblemDetail"],
    proof: [
      "Health and user creation prove the service is live before auth is introduced.",
      "Core entities are modeled early so later weeks extend instead of rework.",
      "Generated API docs anchor the project as a real service, not a toy example."
    ],
    nextStep: "Create a user and verify the health endpoint."
  },
  week2: {
    label: "Week 2",
    title: "JWT auth, refresh tokens, and board RBAC",
    summary: "Show that identity, token flow, and board-level permissions are enforced before any serious collaboration features arrive.",
    tags: ["JWT", "Refresh", "RBAC"],
    proof: [
      "Seeded roles make it easy to demonstrate admin, member, and guest behavior.",
      "Protected board endpoints show the difference between authentication and authorization.",
      "Refresh flow keeps the demo close to production token lifecycles."
    ],
    nextStep: "Log in as Alice or Guest, then compare read and write permissions."
  },
  week3: {
    label: "Week 3",
    title: "CRUD depth, search, pagination, and optimistic locking",
    summary: "Move beyond scaffolding and show the service can manage real board data with safe update semantics.",
    tags: ["CRUD", "Pagination", "409 Conflict"],
    proof: [
      "Boards, columns, cards, and comments all run through typed DTOs and service logic.",
      "Search and paging keep list endpoints realistic for growing board data.",
      "Stale version handling demonstrates concurrency awareness before realtime is added."
    ],
    nextStep: "Create a card, fetch its version, and trigger a stale update conflict."
  },
  week4: {
    label: "Week 4",
    title: "Realtime board events with SSE and echo suppression",
    summary: "Two clients can observe the same board live, while the writing client avoids receiving its own echoed event.",
    tags: ["SSE", "Redis", "Echo Suppression"],
    proof: [
      "Board-scoped event streams make collaboration visible immediately in class.",
      "Redis-backed fan-out keeps the transport model close to a scalable deployment.",
      "Client IDs prove that realtime delivery can still respect UX details."
    ],
    nextStep: "Connect both clients, then create a card from one side and inspect the opposite stream."
  },
  week5: {
    label: "Week 5",
    title: "Transactional drag/drop with guided conflict recovery",
    summary: "The project now demonstrates collaboration rules, not just collaboration transport: ordering gaps, bulk reorder, and user-guided retries.",
    tags: ["Position Gaps", "Transactions", "Retry Guidance"],
    proof: [
      "Column reorder persists a full drag/drop result in one transaction.",
      "Card moves are neighbor-aware, so the API reflects actual UI drag semantics.",
      "Conflict responses return retry guidance instead of an opaque 409."
    ],
    nextStep: "Load a card snapshot, move it between neighbors, then force a stale retry."
  }
};

const showcaseBlueprint = {
  sourceColumnName: "Inbox",
  targetColumnName: "Doing",
  doneColumnName: "Done",
  movingCardTitle: "Demo: User Signup Flow",
  targetCardATitle: "Demo: Realtime Relay",
  targetCardBTitle: "Demo: Conflict Guidance",
  commentBody: "Created from the guided showcase to prove card comments work."
};

const showcaseState = {
  user: null,
  board: null,
  columns: [],
  cards: [],
  comment: null
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

function setTextIfPresent(id, value) {
  const node = document.getElementById(id);
  if (node) {
    node.textContent = value;
  }
}

function renderWeekCollection(containerId, className, items) {
  const container = document.getElementById(containerId);
  if (!container) {
    return;
  }

  container.replaceChildren();
  items.forEach((item) => {
    const node = document.createElement(container.tagName === "UL" ? "li" : "span");
    node.className = className;
    node.textContent = item;
    container.appendChild(node);
  });
}

function getInputValue(id) {
  const node = document.getElementById(id);
  return node ? node.value : "";
}

function setInputValue(id, value) {
  const node = document.getElementById(id);
  if (node && value !== undefined && value !== null) {
    node.value = value;
  }
}

function toNumericPosition(value) {
  if (typeof value === "number") {
    return value;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? 0 : parsed;
}

function sortByPosition(items) {
  return [...items].sort((left, right) => {
    const positionDelta = toNumericPosition(left.position) - toNumericPosition(right.position);
    if (positionDelta !== 0) {
      return positionDelta;
    }
    return (left.id ?? 0) - (right.id ?? 0);
  });
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

function switchWeek(selectedWeek) {
  state.currentWeek = selectedWeek;

  const tabs = document.querySelectorAll(".week-tab");
  tabs.forEach((button) => {
    const active = button.dataset.week === selectedWeek;
    button.classList.toggle("active", active);
    button.setAttribute("aria-selected", String(active));
  });

  document.querySelectorAll(".week-panel").forEach((panel) => {
    panel.classList.toggle("active", panel.id === selectedWeek);
  });

  renderWeekSpotlight();
}

function renderWeekSpotlight() {
  const meta = weekMeta[state.currentWeek];
  const weekNumber = Number.parseInt(state.currentWeek.replace("week", ""), 10) || 1;
  const progressPercent = Math.max(20, Math.min(100, (weekNumber / 5) * 100));

  setTextIfPresent("spotlight-week-label", meta.label);
  setTextIfPresent("spotlight-title", meta.title);
  setTextIfPresent("spotlight-summary", meta.summary);
  setTextIfPresent("spotlight-progress-copy", `Delivery progress: ${meta.label} of 5.`);
  setTextIfPresent("active-week-name", meta.label);
  setTextIfPresent("next-step-copy", meta.nextStep);
  renderWeekCollection("spotlight-tags", "tag-chip", meta.tags);
  renderWeekCollection("spotlight-points", "", meta.proof);

  const fill = document.getElementById("spotlight-progress");
  if (fill) {
    fill.style.width = `${progressPercent}%`;
  }
}

function initWeekTabs() {
  const tabs = document.querySelectorAll(".week-tab");
  tabs.forEach((tab) => {
    tab.addEventListener("click", () => switchWeek(tab.dataset.week));
  });
}

function initHeroActions() {
  document.querySelectorAll("[data-target-week]").forEach((node) => {
    node.addEventListener("click", () => switchWeek(node.dataset.targetWeek));
  });
}

function generateShowcaseDefaults() {
  const token = Date.now().toString().slice(-6);
  const timeLabel = new Date().toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  });

  return {
    email: `demo.${token}@kanban.local`,
    displayName: `Demo Student ${token.slice(-3)}`,
    password: "Password123!",
    boardName: `Showcase Board ${timeLabel}`
  };
}

function syncShowcaseIdentityIntoWeekForms() {
  const email = getInputValue("showcase-email");
  const displayName = getInputValue("showcase-display-name");
  const password = getInputValue("showcase-password");

  setInputValue("week1-email", email);
  setInputValue("week1-display-name", displayName);
  setInputValue("week1-password", password);
  setInputValue("week2-email", email);
  setInputValue("week2-password", password);
}

function applyShowcaseDefaults(defaults = generateShowcaseDefaults()) {
  setInputValue("showcase-email", defaults.email);
  setInputValue("showcase-display-name", defaults.displayName);
  setInputValue("showcase-password", defaults.password);
  setInputValue("showcase-board-name", defaults.boardName);
  syncShowcaseIdentityIntoWeekForms();
}

function cardsForColumn(columnId) {
  return sortByPosition(showcaseState.cards.filter((card) => card.columnId === columnId));
}

function findShowcaseColumn(name) {
  return showcaseState.columns.find((column) => column.name === name) ?? null;
}

function findShowcaseCard(title) {
  return showcaseState.cards.find((card) => card.title === title) ?? null;
}

function resolveShowcaseScenario() {
  const sourceColumn = findShowcaseColumn(showcaseBlueprint.sourceColumnName) ?? showcaseState.columns[0] ?? null;
  const targetColumn = findShowcaseColumn(showcaseBlueprint.targetColumnName) ?? showcaseState.columns[1] ?? null;
  const doneColumn = findShowcaseColumn(showcaseBlueprint.doneColumnName) ?? showcaseState.columns[2] ?? null;
  const movingCard = findShowcaseCard(showcaseBlueprint.movingCardTitle);
  const targetCardA = findShowcaseCard(showcaseBlueprint.targetCardATitle);
  const targetCardB = findShowcaseCard(showcaseBlueprint.targetCardBTitle);

  return {
    sourceColumn,
    targetColumn,
    doneColumn,
    movingCard,
    targetCardA,
    targetCardB
  };
}

function syncShowcaseStateToWeekForms() {
  syncShowcaseIdentityIntoWeekForms();

  if (showcaseState.board) {
    setInputValue("week2-board-id", showcaseState.board.id);
    setInputValue("week3-board-id", showcaseState.board.id);
    setInputValue("week4-board-id", showcaseState.board.id);
    setInputValue("week5-board-id", showcaseState.board.id);
  }

  const scenario = resolveShowcaseScenario();
  const sourceColumn = scenario.sourceColumn;
  const targetColumn = scenario.targetColumn;
  const movingCard = scenario.movingCard;
  const targetCardA = scenario.targetCardA;
  const targetCardB = scenario.targetCardB;

  if (sourceColumn) {
    setInputValue("week3-column-id", sourceColumn.id);
    setInputValue("week5-source-column-id", sourceColumn.id);
  }

  if (targetColumn) {
    setInputValue("week4-column-id", targetColumn.id);
    setInputValue("week5-target-column-id", targetColumn.id);
  }

  if (movingCard) {
    setInputValue("week3-card-id", movingCard.id);
    setInputValue("week3-card-version", movingCard.version);
    setInputValue("week3-card-title", movingCard.title);
    setInputValue("week3-card-description", movingCard.description ?? "");
    setInputValue("week5-card-id", movingCard.id);
    setInputValue("week5-card-version", movingCard.version);
  }

  if (targetCardA) {
    setInputValue("week5-previous-card-id", targetCardA.id);
  }

  if (targetCardB) {
    setInputValue("week5-next-card-id", targetCardB.id);
  }
}

function setShowcaseOutput(value) {
  setOutput("showcase-output", value);
}

function renderShowcaseBoardView() {
  const container = document.getElementById("showcase-board-view");
  if (!container) {
    return;
  }

  container.replaceChildren();

  if (!showcaseState.board) {
    const emptyLane = document.createElement("div");
    emptyLane.className = "lane lane--empty";

    const title = document.createElement("h3");
    title.textContent = "No board loaded";
    const copy = document.createElement("p");
    copy.textContent = "Create a board and seed cards to see the collaboration story rendered here.";

    emptyLane.append(title, copy);
    container.appendChild(emptyLane);
    return;
  }

  showcaseState.columns.forEach((column) => {
    const lane = document.createElement("section");
    lane.className = "lane";

    const laneHeader = document.createElement("div");
    laneHeader.className = "lane-header";

    const heading = document.createElement("h3");
    heading.textContent = column.name;
    const badge = document.createElement("span");
    badge.className = "lane-badge";
    badge.textContent = `Column #${column.id}`;
    laneHeader.append(heading, badge);
    lane.appendChild(laneHeader);

    const cards = cardsForColumn(column.id);
    if (!cards.length) {
      const emptyCopy = document.createElement("p");
      emptyCopy.className = "support-copy";
      emptyCopy.textContent = "No cards in this lane yet.";
      lane.appendChild(emptyCopy);
    }

    cards.forEach((card) => {
      const cardNode = document.createElement("article");
      cardNode.className = "lane-card";

      const title = document.createElement("strong");
      title.textContent = card.title;

      const meta = document.createElement("span");
      meta.textContent = `Card #${card.id} | version ${card.version}`;

      const description = document.createElement("span");
      description.textContent = card.description || "No description";

      const chip = document.createElement("code");
      chip.textContent = `position ${card.position}`;

      cardNode.append(title, meta, description, chip);
      lane.appendChild(cardNode);
    });

    container.appendChild(lane);
  });
}

function renderShowcaseWorkspace() {
  const scenario = resolveShowcaseScenario();
  const moveReady = scenario.movingCard && scenario.targetColumn && scenario.targetCardA && scenario.targetCardB;

  setTextIfPresent(
    "showcase-user-summary",
    showcaseState.user ? `${showcaseState.user.displayName} (#${showcaseState.user.id})` : "Not created yet"
  );
  setTextIfPresent(
    "showcase-board-summary",
    showcaseState.board ? `${showcaseState.board.name} (#${showcaseState.board.id})` : "No board yet"
  );
  setTextIfPresent("showcase-column-summary", String(showcaseState.columns.length));
  setTextIfPresent("showcase-card-summary", String(showcaseState.cards.length));
  setTextIfPresent(
    "showcase-comment-summary",
    showcaseState.comment
      ? `${showcaseState.comment.authorDisplayName}: ${showcaseState.comment.body.slice(0, 32)}${showcaseState.comment.body.length > 32 ? "..." : ""}`
      : "Not added"
  );
  setTextIfPresent(
    "showcase-move-summary",
    moveReady
      ? `Move "${scenario.movingCard.title}" into ${scenario.targetColumn.name}`
      : "Seed the demo board to unlock move actions"
  );

  renderShowcaseBoardView();
}

function requireShowcaseAuth() {
  if (state.accessToken) {
    return true;
  }
  setShowcaseOutput("Login first so the demo can create boards, cards, and comments.");
  return false;
}

function requireShowcaseBoard() {
  if (showcaseState.board) {
    return true;
  }
  setShowcaseOutput("Create a board first or run the full setup.");
  return false;
}

async function loadCurrentUserIntoShowcase() {
  if (!state.accessToken) {
    return null;
  }
  const result = await callApi("GET", "/api/v1/users/me", null, true);
  if (result.ok) {
    showcaseState.user = result.payload;
    renderShowcaseWorkspace();
  }
  return result;
}

async function createShowcaseUser(options = {}) {
  const quiet = options.quiet === true;
  syncShowcaseIdentityIntoWeekForms();

  const body = {
    email: getInputValue("showcase-email"),
    displayName: getInputValue("showcase-display-name"),
    password: getInputValue("showcase-password")
  };

  const result = await callApi("POST", "/api/v1/users", body);
  if (result.ok) {
    showcaseState.user = result.payload;
    renderShowcaseWorkspace();
  }

  if (!quiet) {
    setShowcaseOutput({
      status: result.status,
      action: "create-user",
      body: result.payload
    });
  }

  return result;
}

async function loginShowcaseUser(options = {}) {
  const quiet = options.quiet === true;
  syncShowcaseIdentityIntoWeekForms();

  const body = {
    email: getInputValue("showcase-email"),
    password: getInputValue("showcase-password")
  };

  const result = await callApi("POST", "/api/v1/auth/login", body);
  if (result.ok) {
    state.accessToken = result.payload.accessToken;
    state.refreshToken = result.payload.refreshToken;
    state.currentEmail = body.email;
    renderAuthState();
    await loadCurrentUserIntoShowcase();
  }

  if (!quiet) {
    setShowcaseOutput({
      status: result.status,
      action: "login",
      tokenPreview: result.ok ? `${result.payload.accessToken.slice(0, 24)}...` : null,
      body: result.payload
    });
  }

  return result;
}

async function createShowcaseBoard(options = {}) {
  const quiet = options.quiet === true;
  if (!requireShowcaseAuth()) {
    return { ok: false, status: 401, payload: "Not authenticated" };
  }

  const result = await callApi(
    "POST",
    "/api/v1/boards",
    { name: getInputValue("showcase-board-name") },
    true
  );

  if (result.ok) {
    showcaseState.board = result.payload;
    showcaseState.columns = [];
    showcaseState.cards = [];
    showcaseState.comment = null;
    syncShowcaseStateToWeekForms();
    renderShowcaseWorkspace();
  }

  if (!quiet) {
    setShowcaseOutput({
      status: result.status,
      action: "create-board",
      body: result.payload
    });
  }

  return result;
}

async function seedShowcaseBoard(options = {}) {
  const quiet = options.quiet === true;
  if (!requireShowcaseAuth() || !requireShowcaseBoard()) {
    return { ok: false, status: 400, payload: "Missing auth or board" };
  }

  const boardId = showcaseState.board.id;
  const existingColumns = await callApi("GET", `/api/v1/boards/${boardId}/columns?page=0&size=5`, null, true);
  if (existingColumns.ok && existingColumns.payload.totalElements > 0) {
    if (!quiet) {
      setShowcaseOutput("This board already has columns. Create a fresh board before seeding the demo scenario again.");
    }
    return { ok: false, status: 409, payload: "Board already seeded" };
  }

  const createdColumns = [];
  for (const name of [showcaseBlueprint.sourceColumnName, showcaseBlueprint.targetColumnName, showcaseBlueprint.doneColumnName]) {
    const result = await callApi("POST", `/api/v1/boards/${boardId}/columns`, { name }, true);
    if (!result.ok) {
      if (!quiet) {
        setShowcaseOutput({ status: result.status, action: "create-column", body: result.payload });
      }
      return result;
    }
    createdColumns.push(result.payload);
  }

  const sourceColumn = createdColumns.find((column) => column.name === showcaseBlueprint.sourceColumnName);
  const targetColumn = createdColumns.find((column) => column.name === showcaseBlueprint.targetColumnName);
  const doneColumn = createdColumns.find((column) => column.name === showcaseBlueprint.doneColumnName);

  const cardSpecs = [
    {
      columnId: sourceColumn.id,
      title: showcaseBlueprint.movingCardTitle,
      description: "Start here: this card will be moved during the collaboration demo."
    },
    {
      columnId: targetColumn.id,
      title: showcaseBlueprint.targetCardATitle,
      description: "First anchor card in the target lane for move demos."
    },
    {
      columnId: targetColumn.id,
      title: showcaseBlueprint.targetCardBTitle,
      description: "Second anchor card used to show conflict-safe placement."
    },
    {
      columnId: doneColumn.id,
      title: "Demo: Delivery Snapshot",
      description: "A third lane to make the board feel complete."
    }
  ];

  const createdCards = [];
  for (const spec of cardSpecs) {
    const result = await callApi(
      "POST",
      `/api/v1/columns/${spec.columnId}/cards`,
      { title: spec.title, description: spec.description },
      true
    );
    if (!result.ok) {
      if (!quiet) {
        setShowcaseOutput({ status: result.status, action: "create-card", body: result.payload });
      }
      return result;
    }
    createdCards.push(result.payload);
  }

  showcaseState.columns = sortByPosition(createdColumns);
  showcaseState.cards = sortByPosition(createdCards);
  syncShowcaseStateToWeekForms();
  renderShowcaseWorkspace();

  if (!quiet) {
    setShowcaseOutput({
      status: 200,
      action: "seed-board",
      boardId,
      columns: createdColumns,
      cards: createdCards
    });
  }

  return { ok: true, status: 200, payload: { columns: createdColumns, cards: createdCards } };
}

async function createShowcaseComment(options = {}) {
  const quiet = options.quiet === true;
  if (!requireShowcaseAuth() || !requireShowcaseBoard()) {
    return { ok: false, status: 400, payload: "Missing auth or board" };
  }

  const movingCard = resolveShowcaseScenario().movingCard;
  if (!movingCard) {
    if (!quiet) {
      setShowcaseOutput("Seed the demo board first so there is a card to comment on.");
    }
    return { ok: false, status: 404, payload: "Moving card not found" };
  }

  const result = await callApi(
    "POST",
    `/api/v1/cards/${movingCard.id}/comments`,
    { body: showcaseBlueprint.commentBody },
    true
  );

  if (result.ok) {
    showcaseState.comment = result.payload;
    renderShowcaseWorkspace();
  }

  if (!quiet) {
    setShowcaseOutput({
      status: result.status,
      action: "create-comment",
      body: result.payload
    });
  }

  return result;
}

async function refreshShowcaseWorkspace(options = {}) {
  const quiet = options.quiet === true;
  if (!requireShowcaseAuth() || !requireShowcaseBoard()) {
    return { ok: false, status: 400, payload: "Missing auth or board" };
  }

  const boardId = showcaseState.board.id;
  const columnsResult = await callApi("GET", `/api/v1/boards/${boardId}/columns?page=0&size=50`, null, true);
  if (!columnsResult.ok) {
    if (!quiet) {
      setShowcaseOutput({ status: columnsResult.status, action: "refresh-columns", body: columnsResult.payload });
    }
    return columnsResult;
  }

  const columns = sortByPosition(columnsResult.payload.items || []);
  const cardResults = await Promise.all(
    columns.map((column) => callApi("GET", `/api/v1/columns/${column.id}/cards?page=0&size=50`, null, true))
  );

  const cards = [];
  for (const result of cardResults) {
    if (!result.ok) {
      if (!quiet) {
        setShowcaseOutput({ status: result.status, action: "refresh-cards", body: result.payload });
      }
      return result;
    }
    cards.push(...(result.payload.items || []));
  }

  showcaseState.columns = columns;
  showcaseState.cards = sortByPosition(cards);

  const movingCard = resolveShowcaseScenario().movingCard;
  if (movingCard) {
    const commentsResult = await callApi("GET", `/api/v1/cards/${movingCard.id}/comments?page=0&size=10`, null, true);
    showcaseState.comment = commentsResult.ok && commentsResult.payload.items?.length
      ? commentsResult.payload.items[0]
      : null;
  }

  syncShowcaseStateToWeekForms();
  renderShowcaseWorkspace();

  if (!quiet) {
    setShowcaseOutput({
      status: 200,
      action: "refresh-workspace",
      boardId,
      columns: showcaseState.columns.length,
      cards: showcaseState.cards.length
    });
  }

  return { ok: true, status: 200, payload: showcaseState };
}

async function runShowcaseMove(options = {}) {
  const quiet = options.quiet === true;
  if (!requireShowcaseAuth() || !requireShowcaseBoard()) {
    return { ok: false, status: 400, payload: "Missing auth or board" };
  }

  await refreshShowcaseWorkspace({ quiet: true });
  const scenario = resolveShowcaseScenario();
  if (!scenario.movingCard || !scenario.targetColumn || !scenario.targetCardA || !scenario.targetCardB) {
    if (!quiet) {
      setShowcaseOutput("The move demo needs the seeded Inbox and Doing columns plus the two target cards.");
    }
    return { ok: false, status: 404, payload: "Move scenario not ready" };
  }

  const result = await callApi(
    "PATCH",
    `/api/v1/cards/${scenario.movingCard.id}/move`,
    {
      targetColumnId: scenario.targetColumn.id,
      previousCardId: scenario.targetCardA.id,
      nextCardId: scenario.targetCardB.id,
      version: scenario.movingCard.version
    },
    true
  );

  if (result.ok) {
    setWeek5CardState(result.payload);
    await refreshShowcaseWorkspace({ quiet: true });
  }

  setOutput("week5-move-output", {
    status: result.status,
    expected: "Should move the card into the Doing lane between the two anchor cards.",
    body: result.payload
  });

  if (!quiet) {
    setShowcaseOutput({
      status: result.status,
      action: "move-card",
      body: result.payload
    });
  }

  return result;
}

async function runShowcaseConflictDemo() {
  if (!requireShowcaseAuth() || !requireShowcaseBoard()) {
    return { ok: false, status: 400, payload: "Missing auth or board" };
  }

  await refreshShowcaseWorkspace({ quiet: true });
  const scenario = resolveShowcaseScenario();
  if (!scenario.movingCard) {
    setShowcaseOutput("Seed the board first so the page has a tracked card for the conflict demo.");
    return { ok: false, status: 404, payload: "Moving card not found" };
  }

  const staleVersion = scenario.movingCard.version;
  const updateResult = await callApi(
    "PUT",
    `/api/v1/columns/${scenario.movingCard.columnId}/cards/${scenario.movingCard.id}`,
    {
      title: scenario.movingCard.title,
      description: `${scenario.movingCard.description ?? ""} [background update ${new Date().toLocaleTimeString()}]`,
      version: staleVersion
    },
    true
  );

  if (!updateResult.ok) {
    setShowcaseOutput({
      status: updateResult.status,
      action: "bump-version",
      body: updateResult.payload
    });
    return updateResult;
  }

  const fallbackTarget = showcaseState.columns.find((column) => column.id !== scenario.movingCard.columnId);
  if (!fallbackTarget) {
    setShowcaseOutput("The conflict demo needs at least two columns.");
    return { ok: false, status: 400, payload: "No alternate target column" };
  }

  const moveResult = await callApi(
    "PATCH",
    `/api/v1/cards/${scenario.movingCard.id}/move`,
    {
      targetColumnId: fallbackTarget.id,
      previousCardId: null,
      nextCardId: null,
      version: staleVersion
    },
    true
  );

  await refreshShowcaseWorkspace({ quiet: true });
  setOutput("week5-move-output", {
    status: moveResult.status,
    expected: "Should be 409 with latest state and retry guidance.",
    body: moveResult.payload
  });
  setShowcaseOutput({
    status: moveResult.status,
    action: "stale-conflict-demo",
    body: moveResult.payload
  });

  return moveResult;
}

async function runShowcaseSetup() {
  const createUserResult = await createShowcaseUser({ quiet: true });
  if (!createUserResult.ok) {
    setShowcaseOutput({
      status: createUserResult.status,
      action: "create-user",
      body: createUserResult.payload
    });
    return;
  }

  const loginResult = await loginShowcaseUser({ quiet: true });
  if (!loginResult.ok) {
    setShowcaseOutput({
      status: loginResult.status,
      action: "login",
      body: loginResult.payload
    });
    return;
  }

  const boardResult = await createShowcaseBoard({ quiet: true });
  if (!boardResult.ok) {
    setShowcaseOutput({
      status: boardResult.status,
      action: "create-board",
      body: boardResult.payload
    });
    return;
  }

  const seedResult = await seedShowcaseBoard({ quiet: true });
  if (!seedResult.ok) {
    setShowcaseOutput({
      status: seedResult.status,
      action: "seed-board",
      body: seedResult.payload
    });
    return;
  }

  const commentResult = await createShowcaseComment({ quiet: true });
  await refreshShowcaseWorkspace({ quiet: true });
  switchWeek("week5");

  setShowcaseOutput({
    status: 200,
    action: "full-setup-ready",
    user: showcaseState.user,
    board: showcaseState.board,
    columns: showcaseState.columns.map((column) => ({ id: column.id, name: column.name })),
    cards: showcaseState.cards.map((card) => ({ id: card.id, title: card.title, columnId: card.columnId })),
    commentStatus: commentResult.ok ? "created" : "skipped"
  });
}

function initShowcase() {
  applyShowcaseDefaults();
  renderShowcaseWorkspace();

  document.getElementById("showcase-generate-btn").addEventListener("click", () => {
    applyShowcaseDefaults();
    setShowcaseOutput("Fresh demo credentials and board name generated. Create the user next.");
  });

  ["showcase-email", "showcase-display-name", "showcase-password"].forEach((id) => {
    document.getElementById(id).addEventListener("input", () => {
      syncShowcaseIdentityIntoWeekForms();
    });
  });

  document.getElementById("showcase-create-user-btn").addEventListener("click", () => createShowcaseUser());
  document.getElementById("showcase-login-btn").addEventListener("click", () => loginShowcaseUser());
  document.getElementById("showcase-create-board-btn").addEventListener("click", () => createShowcaseBoard());
  document.getElementById("showcase-seed-board-btn").addEventListener("click", () => seedShowcaseBoard());
  document.getElementById("showcase-comment-btn").addEventListener("click", () => createShowcaseComment());
  document.getElementById("showcase-refresh-btn").addEventListener("click", () => refreshShowcaseWorkspace());
  document.getElementById("showcase-move-btn").addEventListener("click", () => runShowcaseMove());
  document.getElementById("showcase-conflict-btn").addEventListener("click", () => runShowcaseConflictDemo());
  document.getElementById("showcase-setup-btn").addEventListener("click", () => runShowcaseSetup());
  document.getElementById("showcase-open-week3-btn").addEventListener("click", () => switchWeek("week3"));
  document.getElementById("showcase-open-week5-btn").addEventListener("click", () => switchWeek("week5"));
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
      if (result.ok) {
        setInputValue("showcase-email", body.email);
        setInputValue("showcase-display-name", body.displayName);
        setInputValue("showcase-password", body.password);
        showcaseState.user = result.payload;
        renderShowcaseWorkspace();
      }
      setOutput("week1-user-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week1-user-output", `Request failed: ${error.message}`);
    }
  });
}

function renderAuthState() {
  const compactLabel = state.accessToken ? "Authenticated" : "Not logged in";
  const summaryLabel = state.accessToken
    ? (state.currentEmail ? `Ready as ${state.currentEmail}` : "Access token loaded")
    : "Viewer mode";

  setTextIfPresent("week2-auth-state", compactLabel);
  setTextIfPresent("hero-auth-summary", summaryLabel);
  setTextIfPresent("spotlight-auth", summaryLabel);
}

function initWeek2() {
  document.querySelectorAll(".preset-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.getElementById("week2-email").value = btn.dataset.email;
      document.getElementById("week2-password").value = "Password123!";
      setInputValue("showcase-email", btn.dataset.email);
      setInputValue("showcase-password", "Password123!");
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
        state.currentEmail = body.email;
        setInputValue("showcase-email", body.email);
        setInputValue("showcase-password", body.password);
        await loadCurrentUserIntoShowcase();
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

function parseOptionalNumber(value) {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const parsed = Number(trimmed);
  return Number.isNaN(parsed) ? null : parsed;
}

function setWeek5CardState(cardPayload) {
  if (!cardPayload || !cardPayload.id) {
    return;
  }
  document.getElementById("week5-card-id").value = cardPayload.id;
  document.getElementById("week5-source-column-id").value = cardPayload.columnId;
  document.getElementById("week5-card-version").value = cardPayload.version;
}

function requireWeek5Login(outputId) {
  if (state.accessToken) {
    return true;
  }
  setOutput(outputId, "Week 5 requires login first (use the Week 2 panel).");
  return false;
}

function parseColumnOrderInput() {
  const raw = document.getElementById("week5-column-order").value;
  const ids = raw
    .split(",")
    .map((value) => Number(value.trim()))
    .filter((value) => !Number.isNaN(value));

  if (!ids.length) {
    throw new Error("Provide at least one column ID.");
  }
  return ids;
}

function buildWeek5MovePayload(versionOverride = null) {
  const versionInput = document.getElementById("week5-card-version").value.trim();
  const version = versionOverride ?? (versionInput ? Number(versionInput) : Number.NaN);
  if (Number.isNaN(version)) {
    throw new Error("Card version is required. Load the card first.");
  }

  return {
    targetColumnId: Number(document.getElementById("week5-target-column-id").value),
    previousCardId: parseOptionalNumber(document.getElementById("week5-previous-card-id").value),
    nextCardId: parseOptionalNumber(document.getElementById("week5-next-card-id").value),
    version
  };
}

async function initWeek5Move(versionOverride = null, expected = null) {
  if (!requireWeek5Login("week5-move-output")) {
    return;
  }

  const cardId = Number(document.getElementById("week5-card-id").value);
  if (!cardId) {
    setOutput("week5-move-output", "Card ID is required.");
    return;
  }

  try {
    const result = await callApi(
      "PATCH",
      `/api/v1/cards/${cardId}/move`,
      buildWeek5MovePayload(versionOverride),
      true
    );
    if (result.ok) {
      setWeek5CardState(result.payload);
      if (showcaseState.board) {
        await refreshShowcaseWorkspace({ quiet: true });
      }
    }
    setOutput("week5-move-output", {
      status: result.status,
      expected,
      body: result.payload
    });
  } catch (error) {
    setOutput("week5-move-output", `Request failed: ${error.message}`);
  }
}

function initWeek5() {
  setOutput("week5-column-output", "Fetch columns first, then submit a new ordered ID list.");
  setOutput("week5-move-output", "Load the card snapshot, then move it with neighbor IDs.");

  document.getElementById("week5-list-columns-btn").addEventListener("click", async () => {
    if (!requireWeek5Login("week5-column-output")) {
      return;
    }
    const boardId = Number(document.getElementById("week5-board-id").value);
    try {
      const result = await callApi("GET", `/api/v1/boards/${boardId}/columns?page=0&size=50`, null, true);
      setOutput("week5-column-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week5-column-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week5-column-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!requireWeek5Login("week5-column-output")) {
      return;
    }

    const boardId = Number(document.getElementById("week5-board-id").value);
    try {
      const result = await callApi(
        "PATCH",
        `/api/v1/boards/${boardId}/columns/reorder`,
        { orderedColumnIds: parseColumnOrderInput() },
        true
      );
      if (result.ok && showcaseState.board?.id === boardId) {
        await refreshShowcaseWorkspace({ quiet: true });
      }
      setOutput("week5-column-output", {
        status: result.status,
        expected: "Columns should come back in the same order as the submitted ID list.",
        body: result.payload
      });
    } catch (error) {
      setOutput("week5-column-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week5-get-card-btn").addEventListener("click", async () => {
    if (!requireWeek5Login("week5-move-output")) {
      return;
    }

    const columnId = Number(document.getElementById("week5-source-column-id").value);
    const cardId = Number(document.getElementById("week5-card-id").value);
    try {
      const result = await callApi("GET", `/api/v1/columns/${columnId}/cards/${cardId}`, null, true);
      if (result.ok) {
        setWeek5CardState(result.payload);
      }
      setOutput("week5-move-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week5-move-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week5-list-target-cards-btn").addEventListener("click", async () => {
    if (!requireWeek5Login("week5-move-output")) {
      return;
    }

    const columnId = Number(document.getElementById("week5-target-column-id").value);
    try {
      const result = await callApi("GET", `/api/v1/columns/${columnId}/cards?page=0&size=20`, null, true);
      setOutput("week5-move-output", { status: result.status, body: result.payload });
    } catch (error) {
      setOutput("week5-move-output", `Request failed: ${error.message}`);
    }
  });

  document.getElementById("week5-move-card-btn").addEventListener("click", () => {
    initWeek5Move(null, "Should move the card and return the new version/position.");
  });

  document.getElementById("week5-stale-move-btn").addEventListener("click", () => {
    const currentVersion = Number(document.getElementById("week5-card-version").value);
    if (Number.isNaN(currentVersion) || currentVersion < 1) {
      setOutput("week5-move-output", "Run one successful move or update first so version - 1 becomes stale.");
      return;
    }
    initWeek5Move(currentVersion - 1, "Should be 409 with latest state and retry guidance.");
  });
}

initWeekTabs();
initHeroActions();
initShowcase();
initWeek1();
initWeek2();
initWeek3();
initWeek4();
initWeek5();
switchWeek(state.currentWeek);
renderAuthState();
