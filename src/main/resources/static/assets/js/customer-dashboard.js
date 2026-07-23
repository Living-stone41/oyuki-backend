(function(){
'use strict';
let user,orders=[],wishlist=[],addresses=[],notifications=[],payments=[],cart={totalItems:0},bankAccount=null;
const e=value=>Oyuki.escapeHtml(value);
const pretty=value=>String(value||'—').replaceAll('_',' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase());
const view=()=>document.getElementById('view');

document.addEventListener('DOMContentLoaded',async()=>{
  user=Oyuki.Auth.require('CUSTOMER'); if(!user)return;
  document.getElementById('who-name').textContent=user.fullName||'Customer';
  window.addEventListener('hashchange',route);
  await reload(); route();
  const paymentOrder=sessionStorage.getItem('oyuki_payment_order');
  if(paymentOrder){sessionStorage.removeItem('oyuki_payment_order');setTimeout(()=>openPaymentUpload(Number(paymentOrder)),350);}
});
async function safe(task,fallback){try{return await task();}catch(_){return fallback;}}
async function reload(){
  [orders,wishlist,addresses,notifications,cart,payments,bankAccount]=await Promise.all([
    safe(()=>Oyuki.Orders.list(),[]),safe(()=>Oyuki.Wishlist.list(),[]),safe(()=>Oyuki.Addresses.list(),[]),
    safe(()=>Oyuki.Notifications.list(),[]),safe(()=>Oyuki.Cart.get(),{totalItems:0}),
    safe(()=>Oyuki.CustomerPayments.list(),[]),safe(()=>Oyuki.CustomerPayments.bankAccount(),null)
  ]);
}
function route(){
  const sec=(location.hash||'#dashboard').slice(1);
  document.querySelectorAll('aside a').forEach(a=>a.classList.toggle('active',a.dataset.s===sec));
  if(sec==='dashboard')renderDashboard(); else if(sec==='orders')renderOrders(); else if(sec==='wishlist')renderWishlist();
  else if(sec==='addresses')renderAddresses(); else if(sec==='notifications')renderNotifications(); else renderProfile();
}
function renderDashboard(){const spent=orders.reduce((s,o)=>s+Number(o.totalAmount||0),0);view().innerHTML=`<h3>Hi ${e((user.fullName||'Customer').split(' ')[0])} 👋</h3><div class="row g-3 my-3">${stat('Orders',orders.length)}${stat('Cart items',cart.totalItems||0)}${stat('Wishlist',wishlist.length)}${stat('Spent',Oyuki.fmt(spent))}</div><div class="panel"><h5>Recent orders</h5>${ordersTable(orders.slice(0,5))}</div>`;}
function stat(label,value){return `<div class="col-md-3"><div class="stat"><div class="k">${e(label)}</div><div class="v">${value}</div></div></div>`;}
function proofFor(orderId){return payments.filter(p=>Number(p.orderId)===Number(orderId)).sort((a,b)=>new Date(b.createdAt)-new Date(a.createdAt))[0]||null;}
function renderOrders(){view().innerHTML=`<div class="d-flex justify-content-between align-items-center"><div><h3>My orders</h3><p class="text-muted">Upload bank-transfer receipts here for administrator confirmation.</p></div><button class="btn btn-outline-brand" onclick="CustomerDashboard.refresh()"><i class="bi bi-arrow-clockwise"></i></button></div>${ordersTable(orders)}`;}
function ordersTable(list){
  if(!list.length)return'<p class="text-muted">No orders yet. <a href="shop.html">Shop now</a>.</p>';
  return `<div class="table-responsive"><table class="oy-table mt-2"><thead><tr><th>Order</th><th>Date</th><th>Total</th><th>Order status</th><th>Payment</th><th></th></tr></thead><tbody>${list.map(o=>{
    const proof=proofFor(o.id); const canUpload=String(o.paymentMethod)==='BANK_TRANSFER'&&['AWAITING_CONFIRMATION','REJECTED'].includes(String(o.paymentStatus))&&(!proof||proof.status==='REJECTED');
    return `<tr><td><strong>${e(o.orderNumber||o.id)}</strong><div class="small text-muted">${o.items?.length||0} item(s)</div></td><td>${new Date(o.createdAt).toLocaleDateString()}</td><td>${Oyuki.fmt(o.totalAmount)}</td><td>${pill(o.status)}</td><td>${pill(o.paymentStatus)}${proof?`<div class="small mt-1">Receipt: ${pill(proof.status)}</div>`:''}${proof?.rejectionReason?`<div class="small text-danger">${e(proof.rejectionReason)}</div>`:''}</td><td class="text-nowrap"><a href="tracking.html?id=${o.id}" class="btn btn-sm btn-outline-brand">View</a> ${canUpload?`<button class="btn btn-sm btn-brand" onclick="CustomerDashboard.openPaymentUpload(${o.id})"><i class="bi bi-upload"></i> Upload receipt</button>`:''} ${proof?`<button class="btn btn-sm btn-ghost" onclick="CustomerDashboard.openReceipt(${proof.id})">Receipt</button>`:''}</td></tr>`;
  }).join('')}</tbody></table></div>`;
}
function pill(status){const v=String(status||'UNKNOWN');const cls=v.includes('REJECT')||v.includes('CANCEL')?'status-rejected':v.includes('PAID')||v.includes('CONFIRM')||v.includes('ACTIVE')||v.includes('DELIVERED')?'status-approved':v.includes('PROCESS')?'status-cooking':'status-pending';return `<span class="status-pill ${cls}">${e(pretty(v))}</span>`;}
function renderWishlist(){view().innerHTML=`<h3>Wishlist</h3><div class="row mt-2">${wishlist.length?wishlist.map(i=>`<div class="col-md-4 mb-3"><div class="panel"><h6>${e(i.productName)}</h6><div class="small text-muted">${e(i.productStatus)}</div><a href="product.html?id=${i.productId}" class="btn btn-sm btn-outline-brand mt-2">View</a> <button class="btn btn-sm btn-ghost mt-2" onclick="CustomerDashboard.removeWish(${i.productId})">Remove</button></div></div>`).join(''):'<p class="text-muted">Empty.</p>'}</div>`;}
function renderAddresses(){view().innerHTML=`<div class="d-flex justify-content-between"><h3>Addresses</h3><a href="checkout.html" class="btn btn-brand btn-sm">Add at checkout</a></div>${addresses.length?addresses.map(a=>`<div class="panel mb-2"><div class="d-flex justify-content-between"><strong>${e(a.label)}</strong>${a.defaultAddress?'<span class="badge-soft">Default</span>':''}</div><div class="small text-muted">${e(a.recipientName)} · ${e(a.phone)}</div><div>${e(a.streetAddress)}, ${e(a.area)}, ${e(a.city)}, ${e(a.state)}</div><div class="mt-2">${!a.defaultAddress?`<button class="btn btn-sm btn-outline-brand" onclick="CustomerDashboard.makeDefault(${a.id})">Make default</button> `:''}<button class="btn btn-sm btn-ghost" onclick="CustomerDashboard.deleteAddress(${a.id})">Delete</button></div></div>`).join(''):'<p class="text-muted">No saved address yet.</p>'}`;}
function renderNotifications(){view().innerHTML=`<div class="d-flex justify-content-between"><h3>Notifications</h3><button class="btn btn-sm btn-outline-brand" onclick="CustomerDashboard.readAll()">Mark all read</button></div>${notifications.length?notifications.map(n=>`<div class="panel mb-2 ${n.read?'':'border-success'}"><strong>${e(n.title)}</strong><div>${e(n.message)}</div><small class="text-muted">${Oyuki.date(n.createdAt)}</small></div>`).join(''):'<p class="text-muted">No notifications.</p>'}`;}
function renderProfile(){view().innerHTML=`<h3>Profile</h3><div class="panel" style="max-width:560px"><div class="mb-2"><strong>Name:</strong> ${e(user.fullName)}</div><div class="mb-2"><strong>Email:</strong> ${e(user.email||'—')}</div><div class="mb-2"><strong>Phone:</strong> ${e(user.phoneNumber||'—')}</div><div class="mb-2"><strong>Status:</strong> ${e(user.status||'—')}</div></div>`;}
function openPaymentUpload(orderId){
  const order=orders.find(o=>Number(o.id)===Number(orderId)); if(!order)return;
  const now=new Date(Date.now()-new Date().getTimezoneOffset()*60000).toISOString().slice(0,16);
  document.getElementById('customer-modal-holder').innerHTML=`<div class="modal fade" id="paymentModal"><div class="modal-dialog modal-lg"><div class="modal-content"><div class="modal-header"><h5 class="modal-title">Upload receipt — ${e(order.orderNumber)}</h5><button class="btn-close" data-bs-dismiss="modal"></button></div><div class="modal-body">${bankAccount?`<div class="alert alert-success"><strong>Pay Oyuki:</strong><br>${e(bankAccount.bankName)} · ${e(bankAccount.accountName)} · <strong>${e(bankAccount.accountNumber)}</strong>${bankAccount.paymentInstructions?`<div class="small mt-2">${e(bankAccount.paymentInstructions)}</div>`:''}</div>`:'<div class="alert alert-warning">The admin has not configured a receiving bank account yet.</div>'}<form id="paymentProofForm" onsubmit="CustomerDashboard.submitPayment(event,${order.id})"><div class="row g-3"><div class="col-md-6"><label class="form-label">Amount paid</label><input class="form-control" name="amount" type="number" min="0.01" step="0.01" required value="${Number(order.totalAmount||0)}"></div><div class="col-md-6"><label class="form-label">Payment date</label><input class="form-control" name="paymentDate" type="datetime-local" required value="${now}"></div><div class="col-md-6"><label class="form-label">Sender bank name</label><input class="form-control" name="senderBankName" required maxlength="150" data-letters-only data-allow-ampersand="true" data-validation-message="Bank name must contain letters only."></div><div class="col-md-6"><label class="form-label">Sender account name</label><input class="form-control" name="senderAccountName" required maxlength="200" data-letters-only data-validation-message="Account name must contain letters only."></div><div class="col-md-6"><label class="form-label">Transaction reference</label><input class="form-control" name="transactionReference" required maxlength="150"></div><div class="col-md-6"><label class="form-label">Receipt file</label><input class="form-control" name="receiptFile" type="file" accept="image/jpeg,image/png,image/webp,application/pdf" required><div class="form-text">JPG, PNG, WEBP or PDF; maximum 5 MB.</div></div><div class="col-12"><button class="btn btn-brand" type="submit"><i class="bi bi-upload"></i> Submit receipt</button></div></div></form></div></div></div></div>`;
  Oyuki.Forms.bindGuards(document.getElementById('paymentProofForm'));
  bootstrap.Modal.getOrCreateInstance(document.getElementById('paymentModal')).show();
}
async function submitPayment(event,orderId){event.preventDefault();const form=event.currentTarget;if(!form.reportValidity())return;const fd=new FormData(form);try{form.querySelector('button[type=submit]').disabled=true;await Oyuki.CustomerPayments.upload(orderId,fd);Oyuki.Toast.show('Receipt submitted for administrator confirmation.','success');bootstrap.Modal.getInstance(document.getElementById('paymentModal'))?.hide();await reload();renderOrders();}catch(error){Oyuki.Toast.show(error.message,'error');form.querySelector('button[type=submit]').disabled=false;}}
async function openReceipt(id){try{await Oyuki.CustomerPayments.receipt(id);}catch(error){Oyuki.Toast.show(error.message,'error');}}
async function removeWish(id){try{await Oyuki.Wishlist.remove(id);await reload();route();}catch(error){Oyuki.Toast.show(error.message,'error');}}
async function makeDefault(id){try{await Oyuki.Addresses.setDefault(id);await reload();route();}catch(error){Oyuki.Toast.show(error.message,'error');}}
async function deleteAddress(id){if(!confirm('Delete this address?'))return;try{await Oyuki.Addresses.remove(id);await reload();route();}catch(error){Oyuki.Toast.show(error.message,'error');}}
async function readAll(){try{await Oyuki.Notifications.readAll();await reload();route();}catch(error){Oyuki.Toast.show(error.message,'error');}}
async function refresh(){await reload();route();Oyuki.Toast.show('Dashboard refreshed','success');}
window.CustomerDashboard={refresh,openPaymentUpload,submitPayment,openReceipt,removeWish,makeDefault,deleteAddress,readAll};
})();
