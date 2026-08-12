(function () {
  'use strict';

  const STATES = [
    'ABIA','ADAMAWA','AKWA_IBOM','ANAMBRA','BAUCHI','BAYELSA','BENUE','BORNO',
    'CROSS_RIVER','DELTA','EBONYI','EDO','EKITI','ENUGU','GOMBE','IMO','JIGAWA',
    'KADUNA','KANO','KATSINA','KEBBI','KOGI','KWARA','LAGOS','NASARAWA','NIGER',
    'OGUN','ONDO','OSUN','OYO','PLATEAU','RIVERS','SOKOTO','TARABA','YOBE',
    'ZAMFARA','FEDERAL_CAPITAL_TERRITORY'
  ];

  const state = {
    role: '',
    user: null,
    products: [],
    orders: [],
    profile: null,
    pickup: null,
    reviews: [],
    rating: null,
    editingProductId: null
  };

  const $ = selector => document.querySelector(selector);
  const e = value => Oyuki.escapeHtml(value);
  const money = value => Oyuki.fmt(value);
  const pretty = value => String(value || '—').replaceAll('_', ' ').toLowerCase()
    .replace(/\b\w/g, c => c.toUpperCase());

  document.addEventListener('DOMContentLoaded', init);

  async function init() {
    state.role = String(document.body.dataset.providerRole || '').toUpperCase();
    state.user = Oyuki.Auth.require(state.role);
    if (!state.user) return;

    if (String(state.user.role || '').toUpperCase() !== state.role) {
      Oyuki.Toast.show(`This page is for ${pretty(state.role)} accounts.`, 'error');
      setTimeout(() => location.href = Oyuki.rolePage(state.user.role), 500);
      return;
    }

    $('#who-name').textContent = state.user.fullName || 'Provider';
    window.addEventListener('hashchange', route);

    await refreshAll();
    route();
  }

  async function safe(task, fallback = null) {
    try { return await task(); }
    catch (_) { return fallback; }
  }

  async function refreshAll() {
    renderLoading('Loading dashboard…');

    const [products, orders, profile, pickup, reviews, rating] = await Promise.all([
      safe(() => Oyuki.ProviderProducts.list(), []),
      safe(() => Oyuki.ProviderOrders.list(), []),
      safe(() => Oyuki.ProviderProfiles.get(state.role).then(Oyuki.unwrap || (x => x?.data ?? x)), null),
      safe(() => Oyuki.PickupAddress.get(), null),
      safe(() => Oyuki.PublicReviews.provider(state.user.id || state.user.userId), []),
      safe(() => Oyuki.PublicReviews.providerSummary(state.user.id || state.user.userId), null)
    ]);

    state.products = Array.isArray(products) ? products : [];
    state.orders = Array.isArray(orders) ? orders : [];
    state.profile = profile?.data ?? profile;
    state.pickup = pickup?.data ?? pickup;
    state.reviews = Array.isArray(reviews) ? reviews : [];
    state.rating = rating?.data ?? rating;
  }

  function renderLoading(message) {
    $('#view').innerHTML = `<div class="panel text-center py-5">
      <div class="spinner-border text-success mb-3"></div>
      <div class="text-muted">${e(message)}</div>
    </div>`;
  }

  function currentSection() {
    return (location.hash || '#dashboard').slice(1).split('?')[0];
  }

  function route() {
    const sec = currentSection();
    document.querySelectorAll('aside a').forEach(a => {
      a.classList.toggle('active', a.dataset.s === sec);
    });

    if (sec === 'dashboard') renderDashboard();
    else if (sec === 'products' || sec === 'menu') renderProducts();
    else if (sec === 'orders' || sec === 'cooking') renderOrders(sec);
    else if (sec === 'sales') renderSales();
    else if (sec === 'reviews') renderReviews();
    else if (sec === 'profile') renderProfile();
    else renderDashboard();
  }

  function nonRejectedOrders() {
    return state.orders.filter(item => !['REJECTED', 'CANCELLED'].includes(String(item.status)));
  }

  function renderDashboard() {
    const pending = state.orders.filter(item => item.status === 'PENDING').length;
    const active = state.orders.filter(item => ['ACCEPTED', 'PROCESSING', 'READY_FOR_PICKUP'].includes(item.status)).length;
    const revenue = nonRejectedOrders().reduce((sum, item) => sum + Number(item.lineTotal || 0), 0);
    const stock = state.products.reduce((sum, product) => {
      return sum + (product.variants || []).reduce((s, variant) => s + Number(variant.stockQuantity || 0), 0);
    }, 0);

    $('#view').innerHTML = `
      <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
        <div>
          <h3 class="mb-1">${state.role === 'KITCHEN' ? 'Kitchen' : 'Seller'} dashboard</h3>
          <p class="text-muted mb-0">Live information from the Oyuki backend.</p>
        </div>
        <button class="btn btn-outline-brand" onclick="ProviderDashboard.refresh()">
          <i class="bi bi-arrow-clockwise"></i> Refresh
        </button>
      </div>

      ${accountNotice()}

      <div class="row g-3 my-3">
        ${statCard(state.role === 'KITCHEN' ? 'Menu items' : 'Products', state.products.length)}
        ${statCard('Pending orders', pending)}
        ${statCard('Active orders', active)}
        ${statCard('Potential revenue', money(revenue))}
        ${statCard('Total stock', stock)}
        ${statCard('Average rating', `${Number(state.rating?.averageRating || 0).toFixed(1)}★`)}
      </div>

      <div class="panel">
        <div class="d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Recent order items</h5>
          <a href="#orders" class="btn btn-sm btn-ghost">View all</a>
        </div>
        ${ordersTable(state.orders.slice(0, 6))}
      </div>`;
  }

  function accountNotice() {
    const status = String(state.user.status || state.profile?.accountStatus || '');
    if (status === 'ACTIVE') return '';
    return `<div class="alert alert-warning mt-3 mb-0">
      Your account status is <strong>${e(pretty(status))}</strong>.
      Complete your profile and pickup address so an administrator can approve your account.
    </div>`;
  }

  function statCard(label, value) {
    return `<div class="col-6 col-lg-4 col-xl-2">
      <div class="stat h-100">
        <div class="k">${e(label)}</div>
        <div class="v">${value}</div>
      </div>
    </div>`;
  }

  function renderProducts() {
    const editing = state.editingProductId !== null;
    const product = editing && state.editingProductId
      ? state.products.find(p => Number(p.id) === Number(state.editingProductId))
      : null;

    $('#view').innerHTML = `
      <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
        <div>
          <h3 class="mb-1">${state.role === 'KITCHEN' ? 'Menu and services' : 'Products'}</h3>
          <p class="text-muted mb-0">Create, update, upload images, and remove your listings.</p>
        </div>
        <button class="btn btn-brand" onclick="ProviderDashboard.openProductForm()">
          <i class="bi bi-plus-lg"></i> Add ${state.role === 'KITCHEN' ? 'item' : 'product'}
        </button>
      </div>

      ${editing ? productForm(product) : ''}
      <div class="panel mt-3">${productsTable(state.products)}</div>`;

    if (editing) {
      const selected = product || state.profile || {};
      bindLocations('product', selected.state, selected.lga, selected.area, 'name');
      Oyuki.Forms.bindGuards($('#provider-product-form'));
    }
  }

  function productsTable(products) {
    if (!products.length) {
      return `<div class="text-center py-5 text-muted">
        <i class="bi bi-box-seam fs-1"></i>
        <p class="mt-2 mb-0">No products have been created yet.</p>
      </div>`;
    }

    return `<div class="table-responsive"><table class="oy-table">
      <thead><tr>
        <th>Product</th><th>Type</th><th>Variant</th><th>Stock</th><th>Status</th><th></th>
      </tr></thead>
      <tbody>${products.map(product => {
        const variant = (product.variants || [])[0] || {};
        const image = Oyuki.imageUrl((product.images || []).find(i => i.primaryImage)?.imageUrl || product.images?.[0]?.imageUrl);
        return `<tr>
          <td>
            <div class="d-flex align-items-center gap-2">
              <img src="${e(image)}" onerror="this.src='assets/images/hero.jpg'"
                style="width:46px;height:46px;object-fit:cover;border-radius:8px">
              <div><strong>${e(product.name)}</strong><div class="small text-muted">${e(product.category)}</div></div>
            </div>
          </td>
          <td>${e(pretty(product.productType))}</td>
          <td>${money(variant.price)} / ${e(`${variant.measurementValue || ''} ${pretty(variant.measurementUnit)}`)}</td>
          <td>${Number(variant.stockQuantity || 0)}</td>
          <td>${statusPill(product.status)}</td>
          <td class="text-nowrap">
            <button class="btn btn-sm btn-ghost" title="Edit"
              onclick="ProviderDashboard.openProductForm(${product.id})"><i class="bi bi-pencil"></i></button>
            <button class="btn btn-sm btn-ghost" title="Upload image"
              onclick="ProviderDashboard.chooseImage(${product.id})"><i class="bi bi-image"></i></button>
            <button class="btn btn-sm btn-ghost text-danger" title="Delete"
              onclick="ProviderDashboard.removeProduct(${product.id})"><i class="bi bi-trash"></i></button>
          </td>
        </tr>`;
      }).join('')}</tbody>
    </table></div>`;
  }

  function productForm(product) {
    const variant = product?.variants?.[0] || {};
    const productType = product?.productType || (state.role === 'KITCHEN' ? 'READY_MEAL' : 'FARM_PRODUCT');
    const typeOptions = state.role === 'KITCHEN'
      ? `<option value="READY_MEAL" ${productType === 'READY_MEAL' ? 'selected' : ''}>Ready meal</option>
         <option value="COOKING_SERVICE" ${productType === 'COOKING_SERVICE' ? 'selected' : ''}>Cooking service</option>`
      : `<option value="FARM_PRODUCT" selected>Farm product</option>`;

    return `<div class="panel mt-3 border border-success-subtle">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="mb-0">${product ? 'Edit product' : 'Add product'}</h5>
        <button class="btn-close" onclick="ProviderDashboard.cancelProductForm()"></button>
      </div>
      <form id="provider-product-form" onsubmit="ProviderDashboard.saveProduct(event, ${product?.id || 'null'})">
        <div class="row g-3">
          <div class="col-md-6"><label class="form-label">Name</label>
            <input class="form-control" name="name" required maxlength="150" value="${e(product?.name || '')}">
          </div>
          <div class="col-md-3"><label class="form-label">Category</label>
            <input class="form-control" name="category" required maxlength="100" value="${e(product?.category || '')}">
          </div>
          <div class="col-md-3"><label class="form-label">Product type</label>
            <select class="form-select" name="productType">${typeOptions}</select>
          </div>
          <div class="col-12"><label class="form-label">Description</label>
            <textarea class="form-control" name="description" minlength="10" maxlength="2000" required rows="3">${e(product?.description || '')}</textarea>
          </div>
          <div class="col-md-4"><label class="form-label">State</label>
            <select class="form-select" id="product-state" name="state" required></select>
          </div>
          <div class="col-md-4"><label class="form-label">LGA</label>
            <select class="form-select" id="product-lga" name="lga" required></select>
          </div>
          <div class="col-md-4"><label class="form-label">Area</label>
            <select class="form-select" id="product-area" name="area" required></select>
          </div>

          <div class="col-12"><hr><h6>First price and measurement option</h6></div>
          <div class="col-md-2"><label class="form-label">Value</label>
            <input class="form-control" type="number" min="0.001" step="0.001" name="measurementValue" required value="${e(variant.measurementValue || 1)}">
          </div>
          <div class="col-md-3"><label class="form-label">Unit</label>
            <select class="form-select" name="measurementUnit">${measurementOptions(variant.measurementUnit || (state.role === 'KITCHEN' ? 'PLATE' : 'KILOGRAM'))}</select>
          </div>
          <div class="col-md-2"><label class="form-label">Price</label>
            <input class="form-control" type="number" min="0.01" step="0.01" name="price" required value="${e(variant.price || '')}">
          </div>
          <div class="col-md-2"><label class="form-label">Stock</label>
            <input class="form-control" type="number" min="0" name="stockQuantity" required value="${e(variant.stockQuantity ?? 0)}">
          </div>
          <div class="col-md-1"><label class="form-label">MOQ</label>
            <input class="form-control" type="number" min="1" name="minimumOrderQuantity" required value="${e(variant.minimumOrderQuantity || 1)}">
          </div>
          <div class="col-md-2"><label class="form-label">SKU</label>
            <input class="form-control" name="sku" maxlength="100" value="${e(variant.sku || '')}">
          </div>
          <div class="col-md-6"><label class="form-label">Product image ${product ? '(optional replacement/addition)' : '(optional)'}</label>
            <input class="form-control" type="file" name="image" accept="image/*">
          </div>
          <div class="col-md-6 d-flex align-items-end">
            <div class="form-check mb-2">
              <input class="form-check-input" type="checkbox" name="available" id="available" ${variant.available === false ? '' : 'checked'}>
              <label class="form-check-label" for="available">Available for sale</label>
            </div>
          </div>
          <div class="col-12 d-flex gap-2">
            <button class="btn btn-brand" type="submit">
              <i class="bi bi-check-lg"></i> ${product ? 'Save changes' : 'Create product'}
            </button>
            <button class="btn btn-ghost" type="button" onclick="ProviderDashboard.cancelProductForm()">Cancel</button>
          </div>
        </div>
      </form>
    </div>
    <input id="provider-hidden-image" type="file" accept="image/*" class="d-none"
      onchange="ProviderDashboard.uploadChosenImage(event)">`;
  }

  function measurementOptions(selected) {
    const units = ['PIECE','CUP','GRAM','KILOGRAM','LITRE','MILLILITRE','BAG','BASKET','CRATE','BUNCH','PACK','TRAY','PLATE','PORTION'];
    return units.map(unit => `<option value="${unit}" ${unit === selected ? 'selected' : ''}>${pretty(unit)}</option>`).join('');
  }

  async function saveProduct(event, productId) {
    event.preventDefault();
    const form = event.currentTarget;
    const fd = new FormData(form);
    const payload = {
      name: fd.get('name'),
      description: fd.get('description'),
      category: fd.get('category'),
      productType: fd.get('productType'),
      state: fd.get('state'),
      lga: fd.get('lga'),
      area: fd.get('area'),
      variants: [{
        measurementValue: Number(fd.get('measurementValue')),
        measurementUnit: fd.get('measurementUnit'),
        price: Number(fd.get('price')),
        stockQuantity: Number(fd.get('stockQuantity')),
        minimumOrderQuantity: Number(fd.get('minimumOrderQuantity')),
        sku: fd.get('sku') || null,
        available: fd.get('available') === 'on'
      }]
    };

    try {
      const button = form.querySelector('button[type="submit"]');
      button.disabled = true;
      button.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Saving…';

      const saved = productId
        ? await Oyuki.ProviderProducts.update(productId, payload)
        : await Oyuki.ProviderProducts.create(payload);

      const image = fd.get('image');
      if (image && image.size) {
        await Oyuki.ProviderProducts.uploadImage(saved.id, image, true);
      }

      state.editingProductId = null;
      state.products = await Oyuki.ProviderProducts.list();
      Oyuki.Toast.show(productId ? 'Product updated' : 'Product created', 'success');
      renderProducts();
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  function openProductForm(productId = null) {
    state.editingProductId = productId || 0;
    if (!location.hash.startsWith('#products') && !location.hash.startsWith('#menu')) {
      location.hash = state.role === 'KITCHEN' ? '#menu' : '#products';
    }
    renderProducts();
    setTimeout(() => $('#provider-product-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
  }

  function cancelProductForm() {
    state.editingProductId = null;
    renderProducts();
  }

  let imageProductId = null;
  function chooseImage(productId) {
    imageProductId = productId;
    let input = $('#provider-hidden-image');
    if (!input) {
      input = document.createElement('input');
      input.id = 'provider-hidden-image';
      input.type = 'file';
      input.accept = 'image/*';
      input.className = 'd-none';
      input.addEventListener('change', uploadChosenImage);
      document.body.appendChild(input);
    }
    input.value = '';
    input.click();
  }

  async function uploadChosenImage(event) {
    const file = event.target.files?.[0];
    if (!file || !imageProductId) return;
    try {
      await Oyuki.ProviderProducts.uploadImage(imageProductId, file, true);
      state.products = await Oyuki.ProviderProducts.list();
      Oyuki.Toast.show('Product image uploaded', 'success');
      renderProducts();
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function removeProduct(productId) {
    if (!confirm('Remove this product from the marketplace?')) return;
    try {
      await Oyuki.ProviderProducts.remove(productId);
      state.products = await Oyuki.ProviderProducts.list();
      Oyuki.Toast.show('Product removed');
      renderProducts();
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  function renderOrders(section) {
    let orders = state.orders;
    if (section === 'cooking') {
      orders = orders.filter(item => item.productType === 'COOKING_SERVICE');
    }

    $('#view').innerHTML = `
      <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
        <div><h3 class="mb-1">${section === 'cooking' ? 'Cooking requests' : 'Orders'}</h3>
          <p class="text-muted mb-0">Accept, process, reject, and mark items ready for pickup.</p>
        </div>
        <button class="btn btn-outline-brand" onclick="ProviderDashboard.refreshOrders()">
          <i class="bi bi-arrow-clockwise"></i> Refresh
        </button>
      </div>
      <div class="panel mt-3">${ordersTable(orders)}</div>`;
  }

  function ordersTable(orders) {
    if (!orders.length) return '<p class="text-muted text-center py-4 mb-0">No order items yet.</p>';

    return `<div class="table-responsive"><table class="oy-table">
      <thead><tr><th>Order</th><th>Customer</th><th>Item</th><th>Total</th><th>Status</th><th></th></tr></thead>
      <tbody>${orders.map(item => `<tr>
        <td><strong>${e(item.orderNumber || `#${item.orderId || item.id}`)}</strong><div class="small text-muted">${e(Oyuki.date(item.createdAt))}</div></td>
        <td>${e(item.customerName || 'Customer')}</td>
        <td><strong>${e(item.productName)}</strong><div class="small text-muted">${item.quantity} × ${money(item.unitPrice)}</div></td>
        <td>${money(item.lineTotal)}</td>
        <td>${statusPill(item.status)}</td>
        <td>${orderActions(item)}</td>
      </tr>`).join('')}</tbody>
    </table></div>`;
  }

  function orderActions(item) {
    if (item.status === 'PENDING') {
      return `<div class="d-flex gap-1">
        <button class="btn btn-sm btn-brand" onclick="ProviderDashboard.orderAction('accept',${item.id})">Accept</button>
        <button class="btn btn-sm btn-outline-danger" onclick="ProviderDashboard.orderAction('reject',${item.id})">Reject</button>
      </div>`;
    }
    if (item.status === 'ACCEPTED') {
      return `<button class="btn btn-sm btn-brand" onclick="ProviderDashboard.orderAction('processing',${item.id})">Start processing</button>`;
    }
    if (item.status === 'PROCESSING') {
      return `<button class="btn btn-sm btn-brand" onclick="ProviderDashboard.orderAction('ready',${item.id})">Mark ready</button>`;
    }
    return '<span class="text-muted small">No action</span>';
  }

  async function orderAction(action, itemId) {
    try {
      if (action === 'reject') {
        const reason = prompt('Why are you rejecting this item?');
        if (!reason) return;
        await Oyuki.ProviderOrders.reject(itemId, reason);
      } else {
        await Oyuki.ProviderOrders[action](itemId);
      }
      state.orders = await Oyuki.ProviderOrders.list();
      Oyuki.Toast.show('Order item updated', 'success');
      route();
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function refreshOrders() {
    try {
      state.orders = await Oyuki.ProviderOrders.list();
      Oyuki.Toast.show('Orders refreshed', 'success');
      route();
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  function renderSales() {
    const rows = nonRejectedOrders();
    const total = rows.reduce((sum, item) => sum + Number(item.lineTotal || 0), 0);
    const delivered = rows.filter(item => ['RECEIVED_BY_ADMIN','DELIVERED'].includes(item.status))
      .reduce((sum, item) => sum + Number(item.lineTotal || 0), 0);

    $('#view').innerHTML = `
      <h3>Sales</h3>
      <div class="row g-3 my-3">
        ${statCard('Order item value', money(total))}
        ${statCard('Received / delivered', money(delivered))}
        ${statCard('Accepted items', rows.length)}
      </div>
      <div class="panel">${ordersTable(rows)}</div>`;
  }

  function renderReviews() {
    const rating = state.rating || {};
    $('#view').innerHTML = `
      <h3>Reviews</h3>
      <div class="row g-3 my-3">
        ${statCard('Average rating', `${Number(rating.averageRating || 0).toFixed(1)}★`)}
        ${statCard('Total reviews', Number(rating.totalReviews || state.reviews.length))}
        ${statCard('5-star reviews', Number(rating.fiveStarReviews || 0))}
      </div>
      <div class="panel">
        ${state.reviews.length ? state.reviews.map(review => `
          <div class="border-bottom py-3">
            <div class="d-flex justify-content-between gap-2">
              <strong>${e(review.customerName || 'Customer')}</strong>
              <span class="rating">${'★'.repeat(Number(review.rating || 0))}${'☆'.repeat(5 - Number(review.rating || 0))}</span>
            </div>
            <div class="small text-muted">${e(review.orderNumber || '')} · ${e(Oyuki.date(review.createdAt))}</div>
            <p class="mb-0 mt-2">${e(review.comment || 'No written comment.')}</p>
          </div>`).join('') : '<p class="text-muted text-center py-4 mb-0">No reviews yet.</p>'}
      </div>`;
  }

  function renderProfile() {
    const p = state.profile || {};
    const pickup = state.pickup || {};
    const isKitchen = state.role === 'KITCHEN';
    const missingProfileImage = !p.profileImageUrl;
    const missingIdDocument = !p.idDocumentUrl;
    const completed = Boolean(p.profileCompleted && state.pickup);

    $('#view').innerHTML = `
      <h3>${isKitchen ? 'Kitchen' : 'Seller'} profile</h3>
      <p class="text-muted">A profile picture, identification document and pickup address are compulsory before approval.</p>

      <div class="alert ${completed ? 'alert-success' : 'alert-warning'}">
        <strong>${completed ? 'Profile documents uploaded.' : 'Profile is not complete yet.'}</strong>
        ${completed
          ? ' Your application is ready for administrator review.'
          : ` Missing: ${[
              missingProfileImage ? 'profile picture' : '',
              missingIdDocument ? 'ID document' : '',
              !state.pickup ? 'pickup address' : ''
            ].filter(Boolean).join(', ')}.`}
      </div>

      <form class="panel" id="provider-profile-form" onsubmit="ProviderDashboard.saveProfile(event)">
        <div class="row g-3">
          <div class="col-md-6"><label class="form-label">${isKitchen ? 'Kitchen name' : 'Business name'}</label>
            <input class="form-control" name="${isKitchen ? 'kitchenName' : 'businessName'}" required maxlength="150" value="${e(isKitchen ? p.kitchenName || '' : p.businessName || '')}">
          </div>
          ${isKitchen ? `<div class="col-md-6"><label class="form-label">Cuisine</label>
            <input class="form-control" name="cuisine" required maxlength="100" data-letters-only data-allow-ampersand="true" value="${e(p.cuisine || '')}"></div>` : ''}
          <div class="${isKitchen ? 'col-12' : 'col-md-6'}"><label class="form-label">Bio</label>
            <textarea class="form-control" name="bio" minlength="20" maxlength="1500" required rows="3">${e(p.bio || '')}</textarea>
          </div>
          <div class="col-md-4"><label class="form-label">State</label><select class="form-select" id="profile-state" name="state" required></select></div>
          <div class="col-md-4"><label class="form-label">LGA</label><select class="form-select" id="profile-lga" name="lga" required></select></div>
          <div class="col-md-4"><label class="form-label">Area</label><select class="form-select" id="profile-area" name="area" required></select></div>
          <div class="col-12"><label class="form-label">Full address</label><input class="form-control" name="addressLine" maxlength="500" required value="${e(p.addressLine || '')}"></div>
          <div class="col-md-4"><label class="form-label">Bank name</label><input class="form-control" name="bankName" maxlength="150" data-letters-only data-allow-ampersand="true" data-validation-message="Bank name must contain letters only." value="${e(p.bankName || '')}"></div>
          <div class="col-md-4"><label class="form-label">Account name</label><input class="form-control" name="accountName" maxlength="150" data-letters-only data-validation-message="Account name must contain letters only." value="${e(p.accountName || '')}"></div>
          <div class="col-md-4"><label class="form-label">Account number</label><input class="form-control" name="accountNumber" inputmode="numeric" minlength="10" maxlength="10" pattern="[0-9]{10}" data-numbers-only data-validation-message="Account number must contain exactly 10 digits." value="${e(p.accountNumber || '')}"></div>

          <div class="col-md-4">
            <label class="form-label">Profile picture <span class="text-danger">*</span></label>
            ${p.profileImageUrl ? `<div class="mb-2"><img src="${e(Oyuki.imageUrl(p.profileImageUrl))}" alt="Profile" style="width:96px;height:96px;object-fit:cover;border-radius:12px"></div>` : ''}
            <input class="form-control" type="file" name="profileImage" accept="image/jpeg,image/png,image/webp" ${missingProfileImage ? 'required' : ''}>
            <div class="form-text">JPG, PNG or WEBP.</div>
          </div>
          <div class="col-md-4">
            <label class="form-label">Cover image</label>
            ${p.coverImageUrl ? `<div class="small text-success mb-2"><i class="bi bi-check-circle"></i> Uploaded</div>` : ''}
            <input class="form-control" type="file" name="coverImage" accept="image/jpeg,image/png,image/webp">
          </div>
          <div class="col-md-4">
            <label class="form-label">Government ID document <span class="text-danger">*</span></label>
            ${p.idDocumentUrl ? `<div class="small text-success mb-2"><i class="bi bi-check-circle"></i> ID document uploaded</div>` : ''}
            <input class="form-control" type="file" name="idDocument" accept="image/jpeg,image/png,image/webp,application/pdf" ${missingIdDocument ? 'required' : ''}>
            <div class="form-text">National ID, driver's licence, passport or CAC document.</div>
          </div>
          <div class="col-12">
            <button class="btn btn-brand" type="submit"><i class="bi bi-check-lg"></i> Save profile and documents</button>
          </div>
        </div>
      </form>

      <form class="panel mt-3" id="provider-pickup-form" onsubmit="ProviderDashboard.savePickup(event)">
        <h5>Pickup address <span class="text-danger">*</span></h5>
        <p class="small text-muted">Delivery fees are calculated from this location.</p>
        <div class="row g-3">
          <div class="col-md-4"><label class="form-label">State</label><select class="form-select" id="pickup-state" name="state" required></select></div>
          <div class="col-md-4"><label class="form-label">LGA / City</label><select class="form-select" id="pickup-lga" name="lga" required></select></div>
          <div class="col-md-4"><label class="form-label">Area</label><select class="form-select" id="pickup-area" name="area" required></select></div>
          <div class="col-md-8"><label class="form-label">Street address</label><input class="form-control" name="streetAddress" required maxlength="500" value="${e(pickup.streetAddress || '')}"></div>
          <div class="col-md-4"><label class="form-label">Landmark</label><input class="form-control" name="landmark" maxlength="255" value="${e(pickup.landmark || '')}"></div>
          <div class="col-12"><button class="btn btn-outline-brand" type="submit">Save pickup address</button></div>
        </div>
      </form>`;

    bindLocations('profile', p.state, p.lga, p.area, 'name');
    bindLocations('pickup', pickup.state || pickup.stateName, pickup.lga || pickup.city, pickup.area, 'enum');
    Oyuki.Forms.bindGuards($('#provider-profile-form'));
  }

  async function bindLocations(prefix, selectedState, selectedLga, selectedArea, stateValue) {
    if (!window.OyukiLocations) return;
    await OyukiLocations.bind({
      stateSelect: document.getElementById(`${prefix}-state`),
      lgaSelect: document.getElementById(`${prefix}-lga`),
      areaSelect: document.getElementById(`${prefix}-area`),
      selectedState: selectedState || '',
      selectedLga: selectedLga || '',
      selectedArea: selectedArea || '',
      stateValue
    });
  }

  async function saveProfile(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const fd = new FormData(form);
    const old = state.profile || {};
    const isKitchen = state.role === 'KITCHEN';

    const payload = {
      ...(isKitchen ? {
        kitchenName: fd.get('kitchenName'),
        cuisine: fd.get('cuisine')
      } : {
        businessName: fd.get('businessName')
      }),
      bio: fd.get('bio'),
      profileImageUrl: old.profileImageUrl || null,
      coverImageUrl: old.coverImageUrl || null,
      state: fd.get('state'),
      lga: fd.get('lga'),
      area: fd.get('area'),
      addressLine: fd.get('addressLine'),
      latitude: old.latitude || null,
      longitude: old.longitude || null,
      idDocumentUrl: old.idDocumentUrl || null,
      bankName: fd.get('bankName') || null,
      accountName: fd.get('accountName') || null,
      accountNumber: fd.get('accountNumber') || null
    };

    const profileFile = fd.get('profileImage');
    const idFile = fd.get('idDocument');
    if (!old.profileImageUrl && (!profileFile || !profileFile.size)) {
      Oyuki.Toast.show('Upload a profile picture before completing your profile.', 'error');
      return;
    }
    if (!old.idDocumentUrl && (!idFile || !idFile.size)) {
      Oyuki.Toast.show('Upload an identification document before completing your profile.', 'error');
      return;
    }

    try {
      let saved = await Oyuki.ProviderProfiles.save(state.role, payload);
      state.profile = saved?.data ?? saved;

      const uploads = [
        ['profile-image', fd.get('profileImage')],
        ['cover-image', fd.get('coverImage')],
        ['id-document', fd.get('idDocument')]
      ];

      for (const [kind, file] of uploads) {
        if (file && file.size) await Oyuki.ProviderProfiles.upload(state.role, kind, file);
      }

      state.profile = await safe(() => Oyuki.ProviderProfiles.get(state.role).then(x => x?.data ?? x), state.profile);
      Oyuki.Toast.show('Profile saved successfully', 'success');
      renderProfile();
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  async function savePickup(event) {
    event.preventDefault();
    const fd = new FormData(event.currentTarget);
    try {
      state.pickup = await Oyuki.PickupAddress.save({
        state: fd.get('state'),
        city: fd.get('lga'),
        lga: fd.get('lga'),
        area: fd.get('area'),
        streetAddress: fd.get('streetAddress'),
        landmark: fd.get('landmark') || null,
        latitude: null,
        longitude: null,
        active: true
      });
      Oyuki.Toast.show('Pickup address saved', 'success');
      renderProfile();
    } catch (error) {
      Oyuki.Toast.show(error.message, 'error');
    }
  }

  function statusPill(status) {
    const value = String(status || 'UNKNOWN');
    const cls = value.includes('REJECT') || value.includes('CANCEL') || value.includes('DELETE')
      ? 'status-rejected'
      : value.includes('READY') || value.includes('ACTIVE') || value.includes('DELIVERED') || value.includes('RECEIVED')
        ? 'status-approved'
        : value.includes('PROCESS') || value.includes('ACCEPT')
          ? 'status-cooking'
          : 'status-pending';
    return `<span class="status-pill ${cls}">${e(pretty(value))}</span>`;
  }

  async function refresh() {
    await refreshAll();
    Oyuki.Toast.show('Dashboard refreshed', 'success');
    route();
  }

  window.ProviderDashboard = {
    refresh,
    refreshOrders,
    openProductForm,
    cancelProductForm,
    saveProduct,
    chooseImage,
    uploadChosenImage,
    removeProduct,
    orderAction,
    saveProfile,
    savePickup
  };
})();
