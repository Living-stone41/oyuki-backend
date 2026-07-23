/* Oyuki HTML frontend — Spring Boot API connected */
(function () {
  'use strict';

  const API_ORIGIN = window.location.origin;
  const API_BASE = API_ORIGIN + '/api';
  const FALLBACK_IMAGE = 'assets/images/hero.jpg';
  const STORAGE = {
    token: 'oyuki_token',
    user: 'oyuki_user',
    pendingContact: 'oyuki_pending_contact',
    recentlyViewed: 'oyuki_recently_viewed'
  };

  // Kept only so old non-connected dashboard scripts do not crash.
  const K = {
    users: 'oy_users', cart: 'oy_cart', wishlist: 'oy_wishlist', orders: 'oy_orders',
    products: 'oy_products', meals: 'oy_meals', kitchens: 'oy_kitchens',
    contacts: 'oy_contacts', reviews: 'oy_reviews', notifs: 'oy_notifs', addresses: 'oy_addresses'
  };
  const load = (key, fallback = []) => {
    try { return JSON.parse(localStorage.getItem(key)) ?? fallback; }
    catch { return fallback; }
  };
  const save = (key, value) => localStorage.setItem(key, JSON.stringify(value));

  const Toast = {
    show(message, kind = '') {
      const el = document.createElement('div');
      el.className = 'oy-toast ' + kind;
      el.textContent = message;
      document.body.appendChild(el);
      requestAnimationFrame(() => el.classList.add('show'));
      setTimeout(() => {
        el.classList.remove('show');
        setTimeout(() => el.remove(), 250);
      }, 3000);
    }
  };

  function errorMessage(payload, fallback) {
    if (!payload) return fallback;
    if (typeof payload === 'string') return payload;
    return payload.message || payload.rootMessage || payload.error || fallback;
  }

  const Api = {
    async request(path, options = {}) {
      const {
        method = 'GET', body, auth = true, headers = {}, raw = false
      } = options;
      const finalHeaders = { Accept: 'application/json', ...headers };
      const token = localStorage.getItem(STORAGE.token);
      if (auth && token) finalHeaders.Authorization = `Bearer ${token}`;

      let requestBody = body;
      if (body !== undefined && body !== null && !(body instanceof FormData)) {
        finalHeaders['Content-Type'] = 'application/json';
        requestBody = JSON.stringify(body);
      }

      let response;
      try {
        response = await fetch(API_BASE + path, {
          method, headers: finalHeaders, body: requestBody
        });
      } catch (error) {
        throw new Error('Cannot reach the Oyuki backend. Start Spring Boot on http://localhost:8080.');
      }

      if (raw) {
        if (!response.ok) throw new Error(`Request failed (${response.status})`);
        return response;
      }

      const text = await response.text();
      let payload = null;
      if (text) {
        try { payload = JSON.parse(text); }
        catch { payload = text; }
      }

      if (!response.ok) {
        if (response.status === 401 && auth && token) {
          localStorage.removeItem(STORAGE.token);
          localStorage.removeItem(STORAGE.user);
        }
        throw new Error(errorMessage(payload, `Request failed (${response.status})`));
      }
      return payload;
    },
    get(path, auth = true) { return this.request(path, { auth }); },
    post(path, body, auth = true) { return this.request(path, { method: 'POST', body, auth }); },
    put(path, body, auth = true) { return this.request(path, { method: 'PUT', body, auth }); },
    patch(path, body, auth = true) { return this.request(path, { method: 'PATCH', body, auth }); },
    delete(path, auth = true) { return this.request(path, { method: 'DELETE', auth }); }
  };

  function unwrap(payload) {
    return payload && Object.prototype.hasOwnProperty.call(payload, 'data')
      ? payload.data
      : payload;
  }

  function rolePage(role) {
    const value = String(role || '').toUpperCase();
    if (value === 'CUSTOMER') return 'customer.html';
    if (value === 'SELLER') return 'seller.html';
    if (value === 'KITCHEN') return 'kitchen.html';
    if (value === 'ADMIN') return 'admin.html';
    if (value === 'RIDER') return 'tracking.html';
    return 'home.html';
  }

  const Auth = {
    current() { return load(STORAGE.user, null); },
    token() { return localStorage.getItem(STORAGE.token); },
    isCustomer() {
      const user = this.current();
      return user && String(user.role).toUpperCase() === 'CUSTOMER';
    },
    async login(identifier, password) {
      const result = unwrap(await Api.post('/auth/login', { identifier, password }, false));
      localStorage.setItem(STORAGE.token, result.token);
      let user = {
        id: result.userId,
        userId: result.userId,
        fullName: result.fullName,
        role: result.role,
        status: result.status
      };
      save(STORAGE.user, user);
      try {
        const me = unwrap(await Api.get('/users/me'));
        user = { ...user, ...me, userId: me.id || result.userId };
        save(STORAGE.user, user);
      } catch (_) {}
      return user;
    },
    async register(data) {
      const response = await Api.post('/auth/register', data, false);
      localStorage.setItem(STORAGE.pendingContact, data.email || data.phoneNumber || '');
      return response;
    },
    async verifyRegistration(contact, token) {
      const response = await Api.post('/auth/verify-registration', { contact, token }, false);
      localStorage.removeItem(STORAGE.pendingContact);
      return response;
    },
    pendingContact() { return localStorage.getItem(STORAGE.pendingContact) || ''; },
    logout() {
      localStorage.removeItem(STORAGE.token);
      localStorage.removeItem(STORAGE.user);
      location.href = 'login.html';
    },
    require(role) {
      const user = this.current();
      if (!user || !this.token()) {
        const redirect = location.pathname.split('/').pop() + location.hash;
        location.href = 'login.html?redirect=' + encodeURIComponent(redirect);
        return null;
      }
      if (role && String(user.role).toUpperCase() !== String(role).toUpperCase() && String(user.role).toUpperCase() !== 'ADMIN') {
        Toast.show('You are not allowed to open this dashboard.', 'error');
        setTimeout(() => location.href = rolePage(user.role), 400);
        return null;
      }
      return user;
    },
    async me() {
      const user = unwrap(await Api.get('/users/me'));
      save(STORAGE.user, { ...this.current(), ...user, userId: user.id });
      return user;
    }
  };

  function imageUrl(value) {
    if (!value) return FALLBACK_IMAGE;
    if (/^https?:\/\//i.test(value)) return value;
    if (value.startsWith('/uploads/')) return API_ORIGIN + value;
    if (value.startsWith('uploads/')) return API_ORIGIN + '/' + value;
    return value;
  }

  function unitLabel(variant) {
    if (!variant) return '';
    const raw = String(variant.measurementUnit || '').replaceAll('_', ' ').toLowerCase();
    const unit = raw ? raw.charAt(0).toUpperCase() + raw.slice(1) : '';
    return `${Number(variant.measurementValue || 0).toLocaleString('en-NG')} ${unit}`;
  }


  function primaryVariant(product) {
    const variants = Array.isArray(product.variants) ? product.variants : [];
    return variants.find(v => v.available && Number(v.stockQuantity) > 0) || variants[0] || null;
  }

  function normalizeProduct(product) {
    const variant = primaryVariant(product);
    const images = Array.isArray(product.images) ? product.images : [];
    const primary = images.find(i => i.primaryImage) || images[0];
    return {
      ...product,
      price: Number(variant?.price || 0),
      variantId: variant?.id || null,
      variant,
      unit: unitLabel(variant),
      image: imageUrl(primary?.imageUrl),
      rating: Number(product.averageRating || 0),
      desc: product.description || '',
      seller: product.ownerName || '',
      location: [product.area, product.lga, product.state].filter(Boolean).join(', ')
    };
  }

  const Products = {
    async list(filters = {}) {
      const params = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '' && value !== 'All') params.set(key, value);
      });
      const suffix = params.toString() ? '?' + params.toString() : '';
      const products = await Api.get('/marketplace/products' + suffix, false);
      return (products || []).map(normalizeProduct);
    },
    async get(id) {
      return normalizeProduct(await Api.get(`/marketplace/products/${id}`, false));
    },
    async mine() {
      return (await Api.get('/products') || []).map(normalizeProduct);
    }
  };

  const Cart = {
    async get() { return await Api.get('/cart'); },
    async add(variantId, quantity = 1) {
      const user = Auth.current();
      if (!user || !Auth.token()) {
        location.href = 'login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
        return null;
      }
      if (!Auth.isCustomer()) throw new Error('Only customer accounts can add items to cart.');
      const cart = await Api.post('/cart/items', { variantId: Number(variantId), quantity: Number(quantity) });
      await this.updateBadge(cart);
      Toast.show('Added to cart', 'success');
      return cart;
    },
    async update(cartItemId, quantity) {
      const cart = await Api.put(`/cart/items/${cartItemId}`, { quantity: Number(quantity) });
      await this.updateBadge(cart);
      return cart;
    },
    async remove(cartItemId) {
      const cart = await Api.delete(`/cart/items/${cartItemId}`);
      await this.updateBadge(cart);
      Toast.show('Item removed');
      return cart;
    },
    async clear() {
      const cart = await Api.delete('/cart/clear');
      await this.updateBadge(cart);
      return cart;
    },
    async updateBadge(cartData = null) {
      const badge = document.getElementById('cart-badge');
      if (!badge) return;
      if (!Auth.isCustomer() || !Auth.token()) {
        badge.style.display = 'none';
        return;
      }
      try {
        const cart = cartData || await this.get();
        const count = Number(cart?.totalItems || 0);
        badge.textContent = count;
        badge.style.display = count ? 'inline-block' : 'none';
      } catch (_) { badge.style.display = 'none'; }
    }
  };

  const Wishlist = {
    async list() { return await Api.get('/wishlist'); },
    async check(productId) { return await Api.get(`/wishlist/check/${productId}`); },
    async add(productId) { return await Api.post(`/wishlist/products/${productId}`, {}); },
    async remove(productId) { return await Api.delete(`/wishlist/products/${productId}`); },
    async toggle(productId, button = null) {
      if (!Auth.isCustomer()) {
        location.href = 'login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
        return;
      }
      const saved = button?.dataset.saved === 'true';
      if (saved) {
        await this.remove(productId);
        if (button) button.dataset.saved = 'false';
        Toast.show('Removed from wishlist');
      } else {
        await this.add(productId);
        if (button) button.dataset.saved = 'true';
        Toast.show('Saved to wishlist', 'success');
      }
      if (button) button.innerHTML = `<i class="bi bi-heart${button.dataset.saved === 'true' ? '-fill' : ''}"></i>`;
    }
  };

  const Addresses = {
    list() { return Api.get('/addresses'); },
    get(id) { return Api.get(`/addresses/${id}`); },
    create(data) { return Api.post('/addresses', data); },
    update(id, data) { return Api.put(`/addresses/${id}`, data); },
    setDefault(id) { return Api.patch(`/addresses/${id}/default`, {}); },
    remove(id) { return Api.delete(`/addresses/${id}`); }
  };

  const Delivery = {
    calculate(addressId) { return Api.post('/delivery-fees/calculate', { addressId: Number(addressId) }); }
  };

  const Coupons = {
    validate(data) { return Api.post('/coupons/validate', data); }
  };

  const Orders = {
    checkout(data) { return Api.post('/orders/checkout', data); },
    list() { return Api.get('/orders/my'); },
    get(id) { return Api.get(`/orders/my/${id}`); }
  };

  const CustomerPayments = {
    bankAccount() { return Api.get('/payments/bank-account'); },
    list() { return Api.get('/payments/my'); },
    forOrder(orderId) { return Api.get(`/payments/orders/${orderId}`); },
    upload(orderId, data) {
      return Api.request(`/payments/orders/${orderId}/proof`, {
        method: 'POST',
        body: data
      });
    },
    async receipt(id) {
      const response = await Api.request(`/payments/proofs/${id}/receipt`, { raw: true });
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    }
  };

  const Recommendations = {
    record(product) {
      if (!product?.id) return;
      const existing = load(STORAGE.recentlyViewed, []);
      const item = {
        id: product.id,
        name: product.name,
        category: product.category,
        ownerName: product.ownerName,
        image: product.image,
        price: product.price,
        unit: product.unit,
        variantId: product.variantId,
        rating: product.rating,
        viewCount: Number(product.viewCount || 0),
        viewedAt: new Date().toISOString()
      };
      save(
        STORAGE.recentlyViewed,
        [item, ...existing.filter(entry => Number(entry.id) !== Number(item.id))].slice(0, 12)
      );
    },
    recent(limit = 4) {
      return load(STORAGE.recentlyViewed, []).slice(0, limit);
    },
    async popular(excludedIds = [], limit = 4) {
      const excluded = new Set(excludedIds.map(Number));
      const products = await Products.list();
      return products
        .filter(product => !excluded.has(Number(product.id)))
        .sort((a, b) =>
          Number(b.viewCount || 0) - Number(a.viewCount || 0)
          || Number(b.rating || 0) - Number(a.rating || 0)
        )
        .slice(0, limit);
    }
  };

  const Notifications = {
    list(unreadOnly = false) { return Api.get(`/notifications?unreadOnly=${unreadOnly}`); },
    markRead(id) { return Api.patch(`/notifications/${id}/read`, {}); },
    readAll() { return Api.patch('/notifications/read-all', {}); }
  };

  const Password = {
    forgot(contact) { return Api.post('/auth/forgot-password', { contact }, false); },
    verify(contact, token) { return Api.post('/auth/verify-reset-token', { contact, token }, false); },
    reset(contact, token, newPassword, confirmPassword) {
      return Api.post('/auth/reset-password', { contact, token, newPassword, confirmPassword }, false);
    }
  };


  const ProviderProfiles = {
    get(role) {
      const type = String(role || '').toUpperCase();
      return Api.get(type === 'KITCHEN' ? '/kitchen/profile' : '/seller/profile');
    },
    save(role, data) {
      const type = String(role || '').toUpperCase();
      return Api.put(type === 'KITCHEN' ? '/kitchen/profile' : '/seller/profile', data);
    },
    upload(role, kind, file) {
      const type = String(role || '').toUpperCase();
      const base = type === 'KITCHEN' ? '/kitchen/profile' : '/seller/profile';
      const form = new FormData();
      form.append('file', file);
      return Api.request(`${base}/${kind}`, { method: 'POST', body: form });
    }
  };

  const ProviderProducts = {
    list() { return Api.get('/products'); },
    get(id) { return Api.get(`/products/${id}`); },
    create(data) { return Api.post('/products', data); },
    update(id, data) { return Api.put(`/products/${id}`, data); },
    remove(id) { return Api.delete(`/products/${id}`); },
    uploadImage(productId, file, primary = true) {
      const form = new FormData();
      form.append('file', file);
      form.append('primary', String(primary));
      return Api.request(`/products/${productId}/images`, { method: 'POST', body: form });
    },
    deleteImage(productId, imageId) {
      return Api.delete(`/products/${productId}/images/${imageId}`);
    }
  };

  const ProviderOrders = {
    list(status = '') {
      const suffix = status ? `?status=${encodeURIComponent(status)}` : '';
      return Api.get('/orders/provider/items' + suffix);
    },
    accept(id) { return Api.patch(`/orders/provider/items/${id}/accept`, {}); },
    reject(id, reason) { return Api.patch(`/orders/provider/items/${id}/reject`, { reason }); },
    processing(id) { return Api.patch(`/orders/provider/items/${id}/processing`, {}); },
    ready(id) { return Api.patch(`/orders/provider/items/${id}/ready`, {}); }
  };

  const PickupAddress = {
    get() { return Api.get('/provider/pickup-address'); },
    save(data) { return Api.put('/provider/pickup-address', data); }
  };

  const PublicReviews = {
    provider(providerId) { return Api.get(`/reviews/providers/${providerId}`, false); },
    providerSummary(providerId) { return Api.get(`/reviews/providers/${providerId}/summary`, false); }
  };

  const AdminUsers = {
    list(filters = {}) {
      const params = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') params.set(key, value);
      });
      return Api.get('/admin/users' + (params.toString() ? '?' + params.toString() : ''));
    },
    statistics() { return Api.get('/admin/users/statistics'); },
    updateStatus(id, status, reason = '') {
      return Api.patch(`/admin/users/${id}/status`, { status, reason: reason || null });
    }
  };

  const AdminApplications = {
    pending() { return Api.get('/admin/applications/pending').then(unwrap); },
    get(userId) { return Api.get(`/admin/applications/${userId}`).then(unwrap); },
    approve(userId) { return Api.patch(`/admin/applications/${userId}/approve`, {}).then(unwrap); },
    reject(userId, reason) {
      return Api.patch(`/admin/applications/${userId}/reject`, { reason }).then(unwrap);
    }
  };

  const AdminOrders = {
    list() { return Api.get('/admin/orders'); },
    get(id) { return Api.get(`/admin/orders/${id}`); },
    markReceived(itemId) { return Api.patch(`/admin/orders/items/${itemId}/received`, {}); }
  };

  const AdminPayments = {
    list(status = '') {
      const suffix = status ? `?status=${encodeURIComponent(status)}` : '';
      return Api.get('/admin/payments' + suffix);
    },
    confirm(id, note = '') {
      return Api.patch(`/admin/payments/${id}/confirm`, { note: note || null });
    },
    reject(id, reason) {
      return Api.patch(`/admin/payments/${id}/reject`, { reason });
    },
    async receipt(id) {
      const response = await Api.request(`/admin/payments/${id}/receipt`, { raw: true });
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    }
  };

  const AdminFiles = {
    async open(protectedUrl) {
      if (!protectedUrl) throw new Error('Document is not available.');
      let path = protectedUrl;
      if (/^https?:\/\//i.test(path)) {
        path = new URL(path).pathname;
      }
      if (path.startsWith('/api/')) path = path.slice(4);
      const response = await Api.request(path, { raw: true });
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank', 'noopener');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    }
  };

  const Forms = {
    togglePassword(inputId, button) {
      const input = document.getElementById(inputId);
      if (!input) return;
      const showing = input.type === 'text';
      input.type = showing ? 'password' : 'text';
      if (button) {
        button.innerHTML = `<i class="bi bi-eye${showing ? '' : '-slash'}"></i>`;
        button.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
      }
    },
    bindGuards(root = document) {
      root.querySelectorAll('[data-letters-only]').forEach(input => {
        const allowAmpersand = input.dataset.allowAmpersand === 'true';
        const pattern = allowAmpersand
          ? /^[A-Za-z][A-Za-z .&'\-]*$/
          : /^[A-Za-z][A-Za-z .'\-]*$/;
        const message = input.dataset.validationMessage
          || 'Use letters and spaces only.';
        const validate = () => {
          const value = input.value.trim();
          input.setCustomValidity(!value || pattern.test(value) ? '' : message);
        };
        input.addEventListener('input', validate);
        input.addEventListener('blur', validate);
      });

      root.querySelectorAll('[data-numbers-only]').forEach(input => {
        const message = input.dataset.validationMessage
          || 'Use numbers only.';
        const validate = () => {
          const value = input.value.trim();
          input.setCustomValidity(!value || /^[0-9]+$/.test(value) ? '' : message);
        };
        input.addEventListener('input', validate);
        input.addEventListener('blur', validate);
      });
    }
  };

  const fmt = value => '₦'  + Number(value || 0).toLocaleString('en-NG', { maximumFractionDigits: 2 });
  const escapeHtml = value => String(value ?? '').replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));
  const date = value => value ? new Date(value).toLocaleString() : '—';

  function productCard(product) {
    const p = normalizeProduct(product);
    const disabled = !p.variantId || !p.variant?.available || Number(p.variant?.stockQuantity || 0) <= 0;
    return `<div class="col-sm-6 col-lg-3 mb-4">
      <div class="product-card h-100">
        <a href="product.html?id=${p.id}" class="product-img"><img src="${escapeHtml(p.image)}" alt="${escapeHtml(p.name)}" onerror="this.src='${FALLBACK_IMAGE}'"></a>
        <div class="p-3">
          <div class="d-flex justify-content-between gap-2"><span class="badge-soft">${escapeHtml(p.category || p.productType)}</span><span class="rating">★ ${p.rating.toFixed(1)}</span></div>
          <a class="text-decoration-none text-dark" href="product.html?id=${p.id}"><h6 class="mt-2 mb-1">${escapeHtml(p.name)}</h6></a>
          <div class="text-muted small mb-2">${escapeHtml(p.ownerName || '')}${p.location ? ' · ' + escapeHtml(p.location) : ''}</div>
          <div class="d-flex align-items-end justify-content-between gap-2">
            <div><strong class="text-success">${fmt(p.price)}</strong><div class="text-muted small">${escapeHtml(p.unit)}</div></div>
            <div class="d-flex gap-1">
              <button class="btn btn-sm btn-outline-brand" data-saved="false" onclick="event.preventDefault();Oyuki.Wishlist.toggle(${p.id},this)"><i class="bi bi-heart"></i></button>
              <button class="btn btn-sm btn-brand" ${disabled ? 'disabled' : ''} onclick="event.preventDefault();Oyuki.addToCart(${p.variantId},1)"><i class="bi bi-basket2"></i></button>
            </div>
          </div>
        </div>
      </div>
    </div>`;
  }

  function kitchenCard(kitchen) {
    return `<div class="col-md-4 mb-4"><div class="kitchen-card h-100">
      <img src="${escapeHtml(kitchen.image || FALLBACK_IMAGE)}" alt="${escapeHtml(kitchen.name)}" onerror="this.src='${FALLBACK_IMAGE}'">
      <div class="p-3"><h5>${escapeHtml(kitchen.name)}</h5><p class="text-muted small mb-2">Kitchen products on Oyuki</p>
      <a href="kitchen-detail.html?id=${kitchen.id}" class="btn btn-outline-brand btn-sm">View meals</a></div>
    </div></div>`;
  }

  function kitchensFromProducts(products) {
    const map = new Map();
    products.filter(p => String(p.ownerRole).toUpperCase() === 'KITCHEN').forEach(p => {
      if (!map.has(p.ownerId)) map.set(p.ownerId, { id: p.ownerId, name: p.ownerName, image: p.image });
    });
    return [...map.values()];
  }

  async function addToCart(variantId, quantity = 1) {
    try { return await Cart.add(variantId, quantity); }
    catch (error) { Toast.show(error.message, 'error'); return null; }
  }

  function renderNav() {
    const holder = document.getElementById('oy-navbar');
    if (!holder) return;
    const user = Auth.current();
    const account = user
      ? `<div class="dropdown"><button class="btn btn-ghost dropdown-toggle" data-bs-toggle="dropdown"><i class="bi bi-person-circle"></i> ${escapeHtml((user.fullName || 'Account').split(' ')[0])}</button>
         <ul class="dropdown-menu dropdown-menu-end"><li><a class="dropdown-item" href="${rolePage(user.role)}">Dashboard</a></li><li><a class="dropdown-item" href="feature-center.html">Feature Centre</a></li>${Auth.isCustomer() ? '<li><a class="dropdown-item" href="cart.html">Cart</a></li>' : ''}<li><hr class="dropdown-divider"></li><li><a class="dropdown-item" href="#" onclick="Oyuki.Auth.logout();return false">Log out</a></li></ul></div>`
      : `<a href="login.html" class="btn btn-ghost">Log in</a><a href="register.html" class="btn btn-brand ms-2">Sign up</a>`;
    holder.innerHTML = `<nav class="oy-nav"><div class="container d-flex align-items-center justify-content-between">
      <a href="home.html" class="brand"><span class="dot"></span> Oyuki</a>
      <button class="btn btn-ghost d-lg-none" type="button" data-bs-toggle="offcanvas" data-bs-target="#oyMenu"><i class="bi bi-list"></i></button>
      <div class="d-none d-lg-flex align-items-center gap-1"><a class="nav-link" href="home.html">Home</a><a class="nav-link" href="shop.html">Marketplace</a><a class="nav-link" href="meals.html">Ready Meals</a><a class="nav-link" href="kitchens.html">Kitchens</a><a class="nav-link" href="about.html">About</a><a class="nav-link" href="contact.html">Contact</a></div>
      <div class="d-none d-lg-flex align-items-center">${Auth.isCustomer() ? '<a href="cart.html" class="btn btn-ghost me-2"><i class="bi bi-basket2"></i><span id="cart-badge" class="cart-badge" style="display:none">0</span></a>' : ''}${account}</div>
    </div></nav>
    <div class="offcanvas offcanvas-end" id="oyMenu"><div class="offcanvas-header"><a href="home.html" class="brand"><span class="dot"></span> Oyuki</a><button class="btn-close" data-bs-dismiss="offcanvas"></button></div><div class="offcanvas-body d-flex flex-column gap-2"><a class="nav-link" href="home.html">Home</a><a class="nav-link" href="shop.html">Marketplace</a><a class="nav-link" href="meals.html">Ready Meals</a><a class="nav-link" href="kitchens.html">Kitchens</a><a class="nav-link" href="about.html">About</a><a class="nav-link" href="contact.html">Contact</a><hr>${account}</div></div>`;
    const current = location.pathname.split('/').pop();
    holder.querySelectorAll('.nav-link').forEach(a => {
      if (a.getAttribute('href') === current) a.classList.add('active');
    });
  }

function renderFooter() {
  const holder = document.getElementById('oy-footer');
  if (!holder) return;

  holder.innerHTML = `
    <footer class="oy-footer">
      <div class="footer-shape footer-shape-one"></div>
      <div class="footer-shape footer-shape-two"></div>

      <div class="container position-relative">
        <div class="footer-cta">
          <div>
            <span class="footer-eyebrow">Oyuki Marketplace</span>
            <h2>Fresh produce and trusted meals, all in one place.</h2>
            <p>
              Discover farmers, food sellers and kitchens serving customers
              across Nigeria.
            </p>
          </div>

          <a href="shop.html" class="btn footer-cta-btn">
            Shop now
            <i class="bi bi-arrow-right"></i>
          </a>
        </div>

        <div class="row g-5 footer-main">
          <div class="col-lg-4 col-md-6">
            <a href="home.html" class="footer-brand">
              <span class="footer-brand-icon">O</span>
              <span>Oyuki</span>
            </a>

            <p class="footer-description">
              Fresh farm produce, ready meals and trusted kitchens delivered
              conveniently across Nigeria.
            </p>

            <div class="footer-socials">
              <a
                href="https://www.instagram.com/YOUR_INSTAGRAM_USERNAME"
                target="_blank"
                rel="noopener noreferrer"
                aria-label="Instagram"
              >
                <i class="bi bi-instagram"></i>
              </a>

              <a
                href="https://www.facebook.com/YOUR_FACEBOOK_USERNAME"
                target="_blank"
                rel="noopener noreferrer"
                aria-label="Facebook"
              >
                <i class="bi bi-facebook"></i>
              </a>

              <a
                href="https://www.tiktok.com/@YOUR_TIKTOK_USERNAME"
                target="_blank"
                rel="noopener noreferrer"
                aria-label="TikTok"
              >
                <i class="bi bi-tiktok"></i>
              </a>

              <a
                href="https://www.youtube.com/@YOUR_YOUTUBE_USERNAME"
                target="_blank"
                rel="noopener noreferrer"
                aria-label="YouTube"
              >
                <i class="bi bi-youtube"></i>
              </a>

              <a
                href="https://wa.me/2347013403517?text=Hello%20Oyuki%2C%20I%20would%20like%20to%20make%20an%20enquiry."
                target="_blank"
                rel="noopener noreferrer"
                aria-label="WhatsApp"
              >
                <i class="bi bi-whatsapp"></i>
              </a>
            </div>
          </div>

          <div class="col-6 col-md-3 col-lg-2">
            <h6 class="footer-title">Explore</h6>

            <div class="footer-links">
              <a href="shop.html">Marketplace</a>
              <a href="meals.html">Ready Meals</a>
              <a href="kitchens.html">Kitchens</a>
              <a href="home.html#freshFromFarm">Farm Produce</a>
              <a href="contact.html#complaint">Complaints</a>
            </div>
          </div>

          <div class="col-6 col-md-3 col-lg-2">
            <h6 class="footer-title">Company</h6>

            <div class="footer-links">
              <a href="about.html">About Oyuki</a>
              <a href="contact.html">Contact</a>
              <a href="feature-center.html">Export Requests</a>
              <a href="login.html">Login</a>
              <a href="register.html">Create Account</a>
            </div>
          </div>

          <div class="col-lg-4 col-md-6">
            <div class="footer-newsletter-card">
              <div class="footer-newsletter-icon">
                <i class="bi bi-envelope-paper-heart"></i>
              </div>

              <h6>Join our newsletter</h6>

              <p>
                Get product updates, special offers and Farmers' Day
                announcements.
              </p>

              <form id="newsletterForm" class="footer-newsletter-form">
                <div class="footer-input-group">
                  <i class="bi bi-envelope"></i>

                  <input
                    id="newsletterEmail"
                    type="email"
                    class="form-control"
                    placeholder="Your email address"
                    autocomplete="email"
                    required
                  >
                </div>

                <button
                  class="btn footer-subscribe-btn"
                  type="submit"
                >
                  <span>Subscribe</span>
                  <i class="bi bi-send"></i>
                </button>
              </form>

              <div
                id="newsletterMessage"
                class="footer-newsletter-message"
                aria-live="polite"
              ></div>

              <small class="footer-privacy">
                <i class="bi bi-shield-check"></i>
                No spam. Unsubscribe anytime.
              </small>
            </div>
          </div>
        </div>

        <div class="footer-divider"></div>

        <div class="footer-bottom">
          <span>
            © ${new Date().getFullYear()} Oyuki Marketplace.
            All rights reserved.
          </span>

          <div class="footer-bottom-links">
            <a href="privacy.html">Privacy</a>
            <a href="terms.html">Terms</a>

            <span>
              Made with
              <i class="bi bi-heart-fill"></i>
              in Nigeria
            </span>
          </div>
        </div>
      </div>
    </footer>
  `;

  const form = document.getElementById('newsletterForm');

  if (form) {
    form.addEventListener('submit', async event => {
      event.preventDefault();

      const emailInput =
        document.getElementById('newsletterEmail');

      const message =
        document.getElementById('newsletterMessage');

      const button =
        form.querySelector('button[type="submit"]');

      if (!emailInput || !message || !button) return;

      const email = emailInput.value.trim();
      const originalButton = button.innerHTML;

      button.disabled = true;

      button.innerHTML = `
        <span
          class="spinner-border spinner-border-sm"
          aria-hidden="true"
        ></span>
        Subscribing...
      `;

      message.textContent = '';
      message.className = 'footer-newsletter-message';

      try {
        const result = await Api.post(
          '/newsletter/subscribe',
          { email },
          false
        );

        message.textContent =
          result?.message ||
          'Subscribed successfully. Welcome to Oyuki!';

        message.classList.add('success');

        form.reset();

        Toast.show(
          message.textContent,
          'success'
        );
      } catch (error) {
        message.textContent =
          error?.message ||
          'Unable to subscribe. Please try again.';

        message.classList.add('error');

        Toast.show(
          message.textContent,
          'error'
        );
      } finally {
        button.disabled = false;
        button.innerHTML = originalButton;
      }
    });
  }
}
  window.Oyuki = {
    API_ORIGIN, API_BASE, Api, Auth, Products, Cart, Wishlist, Addresses,
    Delivery, Coupons, Orders, CustomerPayments, Recommendations,
    Notifications, Password, Toast, Forms,
    ProviderProfiles, ProviderProducts, ProviderOrders, PickupAddress,
    PublicReviews, AdminUsers, AdminApplications, AdminOrders, AdminPayments,
    AdminFiles,
    fmt, date, imageUrl, normalizeProduct, productCard, kitchenCard,
    kitchensFromProducts, addToCart, escapeHtml, rolePage, unwrap,
    K, load, save,
    // Legacy synchronous fallbacks for provider/admin pages not yet migrated.
    products: () => [], meals: () => [], kitchens: () => []
  };

  document.addEventListener('DOMContentLoaded', () => {
    renderNav();
    renderFooter();
    Forms.bindGuards();
    Cart.updateBadge();
  });
})();
