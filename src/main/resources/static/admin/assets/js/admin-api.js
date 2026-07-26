(function(){
  'use strict';
  const API_BASE_URL='https://illustrious-nurturing-production-8169.up.railway.app/api';
  const TOKEN_KEY='oyuki_token';
  const USER_KEY='oyuki_user';

  function readUser(){try{return JSON.parse(localStorage.getItem(USER_KEY)||'null')}catch{return null}}
  function unwrap(payload){return payload&&Object.prototype.hasOwnProperty.call(payload,'data')?payload.data:payload}
  function message(payload,fallback){if(!payload)return fallback;if(typeof payload==='string')return payload;return payload.message||payload.rootMessage||payload.error||fallback}

  async function request(path,{method='GET',body,auth=true,raw=false}={}){
    const headers={Accept:'application/json'};
    const token=localStorage.getItem(TOKEN_KEY);
    if(auth&&token)headers.Authorization=`Bearer ${token}`;
    let requestBody=body;
    if(body!==undefined&&body!==null&&!(body instanceof FormData)){headers['Content-Type']='application/json';requestBody=JSON.stringify(body)}
    let response;
    try{response=await fetch(`${API_BASE_URL}${path}`,{method,headers,body:requestBody})}
    catch{throw new Error('Cannot reach the Oyuki backend. Check the Railway deployment and internet connection.')}
    if(raw){if(!response.ok)throw new Error(`Request failed (${response.status})`);return response}
    const text=await response.text();let payload=null;
    if(text){try{payload=JSON.parse(text)}catch{payload=text}}
    if(!response.ok){if(response.status===401||response.status===403){if(auth){localStorage.removeItem(TOKEN_KEY);localStorage.removeItem(USER_KEY)}}throw new Error(message(payload,`Request failed (${response.status})`))}
    return unwrap(payload);
  }

  async function login(identifier,password){
    const result=await request('/auth/login',{method:'POST',body:{identifier,password},auth:false});
    if(!result?.token)throw new Error('The server did not return an access token.');
    if(String(result.role||'').toUpperCase()!=='ADMIN')throw new Error('This account is not an administrator.');
    const user={id:result.userId,userId:result.userId,fullName:result.fullName,role:result.role,status:result.status};
    localStorage.setItem(TOKEN_KEY,result.token);localStorage.setItem(USER_KEY,JSON.stringify(user));return user;
  }

  window.AdminApi={
  API_BASE_URL,
  currentUser:readUser,
  token:()=>localStorage.getItem(TOKEN_KEY),
  isAdmin:()=>Boolean(localStorage.getItem(TOKEN_KEY)&&String(readUser()?.role||'').toUpperCase()==='ADMIN'),
  login,
  logout(){localStorage.removeItem(TOKEN_KEY);localStorage.removeItem(USER_KEY);location.replace('admin-login.html')},
  request,
  users:{list:(filters={})=>{const p=new URLSearchParams();Object.entries(filters).forEach(([k,v])=>{if(v!==undefined&&v!==null&&v!=='')p.set(k,v)});return request(`/admin/users${p.toString()?`?${p}`:''}`)},statistics:()=>request('/admin/users/statistics'),updateStatus:(id,status,reason='')=>request(`/admin/users/${id}/status`,{method:'PATCH',body:{status,reason:reason||null}})},
  applications:{pending:()=>request('/admin/applications/pending'),get:id=>request(`/admin/applications/${id}`),approve:id=>request(`/admin/applications/${id}/approve`,{method:'PATCH',body:{}}),reject:(id,reason)=>request(`/admin/applications/${id}/reject`,{method:'PATCH',body:{reason}})},
  orders:{list:()=>request('/admin/orders'),get:id=>request(`/admin/orders/${id}`),markReceived:id=>request(`/admin/orders/items/${id}/received`,{method:'PATCH',body:{}})},
  payments:{list:(status='')=>request(`/admin/payments${status?`?status=${encodeURIComponent(status)}`:''}`),confirm:(id,note='')=>request(`/admin/payments/${id}/confirm`,{method:'PATCH',body:{note:note||null}}),reject:(id,reason)=>request(`/admin/payments/${id}/reject`,{method:'PATCH',body:{reason}}),receipt:async id=>{const r=await request(`/admin/payments/${id}/receipt`,{raw:true});const blob=await r.blob();const url=URL.createObjectURL(blob);window.open(url,'_blank','noopener');setTimeout(()=>URL.revokeObjectURL(url),60000)}}
};
})();
