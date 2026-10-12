import { useEffect, useRef, useState } from "react";
import * as api from "./api";

const ASSISTANT = "assistant";
const INTERVIEW = "interview";

export default function App() {
  const [auth, setAuth] = useState(Boolean(api.token()));
  const [mode, setMode] = useState(ASSISTANT);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");

  const [recording, setRecording] = useState(false);
  const [busy, setBusy] = useState(false);
  const [speaking, setSpeaking] = useState(false);
  const [status, setStatus] = useState("Ready");

  const [assistantTranscript, setAssistantTranscript] = useState("");
  const [assistantResponse, setAssistantResponse] = useState("");
  const [assistantConversationId, setAssistantConversationId] = useState(null);

  const [interview, setInterview] = useState(null);
  const [interviewTranscript, setInterviewTranscript] = useState("");
  const [interviewResponse, setInterviewResponse] = useState("");
  const [stats, setStats] = useState(null);

  const recorderRef = useRef(null);
  const streamRef = useRef(null);
  const chunksRef = useRef([]);
  const operationRef = useRef(null);
  const audioRef = useRef(null);
  const operationIdRef = useRef(0);

  useEffect(() => {
    if (auth) api.analytics().then(setStats).catch(() => {});
  }, [auth]);

  useEffect(() => () => cancelAll(), []);

  async function authAction(type) {
    try {
      const data = type === "login"
        ? await api.login({ email, password })
        : await api.register({ email, password, displayName: name });
      localStorage.setItem("voxflow_token", data.token);
      setAuth(true);
    } catch (e) {
      alert(e.message);
    }
  }

  function cancelAll() {
    operationIdRef.current += 1;

    if (operationRef.current) {
      void api.cancelOperation(operationRef.current.id).catch(() => {});
      operationRef.current.controller.abort();
      operationRef.current = null;
    }

    if (recorderRef.current) {
      recorderRef.current.onstop = null;
      recorderRef.current.ondataavailable = null;
      if (recorderRef.current.state !== "inactive") recorderRef.current.stop();
      recorderRef.current = null;
    }

    chunksRef.current = [];
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }

    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
      audioRef.current.src = "";
      audioRef.current = null;
    }

    setRecording(false);
    setBusy(false);
    setSpeaking(false);
    setStatus("Ready");
  }

  function beginOperation() {
    cancelAll();
    const controller = new AbortController();
    const id = ++operationIdRef.current;
    operationRef.current = { controller, id };
    setBusy(true);
    setStatus("Processing...");
    return { controller, id };
  }

  function isCurrent(id, signal) {
    return !signal.aborted && operationRef.current?.id === id;
  }

  async function startRecording() {
    if (busy || speaking) return;

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      streamRef.current = stream;
      recorderRef.current = recorder;
      chunksRef.current = [];

      recorder.ondataavailable = (event) => {
        if (event.data.size) chunksRef.current.push(event.data);
      };

      recorder.onstop = async () => {
        if (streamRef.current) {
          streamRef.current.getTracks().forEach((track) => track.stop());
          streamRef.current = null;
        }
        recorderRef.current = null;
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || "audio/webm" });
        chunksRef.current = [];
        if (blob.size) await processRecording(blob, mode);
      };

      recorder.start();
      setRecording(true);
      setStatus("Listening...");
    } catch (e) {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop());
        streamRef.current = null;
      }
      setStatus("Microphone unavailable");
      if (mode === ASSISTANT) setAssistantResponse(e.message);
      else setInterviewResponse(e.message);
    }
  }

  function stopRecording() {
    if (!recorderRef.current || recorderRef.current.state === "inactive") return;
    recorderRef.current.stop();
    setRecording(false);
    setStatus("Processing...");
  }

  async function playSpeech(text, controller, id) {
    if (!text || !isCurrent(id, controller.signal)) return;

    const blob = await api.speak(text, controller.signal, id);
    if (!isCurrent(id, controller.signal)) return;

    const url = URL.createObjectURL(blob);
    const audio = new Audio(url);
    audioRef.current = audio;
    setSpeaking(true);
    setStatus("Speaking...");

    const cleanup = () => {
      URL.revokeObjectURL(url);
      if (audioRef.current === audio) {
        audioRef.current = null;
        setSpeaking(false);
        if (!controller.signal.aborted) setStatus("Ready");
      }
    };

    const abortHandler = () => {
      audio.pause();
      audio.currentTime = 0;
      audio.src = "";
      cleanup();
    };

    controller.signal.addEventListener("abort", abortHandler, { once: true });
    audio.onended = cleanup;
    audio.onerror = cleanup;
    await audio.play();
  }

  async function processRecording(blob, modeAtStart) {
    const { controller, id } = beginOperation();
    const signal = controller.signal;

    try {
      if (modeAtStart === ASSISTANT) {
        const transcript = await api.transcribe(blob, signal, id);
        if (!isCurrent(id, signal)) return;
        setAssistantTranscript(transcript.text);

        const result = await api.assistant(transcript.text, assistantConversationId, signal, id);
        if (!isCurrent(id, signal)) return;
        setAssistantConversationId(result.conversationId);
        setAssistantResponse(result.response);
        await playSpeech(result.response, controller, id);
      } else {
        if (!interview) {
          setInterviewResponse("Start the Java interview first.");
          return;
        }
        if (interview.completed) {
          setInterviewResponse("This interview is complete. Start a new interview.");
          return;
        }

        // The interview endpoint transcribes the submitted audio itself.
        const result = await api.answerInterview(interview.id, blob, signal, id);
        if (!isCurrent(id, signal)) return;

        setInterviewTranscript(result.transcript);
        const spoken = result.completed
          ? `Final score: ${result.finalScore} out of 10. ${result.feedback}`
          : `Score: ${result.score} out of 10. ${result.feedback}. ${result.nextQuestion || ""}`;
        setInterviewResponse(spoken);

        if (result.completed) {
          setInterview({ ...interview, completed: true });
        } else {
          setInterview({ ...interview, question: result.nextQuestion, completed: false });
        }

        await playSpeech(spoken, controller, id);
      }
    } catch (e) {
      if (e?.name === "AbortError" || signal.aborted) return;
      if (modeAtStart === ASSISTANT) setAssistantResponse(e.message);
      else setInterviewResponse(e.message);
    } finally {
      if (operationRef.current?.id === id) {
        operationRef.current = null;
        setBusy(false);
        if (!audioRef.current) setStatus("Ready");
      }
    }
  }

  async function startInterview() {
    const { controller, id } = beginOperation();
    try {
      const result = await api.startInterview("Java Backend", controller.signal, id);
      if (!isCurrent(id, controller.signal)) return;
      setInterview({ ...result, completed: false });
      setInterviewTranscript("");
      setInterviewResponse(result.question);
      await playSpeech(result.question, controller, id);
    } catch (e) {
      if (e?.name !== "AbortError" && !controller.signal.aborted) setInterviewResponse(e.message);
    } finally {
      if (operationRef.current?.id === id) {
        operationRef.current = null;
        setBusy(false);
        if (!audioRef.current) setStatus("Ready");
      }
    }
  }

  function switchMode(nextMode) {
    if (nextMode === mode) return;
    cancelAll();
    setMode(nextMode);
  }

  function logout() {
    cancelAll();
    localStorage.removeItem("voxflow_token");
    setAuth(false);
  }

  const active = recording || busy || speaking;

  if (!auth) {
    return <Auth email={email} setEmail={setEmail} password={password} setPassword={setPassword} name={name} setName={setName} onLogin={() => authAction("login")} onRegister={() => authAction("register")} />;
  }

  return (
    <main className="app">
      <section className="shell">
        <header>
          <div>
            <p className="eyebrow">VOICE AI</p>
            <h1>VoxFlow</h1>
            <p>Voice assistant + Java interview coach</p>
          </div>
          <button className="logout-button" onClick={logout}>Logout</button>
        </header>

        <nav className="mode-switcher" aria-label="VoxFlow modes">
          <button className={mode === ASSISTANT ? "active" : ""} onClick={() => switchMode(ASSISTANT)}>Assistant</button>
          <button className={mode === INTERVIEW ? "active" : ""} onClick={() => switchMode(INTERVIEW)}>Java Interview</button>
        </nav>

        <div className="mode-heading">
          <div>
            <span className="mode-badge">{mode === ASSISTANT ? "Voice Assistant" : "Java Interview Coach"}</span>
            <h2>{mode === ASSISTANT ? "Talk naturally with VoxFlow" : "Practice Java backend interviews"}</h2>
          </div>
          <span className="status-text">{status}</span>
        </div>

        <div className="hero">
          <button className={`mic ${recording ? "recording" : ""}`} onClick={recording ? stopRecording : startRecording} disabled={busy || speaking || (mode === INTERVIEW && interview?.completed)}>
            {recording ? "■" : "🎙"}
          </button>
          <p>{recording ? "Listening... press again to finish" : speaking ? "VoxFlow is speaking..." : busy ? "Processing..." : "Press to speak"}</p>
        </div>

        <div className="control-row">
          <button className="stop-button" onClick={cancelAll} disabled={!active}>Stop</button>
          {mode === INTERVIEW && (!interview || interview.completed) && (
            <button className="primary-action" onClick={startInterview} disabled={busy || speaking}>
              {interview?.completed ? "Start New Interview" : "Start Java Interview"}
            </button>
          )}
        </div>

        {mode === INTERVIEW && interview && !interview.completed && (
          <div className="panel question-panel"><div className="panel-label">Current question</div><p>{interview.question}</p></div>
        )}

        {mode === ASSISTANT && assistantTranscript && (
          <div className="panel"><div className="panel-label">You said</div><p>{assistantTranscript}</p></div>
        )}
        {mode === ASSISTANT && assistantResponse && (
          <div className="panel response-panel"><div className="panel-label">VoxFlow</div><p>{assistantResponse}</p></div>
        )}

        {mode === INTERVIEW && interviewTranscript && (
          <div className="panel"><div className="panel-label">Your answer</div><p>{interviewTranscript}</p></div>
        )}
        {mode === INTERVIEW && interviewResponse && (
          <div className="panel response-panel"><div className="panel-label">Interview feedback</div><p>{interviewResponse}</p></div>
        )}

        {stats && (
          <div className="stats">
            <span>Conversations: {stats.conversations}</span>
            <span>Interviews: {stats.interviews}</span>
            <span>Avg score: {Number(stats.averageInterviewScore).toFixed(1)}/10</span>
          </div>
        )}
      </section>
    </main>
  );
}

function Auth({ email, setEmail, password, setPassword, name, setName, onLogin, onRegister }) {
  return (
    <main className="app"><section className="auth">
      <p className="eyebrow">VOXFLOW AI</p><h1>Welcome</h1>
      <input placeholder="Name (register)" value={name} onChange={(e) => setName(e.target.value)} />
      <input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
      <input placeholder="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
      <button onClick={onLogin}>Login</button><button className="secondary" onClick={onRegister}>Create account</button>
    </section></main>
  );
}
