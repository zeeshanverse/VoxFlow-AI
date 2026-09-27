const API=import.meta.env.VITE_API_BASE_URL||"http://localhost:8080/api";
export function token(){return localStorage.getItem("voxflow_token");}
async function request(path,options={}){
  const headers={...(options.headers||{})};
  const t=token(); if(t) headers.Authorization=`Bearer ${t}`;
  const res=await fetch(`${API}${path}`,{...options,headers});
  if(!res.ok){let body={};try{body=await res.json()}catch{} throw new Error(body.error||`Request failed: ${res.status}`);}
  return res;
}
export async function register(payload){return (await request("/auth/register",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)})).json();}
export async function login(payload){return (await request("/auth/login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)})).json();}
export async function transcribe(blob){const f=new FormData();f.append("audio",blob,"voice.webm");return (await request("/voice/transcribe",{method:"POST",body:f})).json();}
export async function assistant(text,conversationId){return (await request("/assistant/message",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({text,conversationId})})).json();}
export async function speak(text){return (await request("/voice/speak",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({text})})).blob();}
export async function conversations(){return (await request("/conversations")).json();}
export async function analytics(){return (await request("/analytics")).json();}
export async function startInterview(topic){return (await request(`/interviews?topic=${encodeURIComponent(topic)}`,{method:"POST"})).json();}
export async function answerInterview(id,blob){const f=new FormData();f.append("audio",blob,"answer.webm");return (await request(`/interviews/${id}/answer`,{method:"POST",body:f})).json();}
export async function profile(){return (await request("/profile")).json();}
export async function updateProfile(displayName){return (await request("/profile",{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({displayName})})).json();}
export {API};
