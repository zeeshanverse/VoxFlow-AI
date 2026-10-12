const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export function token() {
  return localStorage.getItem("voxflow_token");
}

async function request(path, options = {}) {
  const { operationId, ...fetchOptions } = options;
  const headers = { ...(fetchOptions.headers || {}) };
  const t = token();

  if (t) headers.Authorization = `Bearer ${t}`;
  if (operationId) headers["X-VoxFlow-Operation-Id"] = operationId;

  const res = await fetch(`${API}${path}`, {
    ...fetchOptions,
    headers,
  });

  if (!res.ok) {
    let body = {};
    try {
      body = await res.json();
    } catch {}

    if (res.status === 401 || res.status === 403) {
      throw new Error("Your session has expired. Please log in again.");
    }

    throw new Error(body.error || body.message || `Request failed: ${res.status}`);
  }

  return res;
}

export async function register(payload) {
  return (await request("/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })).json();
}

export async function login(payload) {
  return (await request("/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })).json();
}

export async function transcribe(blob, signal, operationId) {
  const form = new FormData();
  form.append("audio", blob, "voice.webm");
  return (await request("/voice/transcribe", {
    method: "POST",
    body: form,
    signal,
    operationId,
  })).json();
}

export async function assistant(text, conversationId, signal, operationId) {
  return (await request("/assistant/message", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text, conversationId }),
    signal,
    operationId,
  })).json();
}

export async function speak(text, signal, operationId) {
  return (await request("/voice/speak", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text }),
    signal,
    operationId,
  })).blob();
}

export async function conversations() {
  return (await request("/conversations")).json();
}

export async function analytics() {
  return (await request("/analytics")).json();
}

export async function startInterview(topic, signal, operationId) {
  return (await request(`/interviews?topic=${encodeURIComponent(topic)}`, {
    method: "POST",
    signal,
    operationId,
  })).json();
}

export async function answerInterview(id, blob, signal, operationId) {
  const form = new FormData();
  form.append("audio", blob, "answer.webm");
  return (await request(`/interviews/${id}/answer`, {
    method: "POST",
    body: form,
    signal,
    operationId,
  })).json();
}

export async function cancelOperation(operationId) {
  return request("/operations/cancel", {
    method: "POST",
    operationId,
  });
}

export async function updateProfile(displayName) {
  return (await request("/profile", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ displayName }),
  })).json();
}

export { API };
