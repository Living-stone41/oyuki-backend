(function(){'use strict';
  const O=window.Oyuki, form=document.getElementById('marketerActivationForm'), error=document.getElementById('activationError'), button=document.getElementById('activateButton');
  const params=new URLSearchParams(location.search); if(params.get('contact')) document.getElementById('contact').value=params.get('contact');
  form?.addEventListener('submit',async e=>{e.preventDefault(); error.classList.remove('show');
    const payload=Object.fromEntries(new FormData(form).entries());
    if(payload.password!==payload.confirmPassword){error.textContent='Passwords do not match.';error.classList.add('show');return;}
    try{button.disabled=true;button.textContent='Activating…'; await O.Api.post('/auth/activate-marketer',payload,false); O.Toast.show('Account activated. You can now log in.','success'); setTimeout(()=>location.href='login.html',900);}
    catch(err){error.textContent=err.message||'Unable to activate account.';error.classList.add('show');}
    finally{button.disabled=false;button.textContent='Verify and activate';}
  });
})();
