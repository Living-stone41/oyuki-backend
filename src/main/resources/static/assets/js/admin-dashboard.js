(function () {
  'use strict';

  const state = {
    user: null,
    users: [],
    statistics: null,
    applications: [],
    orders: [],
    payments: [],
    products: []
  };

  const $ = selector => document.querySelector(selector);
  const e = value => Oyuki.escapeHtml(value);
  const money = value => Oyuki.fmt(value);
  const pretty = value => String(value || '—').replaceAll('_', ' ').toLowerCase()
    .replace(/\b\w/g, c => c.toUpperCase());

  document.addEventListener('DOMContentLoaded', init);

  async function init() {
    state.user = Oyuki.Auth.require('ADMIN');
    if (!state.user) return;

    if (String(state.user.role || '').toUpperCase() !== 'ADMIN') {
      location.href = Oyuki.rolePage(state.user.role);
      return;
    }

    $('#who-name').textContent = state.user.fullName || 'Administrator';
    window.addEventListener('hashchange', route);

    await refreshAll();
    route();
  }

  async function safe(task, fallback) {
    try { return await task(); }
    catch (_) { return fallback; }
  }

  async function refreshAll() {
    renderLoading('Loading administration data…');
    const [users, statistics, applications, orders, payments, products] = await Promise.all([
      safe(() => Oyuki.AdminUsers.list(), []),
      safe(() => Oyuki.AdminUsers.statistics(), null),
      safe(() => Oyuki.AdminApplications.pending(), []),
      safe(() => Oyuki.AdminOrders.list(), []),
      safe(() => Oyuki.AdminPayments.list(), []),
      safe(() => Oyuki.Products.list(), [])
    ]);

    state.users = Array.isArray(users) ? users : [];
    state.statistics = statistics;
    state.applications = Array.isArray(applications) ? applications : [];
    state.orders = Array.isArray(orders) ? orders : [];
    state.payments = Array.isArray(payments) ? payments : [];
    state.products = Array.isArray(products) ? products : [];
  }

  function renderLoading(message) {
    $('#view').innerHTML = `<div class="panel text-center py-5">
      <div class="spinner-border text-success mb-3"></div>
      <div class="text-muted">${e(message)}</div>
    </div>`;
  }

  function section() {
    return (location.hash || '#dashboard').slice(1);
  }

  function route() {
    const sec = section();
    document.querySelectorAll('aside a').forEach(a => {
      a.classList.toggle('active', a.dataset.s === sec);
    });

    if (sec === 'dashboard') renderDashboard();
    else if (sec === 'customers') renderUsers('CUSTOMER', 'Customers');
    else if (sec === 'sellers') renderProviders('SELLER', 'Sellers');
    else if (sec === 'kitchens') renderProviders('KITCHEN', 'Kitchens');
    else if (sec === 'orders') renderOrders();
    else if (sec === 'payments') renderPayments();
    else if (sec === 'contacts') renderContacts();
    else if (sec === 'reports') renderReports();
    else renderDashboard();
  }

  function statCard(label, value) {
    return `<div class="col-6 col-lg-3">
      <div class="stat h-100"><div class="k">${e(label)}</div><div class="v">${value}</div></div>
    </div>`;
  }

  function renderDashboard() {
    const stats = state.statistics || {};
    const pendingPayments = state.payments.filter(p => p.status === 'SUBMITTED').length;
    const revenue = state.orders.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0);

    $('#view').innerHTML = `
      <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
        <div><h3 class="mb-1">Administration overview</h3>
          <p class="text-muted mb-0">Live users, applications, orders, products and payments.</p>
        </div>
        <button class="btn btn-outline-brand" onclick="AdminDashboard.refresh()">
          <i class="bi bi-arrow-clockwise"></i> Refresh
        </button>
      </div>

      <div class="row g-3 my-3">
        ${statCard('Customers', Number(stats.customers || 0))}
        ${statCard('Sellers', Number(stats.sellers || 0))}
        ${statCard('Kitchens', Number(stats.kitchens || 0))}
        ${statCard('Orders', state.orders.length)}
        ${statCard('Pending approval', state.applications.length)}
        ${statCard('Payment receipts', pendingPayments)}
        ${statCard('Active products', state.products.length)}
        ${statCard('Order value', money(revenue))}
      </div>

      <div class="row g-3">
        <div class="col-xl-7"><div class="panel">
          <div class="d-flex justify-content-between align-items-center"><h5>Pending applications</h5>
            <a href="#sellers" class="btn btn-sm btn-ghost">Manage</a></div>
          ${applicationsTable(state.applications.slice(0, 5))}
        </div></div>
        <div class="col-xl-5"><div class="panel">
          <div class="d-flex justify-content-between align-items-center"><h5>Recent orders</h5>
            <a href="#orders" class="btn btn-sm btn-ghost">View all</a></div>
          ${ordersTable(state.orders.slice(0, 5), true)}
        </div></div>
      </div>`;
  }

  function renderUsers(role, title) {
    const users = state.users.filter(user => user.role === role);
    $('#view').innerHTML = `
      <div class="d-flex justify-content-between align-items-center">
        <div><h3 class="mb-1">${title}</h3><p class="text-muted mb-0">${users.length} account(s)</p></div>
        <button class="btn btn-outline-brand" onclick="AdminDashboard.refreshUsers()"><i class="bi bi-arrow-clockwise"></i></button>
      </div>
      <div class="panel mt-3">${usersTable(users)}</div>`;
  }

  function usersTable(users) {
    if (!users.length) return '<p class="text-muted text-center py-4 mb-0">No users found.</p>';

    return `<div class="table-responsive"><table class="oy-table">
      <thead><tr><th>Name</th><th>Contact</th><th>Status</th><th>Verified</th><th>Joined</th><th></th></tr></thead>
      <tbody>${users.map(user => `<tr>
        <td><strong>${e(user.fullName)}</strong><div class="small text-muted">#${user.id}</div></td>
        <td>${e(user.email || user.phoneNumber || '—')}<div class="small text-muted">${e(user.phoneNumber || '')}</div></td>
        <td>${statusPill(user.status)}${user.statusReason ? `<div class="small text-muted mt-1">${e(user.statusReason)}</div>` : ''}</td>
        <td>${user.emailVerified ? '<span class="badge text-bg-success">Email</span>' : ''} ${user.phoneVerified ? '<span class="badge text-bg-success">Phone</span>' : ''}</td>
        <td>${e(Oyuki.date(user.createdAt))}</td>
        <td>${userStatusActions(user)}</td>
      </tr>`).join('')}</tbody>
    </table></div>`;
  }

  function userStatusActions(user) {
    if (user.status === 'ACTIVE') {
      return `<div class="dropdown">
        <button class="btn btn-sm btn-ghost dropdown-toggle" data-bs-toggle="dropdown">Manage</button>
        <ul class="dropdown-menu dropdown-menu-end">
          <li><button class="dropdown-item" onclick="AdminDashboard.changeStatus(${user.id},'SUSPENDED')">Suspend</button></li>
          <li><button class="dropdown-item text-danger" onclick="AdminDashboard.changeStatus(${user.id},'DISABLED')">Disable</button></li>
        </ul></div>`;
    }
    if (['SUSPENDED','DISABLED','REJECTED'].includes(user.status)) {
      return `<button class="btn btn-sm btn-brand" onclick="AdminDashboard.changeStatus(${user.id},'ACTIVE')">Activate</button>`;
    }
    return '<span class="text-muted small">Use application review</span>';
  }

  async function changeStatus(userId, status) {
    let reason = '';
    if (status !== 'ACTIVE') {
      reason = prompt(`Reason for ${pretty(status)}:`);
      if (!reason) return;
    }

    try {
      await Oyuki.AdminUsers.updateStatus(userId, status, reason);
      await refreshUsers();
      Oyuki.Toast.show('Account status updated', 'success');
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function refreshUsers() {
    const [users, statistics] = await Promise.all([
      Oyuki.AdminUsers.list(),
      Oyuki.AdminUsers.statistics()
    ]);
    state.users = users;
    state.statistics = statistics;
    route();
  }

  function renderProviders(role, title) {
    const users = state.users.filter(user => user.role === role);
    const pending = state.applications.filter(application => application.role === role);

    $('#view').innerHTML = `
      <h3>${title}</h3>
      <div class="panel mt-3">
        <h5>Pending applications</h5>
        ${applicationsTable(pending)}
      </div>
      <div class="panel mt-3">
        <h5>All ${title.toLowerCase()}</h5>
        ${usersTable(users)}
      </div>`;
  }

  function applicationsTable(applications) {
    if (!applications.length) return '<p class="text-muted text-center py-3 mb-0">No pending applications.</p>';

    return `<div class="table-responsive"><table class="oy-table">
      <thead><tr><th>Applicant</th><th>Business</th><th>Location</th><th>Profile</th><th></th></tr></thead>
      <tbody>${applications.map(app => `<tr>
        <td><strong>${e(app.fullName)}</strong><div class="small text-muted">${e(app.email || app.phoneNumber || '')}</div></td>
        <td>${e(app.businessName || 'Incomplete profile')}<div class="small text-muted">${e(pretty(app.role))}</div></td>
        <td>${e([app.area, app.lga, app.state].filter(Boolean).join(', ') || '—')}</td>
        <td>${app.profileCompleted ? '<span class="badge text-bg-success">Completed</span>' : '<span class="badge text-bg-warning">Incomplete</span>'}</td>
        <td class="text-nowrap">
          <button class="btn btn-sm btn-ghost" onclick="AdminDashboard.viewApplication(${app.userId})">View</button>
          <button class="btn btn-sm btn-brand" onclick="AdminDashboard.approve(${app.userId})" ${app.profileCompleted ? '' : 'disabled'}>Approve</button>
          <button class="btn btn-sm btn-outline-danger" onclick="AdminDashboard.reject(${app.userId})">Reject</button>
        </td>
      </tr>`).join('')}</tbody>
    </table></div>`;
  }

  async function viewApplication(userId) {
    try {
      const app = await Oyuki.AdminApplications.get(userId);
      showModal('Application details', `
        <div class="row g-3">
          <div class="col-md-6"><strong>Name</strong><div>${e(app.fullName)}</div></div>
          <div class="col-md-6"><strong>Role</strong><div>${e(pretty(app.role))}</div></div>
          <div class="col-md-6"><strong>Business/Kitchen</strong><div>${e(app.businessName || '—')}</div></div>
          <div class="col-md-6"><strong>Cuisine</strong><div>${e(app.cuisine || '—')}</div></div>
          <div class="col-12"><strong>Bio</strong><p>${e(app.bio || '—')}</p></div>
          <div class="col-12"><strong>Address</strong><div>${e([app.addressLine, app.area, app.lga, app.state].filter(Boolean).join(', ') || '—')}</div></div>
          <div class="col-md-6"><strong>Bank</strong><div>${e(app.bankName || '—')}</div></div>
          <div class="col-md-6"><strong>Account</strong><div>${e(app.accountName || '—')} ${e(app.accountNumber || '')}</div></div>
          <div class="col-md-6"><strong>Facial verification</strong><div>${e(pretty(app.facialVerificationStatus))}</div></div>
          <div class="col-md-6"><strong>Profile completion</strong><div>${app.profileCompleted ? '<span class="badge text-bg-success">Complete</span>' : '<span class="badge text-bg-warning">Missing required files</span>'}</div></div>
          <div class="col-md-6">
            <strong>Profile picture</strong>
            <div class="mt-2">${app.profileImageUrl
              ? `<img src="${e(Oyuki.imageUrl(app.profileImageUrl))}" alt="Applicant profile" style="width:150px;height:150px;object-fit:cover;border-radius:12px">`
              : '<span class="text-danger">Not uploaded</span>'}</div>
          </div>
          <div class="col-md-6">
            <strong>ID document</strong>
            <div class="mt-2">${app.idDocumentUrl
              ? `<button class="btn btn-outline-brand btn-sm" onclick='AdminDashboard.openDocument(${JSON.stringify(app.idDocumentUrl)})'><i class="bi bi-file-earmark-person"></i> Open protected document</button>`
              : '<span class="text-danger">Not uploaded</span>'}</div>
          </div>
        </div>`);
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }


  async function openDocument(url) {
    try {
      await Oyuki.AdminFiles.open(url);
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function approve(userId) {
    if (!confirm('Approve this application?')) return;
    try {
      await Oyuki.AdminApplications.approve(userId);
      await refreshApplications();
      await refreshUsers();
      Oyuki.Toast.show('Application approved', 'success');
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function reject(userId) {
    const reason = prompt('Why are you rejecting this application?');
    if (!reason) return;
    try {
      await Oyuki.AdminApplications.reject(userId, reason);
      await refreshApplications();
      await refreshUsers();
      Oyuki.Toast.show('Application rejected');
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function refreshApplications() {
    state.applications = await Oyuki.AdminApplications.pending();
    route();
  }

  function renderOrders() {
    $('#view').innerHTML = `
      <div class="d-flex justify-content-between align-items-center">
        <div><h3 class="mb-1">Orders</h3><p class="text-muted mb-0">${state.orders.length} order(s)</p></div>
        <button class="btn btn-outline-brand" onclick="AdminDashboard.refreshOrders()"><i class="bi bi-arrow-clockwise"></i></button>
      </div>
      <div class="panel mt-3">${ordersTable(state.orders)}</div>`;
  }

  function ordersTable(orders, compact = false) {
    if (!orders.length) return '<p class="text-muted text-center py-4 mb-0">No orders yet.</p>';

    return `<div class="table-responsive"><table class="oy-table">
      <thead><tr><th>Order</th><th>Customer</th>${compact ? '' : '<th>Items</th><th>Payment</th>'}<th>Total</th><th>Status</th><th></th></tr></thead>
      <tbody>${orders.map(order => `<tr>
        <td><strong>${e(order.orderNumber)}</strong><div class="small text-muted">${e(Oyuki.date(order.createdAt))}</div></td>
        <td>${e(order.customerName || '—')}<div class="small text-muted">${e(order.customerEmail || '')}</div></td>
        ${compact ? '' : `<td>${Number(order.totalItems || 0)} item(s)<div class="small text-muted">${Number(order.totalProviders || 0)} provider(s)</div></td>
          <td>${e(pretty(order.paymentMethod))}<div>${statusPill(order.paymentStatus)}</div></td>`}
        <td>${money(order.totalAmount)}</td>
        <td>${statusPill(order.orderStatus)}</td>
        <td><button class="btn btn-sm btn-ghost" onclick="AdminDashboard.viewOrder(${order.id})">View</button></td>
      </tr>`).join('')}</tbody>
    </table></div>`;
  }

  async function viewOrder(orderId) {
    try {
      const order = await Oyuki.AdminOrders.get(orderId);
      const items = order.items || [];
      showModal(`Order ${e(order.orderNumber)}`, `
        <div class="row g-3 mb-3">
          <div class="col-md-4"><strong>Customer</strong><div>${e(order.customerName || '—')}</div></div>
          <div class="col-md-4"><strong>Payment</strong><div>${e(pretty(order.paymentStatus))}</div></div>
          <div class="col-md-4"><strong>Order status</strong><div>${e(pretty(order.orderStatus))}</div></div>
          <div class="col-md-4"><strong>Subtotal</strong><div>${money(order.subtotal)}</div></div>
          <div class="col-md-4"><strong>Delivery</strong><div>${money(order.deliveryFee)}</div></div>
          <div class="col-md-4"><strong>Discount</strong><div>${money(order.discountAmount)} ${order.couponCode ? `(${e(order.couponCode)})` : ''}</div></div>
          <div class="col-12"><strong>Delivery address</strong><div>${e([order.addressLine, order.area, order.lga, order.state].filter(Boolean).join(', ') || '—')}</div></div>
        </div>
        <div class="table-responsive"><table class="oy-table">
          <thead><tr><th>Item</th><th>Provider</th><th>Total</th><th>Status</th><th></th></tr></thead>
          <tbody>${items.map(item => `<tr>
            <td>${e(item.productName)}<div class="small text-muted">${item.quantity} × ${money(item.unitPrice)}</div></td>
            <td>${e(item.providerName)}<div class="small text-muted">${e(pretty(item.providerRole))}</div></td>
            <td>${money(item.subtotal)}</td>
            <td>${statusPill(item.status)}</td>
            <td>${item.status === 'READY_FOR_PICKUP'
              ? `<button class="btn btn-sm btn-brand" onclick="AdminDashboard.markReceived(${item.id},${order.id})">Mark received</button>`
              : ''}</td>
          </tr>`).join('')}</tbody>
        </table></div>`, true);
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function markReceived(itemId, orderId) {
    try {
      await Oyuki.AdminOrders.markReceived(itemId);
      Oyuki.Toast.show('Item marked as received', 'success');
      closeModal();
      await refreshOrders();
      await viewOrder(orderId);
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function refreshOrders() {
    state.orders = await Oyuki.AdminOrders.list();
    route();
  }

  function renderPayments() {
    $('#view').innerHTML = `
      <div class="d-flex justify-content-between align-items-center">
        <div><h3 class="mb-1">Payment receipts</h3><p class="text-muted mb-0">Confirm or reject customer bank-transfer evidence.</p></div>
        <button class="btn btn-outline-brand" onclick="AdminDashboard.refreshPayments()"><i class="bi bi-arrow-clockwise"></i></button>
      </div>
      <div class="panel mt-3">${paymentsTable(state.payments)}</div>`;
  }

  function paymentsTable(payments) {
    if (!payments.length) return '<p class="text-muted text-center py-4 mb-0">No payment receipts.</p>';

    return `<div class="table-responsive"><table class="oy-table">
      <thead><tr><th>Order</th><th>Customer</th><th>Payment</th><th>Amount</th><th>Status</th><th></th></tr></thead>
      <tbody>${payments.map(payment => `<tr>
        <td>${e(payment.orderNumber)}</td>
        <td>${e(payment.customerName)}<div class="small text-muted">${e(payment.customerEmail || '')}</div></td>
        <td>${e(payment.senderBankName || '—')}<div class="small text-muted">${e(payment.transactionReference || '')}</div></td>
        <td>${money(payment.amount)}</td>
        <td>${statusPill(payment.status)}</td>
        <td class="text-nowrap">
          <button class="btn btn-sm btn-ghost" onclick="AdminDashboard.openReceipt(${payment.id})">Receipt</button>
          ${payment.status === 'SUBMITTED' ? `
            <button class="btn btn-sm btn-brand" onclick="AdminDashboard.confirmPayment(${payment.id})">Confirm</button>
            <button class="btn btn-sm btn-outline-danger" onclick="AdminDashboard.rejectPayment(${payment.id})">Reject</button>` : ''}
        </td>
      </tr>`).join('')}</tbody>
    </table></div>`;
  }

  async function openReceipt(id) {
    try { await Oyuki.AdminPayments.receipt(id); }
    catch (error) { Oyuki.Toast.show(error.message, 'error'); }
  }

  async function confirmPayment(id) {
    const note = prompt('Optional admin note:') || '';
    try {
      await Oyuki.AdminPayments.confirm(id, note);
      await refreshPayments();
      await refreshOrders();
      Oyuki.Toast.show('Payment confirmed', 'success');
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function rejectPayment(id) {
    const reason = prompt('Reason for rejecting this payment:');
    if (!reason) return;
    try {
      await Oyuki.AdminPayments.reject(id, reason);
      await refreshPayments();
      Oyuki.Toast.show('Payment rejected');
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function refreshPayments() {
    state.payments = await Oyuki.AdminPayments.list();
    route();
  }

  function renderContacts() {
    $('#view').innerHTML = `
      <h3>Contact messages</h3>
      <div class="panel mt-3">
        <div class="alert alert-info mb-0">
          The current Spring Boot backend does not yet contain a contact-message endpoint.
          The public contact page can be connected after a Contact module is added.
        </div>
      </div>`;
  }

  function renderReports() {
    const revenue = state.orders.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0);
    const average = state.orders.length ? revenue / state.orders.length : 0;
    const confirmed = state.payments.filter(p => p.status === 'CONFIRMED')
      .reduce((sum, payment) => sum + Number(payment.amount || 0), 0);
    const orderStatuses = state.orders.reduce((acc, order) => {
      acc[order.orderStatus] = (acc[order.orderStatus] || 0) + 1;
      return acc;
    }, {});

    $('#view').innerHTML = `
      <h3>Reports</h3>
      <div class="row g-3 my-3">
        ${statCard('Total order value', money(revenue))}
        ${statCard('Average order', money(average))}
        ${statCard('Confirmed payments', money(confirmed))}
        ${statCard('Marketplace products', state.products.length)}
      </div>
      <div class="panel">
        <h5>Orders by status</h5>
        <div class="row g-3 mt-1">
          ${Object.entries(orderStatuses).map(([status, count]) => `<div class="col-md-3">
            <div class="border rounded p-3"><div class="small text-muted">${e(pretty(status))}</div><strong class="fs-4">${count}</strong></div>
          </div>`).join('') || '<p class="text-muted">No order data.</p>'}
        </div>
      </div>`;
  }

  function statusPill(status) {
    const value = String(status || 'UNKNOWN');
    const cls = value.includes('REJECT') || value.includes('CANCEL') || value.includes('DISABLED') || value.includes('SUSPENDED')
      ? 'status-rejected'
      : value.includes('ACTIVE') || value.includes('CONFIRMED') || value.includes('DELIVERED') || value.includes('RECEIVED') || value.includes('READY')
        ? 'status-approved'
        : value.includes('PROCESS') || value.includes('ACCEPT')
          ? 'status-cooking'
          : 'status-pending';
    return `<span class="status-pill ${cls}">${e(pretty(value))}</span>`;
  }

  function showModal(title, body, wide = false) {
    let holder = $('#admin-modal-holder');
    if (!holder) {
      holder = document.createElement('div');
      holder.id = 'admin-modal-holder';
      document.body.appendChild(holder);
    }
    holder.innerHTML = `<div class="modal fade" id="adminDetailModal" tabindex="-1">
      <div class="modal-dialog modal-dialog-scrollable ${wide ? 'modal-xl' : 'modal-lg'}">
        <div class="modal-content">
          <div class="modal-header"><h5 class="modal-title">${title}</h5>
            <button class="btn-close" data-bs-dismiss="modal"></button></div>
          <div class="modal-body">${body}</div>
        </div>
      </div>
    </div>`;
    bootstrap.Modal.getOrCreateInstance($('#adminDetailModal')).show();
  }

  function closeModal() {
    const element = $('#adminDetailModal');
    if (element) bootstrap.Modal.getInstance(element)?.hide();
  }

  async function refresh() {
    await refreshAll();
    Oyuki.Toast.show('Administration data refreshed', 'success');
    route();
  }

  window.AdminDashboard = {
    refresh,
    refreshUsers,
    refreshApplications,
    refreshOrders,
    refreshPayments,
    changeStatus,
    viewApplication,
    openDocument,
    approve,
    reject,
    viewOrder,
    markReceived,
    openReceipt,
    confirmPayment,
    rejectPayment
  };
})();
