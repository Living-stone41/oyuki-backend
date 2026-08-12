(function(){
  'use strict';
  const API_BASE_URL='https://illustrious-nurturing-production-8169.up.railway.app/api';
  const TOKEN_KEY='oyuki_token', USER_KEY='oyuki_user';
  const readUser=()=>{try{return JSON.parse(localStorage.getItem(USER_KEY)||'null')}catch{return null}};
  const unwrap=p=>p&&Object.prototype.hasOwnProperty.call(p,'data')?p.data:p;
  const message=(p,f)=>!p?f:typeof p==='string'?p:(p.message||p.rootMessage||p.error||f);
  async function request(path,{method='GET',body,auth=true,raw=false}={}){
    const headers={Accept:'application/json'},token=localStorage.getItem(TOKEN_KEY);
    if(auth&&token)headers.Authorization=`Bearer ${token}`;
    let payload=body;if(body!==undefined&&body!==null&&!(body instanceof FormData)){headers['Content-Type']='application/json';payload=JSON.stringify(body)}
    let response;try{response=await fetch(`${API_BASE_URL}${path}`,{method,headers,body:payload})}catch{throw new Error('Cannot reach the Oyuki backend.')}
    if(raw){if(!response.ok)throw new Error(`Request failed (${response.status})`);return response}
    const text=await response.text();let data=null;if(text){try{data=JSON.parse(text)}catch{data=text}}
    if(!response.ok){if((response.status===401||response.status===403)&&auth){localStorage.removeItem(TOKEN_KEY);localStorage.removeItem(USER_KEY)}throw new Error(message(data,`Request failed (${response.status})`))}
    return unwrap(data);
  }
  async function login(identifier,password){const r=await request('/auth/login',{method:'POST',body:{identifier,password},auth:false});if(!r?.token)throw new Error('The server did not return an access token.');if(String(r.role||'').toUpperCase()!=='ADMIN')throw new Error('This account is not an administrator.');const u={id:r.userId,userId:r.userId,fullName:r.fullName,role:r.role,status:r.status};localStorage.setItem(TOKEN_KEY,r.token);localStorage.setItem(USER_KEY,JSON.stringify(u));return u}
  async function downloadFile(path,fallbackName='oyuki-download'){const r=await request(path,{raw:true});const blob=await r.blob();let filename=fallbackName;const d=r.headers.get('Content-Disposition');const m=d&&d.match(/filename="?([^";]+)"?/i);if(m?.[1])filename=m[1];const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=filename;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000)}
  const api={API_BASE_URL,currentUser:readUser,token:()=>localStorage.getItem(TOKEN_KEY),isAdmin:()=>Boolean(localStorage.getItem(TOKEN_KEY)&&String(readUser()?.role||'').toUpperCase()==='ADMIN'),login,logout(){localStorage.removeItem(TOKEN_KEY);localStorage.removeItem(USER_KEY);location.replace('admin-login.html')},request,get:p=>request(p),post:(p,b)=>request(p,{method:'POST',body:b}),put:(p,b)=>request(p,{method:'PUT',body:b}),patch:(p,b)=>request(p,{method:'PATCH',body:b}),delete:p=>request(p,{method:'DELETE'}),downloadFile};
  window.AdminApi=api;window.OyukiAdminApi=api;
})();
