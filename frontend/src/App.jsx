import {useEffect,useRef,useState} from "react";
import * as api from "./api";

export default function App(){
 const [auth,setAuth]=useState(Boolean(api.token())); const [mode,setMode]=useState("assistant");
 const [email,setEmail]=useState(""); const [password,setPassword]=useState(""); const [name,setName]=useState("");
 const [recording,setRecording]=useState(false); const [busy,setBusy]=useState(false);
 const [transcript,setTranscript]=useState(""); const [response,setResponse]=useState("");
 const [conversationId,setConversationId]=useState(null); const [stats,setStats]=useState(null);
 const [interview,setInterview]=useState(null); const recorder=useRef(null); const chunks=useRef([]);

 useEffect(()=>{if(auth) api.analytics().then(setStats).catch(()=>{})},[auth]);

 async function authAction(type){
   try{const data=type==="login"?await api.login({email,password}):await api.register({email,password,displayName:name});
   localStorage.setItem("voxflow_token",data.token);setAuth(true);
   }catch(e){alert(e.message)}
 }
 async function start(){
   const stream=await navigator.mediaDevices.getUserMedia({audio:true});
   const r=new MediaRecorder(stream);chunks.current=[];
   r.ondataavailable=e=>{if(e.data.size)chunks.current.push(e.data)};
   r.onstop=async()=>{stream.getTracks().forEach(t=>t.stop());await process(new Blob(chunks.current,{type:r.mimeType}))};
   recorder.current=r;r.start();setRecording(true);
 }
 function stop(){recorder.current?.stop();setRecording(false)}
 async function playSpeech(text){
   const b=await api.speak(text);
   const url=URL.createObjectURL(b);
   const audio=new Audio(url);
   audio.onended=()=>URL.revokeObjectURL(url);
   await audio.play();
 }
 async function process(blob){
   setBusy(true);setResponse("");
   try{
     const t=await api.transcribe(blob);setTranscript(t.text);
     if(mode==="assistant"){
       const a=await api.assistant(t.text,conversationId);setConversationId(a.conversationId);setResponse(a.response);
       await playSpeech(a.response);
     }else{
       if(!interview){const i=await api.startInterview("Java Backend");setInterview(i);setResponse(i.question)}
       else{const a=await api.answerInterview(interview.id,blob);const spoken=a.completed?`Final score: ${a.finalScore} out of 10. ${a.feedback}`:`Score: ${a.score} out of 10. ${a.feedback}. ${a.nextQuestion||""}`;setResponse(spoken);await playSpeech(spoken);if(!a.completed)setInterview({...interview,question:a.nextQuestion})}
     }
   }catch(e){setResponse(e.message)}finally{setBusy(false)}
 }
 if(!auth) return <Auth email={email} setEmail={setEmail} password={password} setPassword={setPassword} name={name} setName={setName} onLogin={()=>authAction("login")} onRegister={()=>authAction("register")}/>;
 return <main className="app"><section className="shell"><header><div><p className="eyebrow">VOICE AI</p><h1>VoxFlow</h1><p>Voice assistant + Java interview coach</p></div><button onClick={()=>{localStorage.removeItem("voxflow_token");setAuth(false)}}>Logout</button></header>
 <nav><button className={mode==="assistant"?"active":""} onClick={()=>setMode("assistant")}>Assistant</button><button className={mode==="interview"?"active":""} onClick={()=>setMode("interview")}>Java Interview</button></nav>
 <div className="hero"><button className={`mic ${recording?"recording":""}`} onClick={recording?stop:start} disabled={busy}>{recording?"■":"🎙"}</button><p>{busy?"Processing...":recording?"Listening...":"Press to speak"}</p></div>
 {mode==="interview"&&!interview&&<div className="panel"><button onClick={async()=>{try{const i=await api.startInterview("Java Backend");setInterview(i);setResponse(i.question);await playSpeech(i.question)}catch(e){setResponse(e.message)}}}>Start Java Interview</button></div>}{mode==="interview"&&interview&&<div className="panel"><b>Current question</b><p>{interview.question}</p></div>}
 {transcript&&<div className="panel"><b>You said</b><p>{transcript}</p></div>}
 {response&&<div className="panel"><b>VoxFlow</b><p>{response}</p></div>}
 {stats&&<div className="stats"><span>Conversations: {stats.conversations}</span><span>Interviews: {stats.interviews}</span><span>Avg score: {Number(stats.averageInterviewScore).toFixed(1)}/10</span></div>}
 </section></main>
}

function Auth(p){return <main className="app"><section className="auth"><p className="eyebrow">VOXFLOW AI</p><h1>Welcome</h1><input placeholder="Name (register)" value={p.name} onChange={e=>p.setName(e.target.value)}/><input placeholder="Email" value={p.email} onChange={e=>p.setEmail(e.target.value)}/><input placeholder="Password" type="password" value={p.password} onChange={e=>p.setPassword(e.target.value)}/><button onClick={p.onLogin}>Login</button><button className="secondary" onClick={p.onRegister}>Create account</button></section></main>}
