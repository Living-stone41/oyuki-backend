(function () {
  'use strict';

  /*
   * Supports both names used by previous admin-api.js versions.
   */
  const LoadedApi =
    window.OyukiAdminApi ||
    window.AdminApi;

  if (!LoadedApi) {
    console.error(
      'admin-api.js did not load or did not create an API object.'
    );

    document
      .querySelectorAll('.loading-block')
      .forEach(element => {
        element.textContent =
          'Admin API JavaScript did not load.';
      });

    return;
  }

  /*
   * Normalises the API object so this dashboard works with
   * both the old and new admin-api.js structures.
   */
  const Api = {
    get(path) {
      if (
        typeof LoadedApi.get ===
        'function'
      ) {
        return LoadedApi.get(path);
      }

      return LoadedApi.request(path);
    },

    post(path, body) {
      if (
        typeof LoadedApi.post ===
        'function'
      ) {
        return LoadedApi.post(
          path,
          body
        );
      }

      return LoadedApi.request(
        path,
        {
          method: 'POST',
          body
        }
      );
    },

    put(path, body) {
      if (
        typeof LoadedApi.put ===
        'function'
      ) {
        return LoadedApi.put(
          path,
          body
        );
      }

      return LoadedApi.request(
        path,
        {
          method: 'PUT',
          body
        }
      );
    },

    patch(path, body) {
      if (
        typeof LoadedApi.patch ===
        'function'
      ) {
        return LoadedApi.patch(
          path,
          body
        );
      }

      return LoadedApi.request(
        path,
        {
          method: 'PATCH',
          body
        }
      );
    },

    delete(path) {
      if (
        typeof LoadedApi.delete ===
        'function'
      ) {
        return LoadedApi.delete(path);
      }

      return LoadedApi.request(
        path,
        {
          method: 'DELETE'
        }
      );
    }
  };

  const elements = {
    sidebar:
      document.getElementById(
        'sidebar'
      ),

    mobileOverlay:
      document.getElementById(
        'mobileOverlay'
      ),

    menuButton:
      document.getElementById(
        'menuButton'
      ),

    logoutButton:
      document.getElementById(
        'logoutButton'
      ),

    pageTitle:
      document.getElementById(
        'pageTitle'
      ),

    adminName:
      document.getElementById(
        'adminName'
      ),

    adminInitial:
      document.getElementById(
        'adminInitial'
      ),

    globalAlert:
      document.getElementById(
        'globalAlert'
      ),

    modal:
      document.getElementById(
        'modal'
      ),

    modalClose:
      document.getElementById(
        'modalClose'
      ),

    modalContent:
      document.getElementById(
        'modalContent'
      )
  };

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(
        /[&<>'"]/g,
        character => ({
          '&': '&amp;',
          '<': '&lt;',
          '>': '&gt;',
          "'": '&#39;',
          '"': '&quot;'
        })[character]
      );
  }

  function unwrap(payload) {
    if (
      payload &&
      Object.prototype
        .hasOwnProperty.call(
          payload,
          'data'
        )
    ) {
      return payload.data;
    }

    return payload;
  }

  function arrayFrom(payload) {
    const value =
      unwrap(payload);

    if (Array.isArray(value)) {
      return value;
    }

    if (
      Array.isArray(
        value?.content
      )
    ) {
      return value.content;
    }

    if (
      Array.isArray(
        value?.items
      )
    ) {
      return value.items;
    }

    if (
      Array.isArray(
        value?.users
      )
    ) {
      return value.users;
    }

    if (
      Array.isArray(
        value?.orders
      )
    ) {
      return value.orders;
    }

    if (
      Array.isArray(
        value?.payments
      )
    ) {
      return value.payments;
    }

    if (
      Array.isArray(
        value?.applications
      )
    ) {
      return value.applications;
    }

    return [];
  }

  function formatDate(value) {
    if (!value) {
      return '—';
    }

    const parsedDate =
      new Date(value);

    if (
      Number.isNaN(
        parsedDate.getTime()
      )
    ) {
      return escapeHtml(value);
    }

    return parsedDate
      .toLocaleString();
  }

  function formatMoney(value) {
    return `₦${Number(value || 0)
      .toLocaleString(
        'en-NG',
        {
          maximumFractionDigits: 2
        }
      )}`;
  }

  function statusClass(status) {
    return String(status || '')
      .toLowerCase()
      .replaceAll('_', '-');
  }

  function badge(status) {
    const value =
      String(
        status || 'UNKNOWN'
      );

    return `
      <span
        class="status-badge status-${statusClass(value)}"
      >
        ${escapeHtml(value)}
      </span>
    `;
  }

  function loadingState() {
    return `
      <div class="loading-block">
        Loading…
      </div>
    `;
  }

  function emptyState(message) {
    return `
      <div class="empty-state">
        ${escapeHtml(message)}
      </div>
    `;
  }

  function showAlert(
    message,
    type = 'success'
  ) {
    if (!elements.globalAlert) {
      return;
    }

    elements.globalAlert.textContent =
      message;

    elements.globalAlert.className =
      `alert ${type}`;

    elements.globalAlert.hidden =
      false;

    window.setTimeout(
      () => {
        elements.globalAlert.hidden =
          true;
      },
      4500
    );
  }

  function showError(error) {
    console.error(error);

    showAlert(
      error?.message ||
      'Something went wrong.',
      'error'
    );
  }

  function openModal(content) {
    if (
      !elements.modal ||
      !elements.modalContent
    ) {
      return;
    }

    elements.modalContent.innerHTML =
      content;

    elements.modal.hidden =
      false;

    elements.modal.setAttribute(
      'aria-hidden',
      'false'
    );

    document.body.classList.add(
      'modal-open'
    );
  }

  function closeModal() {
    if (!elements.modal) {
      return;
    }

    elements.modal.hidden =
      true;

    elements.modal.setAttribute(
      'aria-hidden',
      'true'
    );

    if (
      elements.modalContent
    ) {
      elements.modalContent
        .innerHTML = '';
    }

    document.body.classList.remove(
      'modal-open'
    );
  }

  function openMobileMenu() {
    elements.sidebar
      ?.classList.add('open');

    if (
      elements.mobileOverlay
    ) {
      elements.mobileOverlay.hidden =
        false;
    }
  }

  function closeMobileMenu() {
    elements.sidebar
      ?.classList.remove('open');

    if (
      elements.mobileOverlay
    ) {
      elements.mobileOverlay.hidden =
        true;
    }
  }

  function currentAdmin() {
    try {
      return JSON.parse(
        localStorage.getItem(
          'oyuki_user'
        ) || 'null'
      );
    } catch {
      return null;
    }
  }

  function requireAdmin() {
    const token =
      localStorage.getItem(
        'oyuki_token'
      );

    const user =
      currentAdmin();

    if (
      !token ||
      !user ||
      String(
        user.role || ''
      ).toUpperCase() !==
        'ADMIN'
    ) {
      window.location.replace(
        'admin-login.html'
      );

      return null;
    }

    return user;
  }

  function initialiseAdminName(
    user
  ) {
    const name =
      user?.fullName ||
      'Oyuki Administrator';

    if (
      elements.adminName
    ) {
      elements.adminName.textContent =
        name;
    }

    if (
      elements.adminInitial
    ) {
      elements.adminInitial.textContent =
        name
          .charAt(0)
          .toUpperCase();
    }
  }

  function showSection(
    sectionName
  ) {
    document
      .querySelectorAll(
        '.page-section'
      )
      .forEach(section => {
        section.classList.toggle(
          'active',
          section.id ===
            sectionName
        );
      });

    document
      .querySelectorAll(
        '.nav-item[data-section]'
      )
      .forEach(button => {
        button.classList.toggle(
          'active',
          button.dataset.section ===
            sectionName
        );
      });

    const titles = {
      overview: 'Overview',
      applications:
        'Applications',
      users: 'Users',
      orders: 'Orders',
      payments: 'Payments'
    };

    if (
      elements.pageTitle
    ) {
      elements.pageTitle.textContent =
        titles[sectionName] ||
        'Overview';
    }

    closeMobileMenu();

    if (
      sectionName ===
      'applications'
    ) {
      loadApplications();
    }

    if (
      sectionName ===
      'users'
    ) {
      loadUsers();
    }

    if (
      sectionName ===
      'orders'
    ) {
      loadOrders();
    }

    if (
      sectionName ===
      'payments'
    ) {
      loadPayments();
    }
  }

  async function loadStatistics() {
    const totalElement =
      document.getElementById(
        'totalUsers'
      );

    try {
      const payload =
        unwrap(
          await Api.get(
            '/admin/users/statistics'
          )
        );

      const total =
        Number(
          payload?.totalUsers ??
          payload?.total ??
          payload?.users ??
          0
        );

      if (totalElement) {
        totalElement.textContent =
          total.toLocaleString();
      }

      return total;
    } catch (error) {
      if (totalElement) {
        totalElement.textContent =
          '0';
      }

      console.warn(
        'Unable to load user statistics:',
        error
      );

      return 0;
    }
  }

  async function loadApplications() {
    const table =
      document.getElementById(
        'applicationsTable'
      );

    const overview =
      document.getElementById(
        'overviewApplications'
      );

    if (table) {
      table.innerHTML =
        loadingState();
    }

    if (overview) {
      overview.innerHTML =
        loadingState();
    }

    try {
      const applications =
        arrayFrom(
          await Api.get(
            '/admin/applications/pending'
          )
        );

      const count =
        applications.length;

      const pendingElement =
        document.getElementById(
          'pendingApplications'
        );

      const badgeElement =
        document.getElementById(
          'applicationBadge'
        );

      if (pendingElement) {
        pendingElement.textContent =
          count.toLocaleString();
      }

      if (badgeElement) {
        badgeElement.textContent =
          count;
      }

      if (overview) {
        overview.innerHTML =
          count
            ? applications
                .slice(0, 4)
                .map(
                  application => `
                    <div class="list-item">
                      <div>
                        <h3>
                          ${escapeHtml(
                            application.fullName ||
                            application.businessName ||
                            application.ownerName ||
                            'Provider'
                          )}
                        </h3>

                        <p>
                          ${escapeHtml(
                            application.role ||
                            application.providerType ||
                            'PROVIDER'
                          )}
                        </p>
                      </div>

                      <button
                        class="secondary-button small"
                        type="button"
                        data-view-application="${
                          application.userId ||
                          application.id
                        }"
                      >
                        Review
                      </button>
                    </div>
                  `
                )
                .join('')
            : emptyState(
                'No pending applications.'
              );
      }

      if (!table) {
        return applications;
      }

      if (!count) {
        table.innerHTML =
          emptyState(
            'No pending applications.'
          );

        return applications;
      }

      table.innerHTML = `
        <table class="admin-table">
          <thead>
            <tr>
              <th>Applicant</th>
              <th>Role</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>

          <tbody>
            ${applications
              .map(
                application => `
                  <tr>
                    <td>
                      ${escapeHtml(
                        application.fullName ||
                        application.businessName ||
                        'Provider'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        application.role ||
                        application.providerType ||
                        'PROVIDER'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        application.email ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        application.phoneNumber ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${badge(
                        application.status ||
                        'PENDING_APPROVAL'
                      )}
                    </td>

                    <td>
                      <button
                        class="secondary-button small"
                        type="button"
                        data-view-application="${
                          application.userId ||
                          application.id
                        }"
                      >
                        Review
                      </button>
                    </td>
                  </tr>
                `
              )
              .join('')}
          </tbody>
        </table>
      `;

      return applications;
    } catch (error) {
      if (table) {
        table.innerHTML =
          emptyState(
            error.message
          );
      }

      if (overview) {
        overview.innerHTML =
          emptyState(
            'Unable to load applications.'
          );
      }

      const pendingElement =
        document.getElementById(
          'pendingApplications'
        );

      if (pendingElement) {
        pendingElement.textContent =
          '0';
      }

      showError(error);

      return [];
    }
  }

  async function openApplication(userId) {
    try {
      openModal(`<h2 id="modalTitle">Loading application…</h2>${loadingState()}`);
      const a = unwrap(await Api.get(`/admin/applications/${userId}`));
      const mediaUrl=value=>{if(!value)return null;if(/^https?:\/\//i.test(value))return value;if(value.startsWith('/uploads/'))return `https://illustrious-nurturing-production-8169.up.railway.app${value}`;return value};
      const picture=(label,value)=>value?`<div class="application-media"><span>${label}</span><a href="${escapeHtml(mediaUrl(value))}" target="_blank" rel="noopener"><img src="${escapeHtml(mediaUrl(value))}" alt="${label}"></a></div>`:'';
      const gallery=Array.isArray(a.kitchenImages)?a.kitchenImages:[];
      const details=[['Full name',a.fullName],['Provider type',a.role],['Business/Kitchen name',a.businessName],['Cuisine',a.cuisine],['Email',a.email],['Phone',a.phoneNumber],['Account status',a.accountStatus],['State',a.state],['LGA',a.lga],['Area',a.area],['Full address',a.addressLine],['Latitude',a.latitude],['Longitude',a.longitude],['Facial verification',a.facialVerificationStatus],['Bank',a.bankName],['Account name',a.accountName],['Account number',a.accountNumber],['Registered',formatDate(a.registeredAt)],['Profile submitted',formatDate(a.profileSubmittedAt)],['Profile complete',a.profileCompleted?'Yes':'No']];
      openModal(`
        <h2 id="modalTitle">Provider application</h2>
        <div class="modal-detail-grid">${details.map(([k,v])=>`<div class="modal-detail"><span>${escapeHtml(k)}</span><strong>${escapeHtml(v??'—')}</strong></div>`).join('')}</div>
        <div class="application-bio"><span>About the business</span><p>${escapeHtml(a.bio||'No bio provided.')}</p></div>
        <div class="application-media-grid">${picture('Profile picture',a.profileImageUrl)}${picture('Cover picture',a.coverImageUrl)}</div>
        ${gallery.length?`<h3 class="application-heading">Kitchen pictures</h3><div class="application-gallery">${gallery.map(img=>`<a href="${escapeHtml(mediaUrl(img.imageUrl))}" target="_blank" rel="noopener"><img src="${escapeHtml(mediaUrl(img.imageUrl))}" alt="${escapeHtml(img.caption||'Kitchen picture')}"><small>${escapeHtml(img.caption||'Kitchen picture')}</small></a>`).join('')}</div>`:''}
        <h3 class="application-heading">Downloads</h3>
        <div class="table-actions">
          <button class="secondary-button" data-download-application="${userId}"><i class="bi bi-download"></i> Download full application</button>
          ${a.idDocumentUrl?`<button class="secondary-button" data-download-id="${userId}"><i class="bi bi-file-earmark-arrow-down"></i> Download ID</button>`:''}
        </div>
        <div class="modal-actions"><button class="danger-button" data-reject-application="${userId}">Reject</button><button class="success-button" data-approve-application="${userId}">Approve</button></div>`);
    } catch (error) { closeModal(); showError(error); }
  }

  async function approveApplication(
    userId
  ) {
    const confirmed =
      window.confirm(
        'Approve this provider application?'
      );

    if (!confirmed) {
      return;
    }

    try {
      await Api.patch(
        `/admin/applications/${userId}/approve`,
        {}
      );

      closeModal();

      showAlert(
        'Application approved successfully.'
      );

      await loadApplications();
      await loadStatistics();
    } catch (error) {
      showError(error);
    }
  }

  function showRejectApplication(
    userId
  ) {
    openModal(`
      <h2 id="modalTitle">
        Reject application
      </h2>

      <p>
        Enter the reason for rejecting this provider.
      </p>

      <textarea
        id="applicationRejectReason"
        placeholder="Rejection reason"
      ></textarea>

      <div class="modal-actions">

        <button
          class="secondary-button"
          type="button"
          data-close-modal
        >
          Cancel
        </button>

        <button
          class="danger-button"
          type="button"
          data-confirm-reject-application="${userId}"
        >
          Reject application
        </button>

      </div>
    `);
  }

  async function rejectApplication(
    userId
  ) {
    const reason =
      document
        .getElementById(
          'applicationRejectReason'
        )
        ?.value
        .trim();

    if (!reason) {
      showAlert(
        'Enter a rejection reason.',
        'error'
      );

      return;
    }

    try {
      await Api.patch(
        `/admin/applications/${userId}/reject`,
        { reason }
      );

      closeModal();

      showAlert(
        'Application rejected.'
      );

      await loadApplications();
      await loadStatistics();
    } catch (error) {
      showError(error);
    }
  }

  async function loadUsers() {
    const table =
      document.getElementById(
        'usersTable'
      );

    if (!table) {
      return [];
    }

    table.innerHTML =
      loadingState();

    const search =
      document
        .getElementById(
          'userSearch'
        )
        ?.value
        .trim() || '';

    const role =
      document
        .getElementById(
          'userRole'
        )
        ?.value || '';

    const status =
      document
        .getElementById(
          'userStatus'
        )
        ?.value || '';

    const params =
      new URLSearchParams();

    if (search) {
      params.set(
        'search',
        search
      );
    }

    if (role) {
      params.set(
        'role',
        role
      );
    }

    if (status) {
      params.set(
        'status',
        status
      );
    }

    try {
      const suffix =
        params.toString()
          ? `?${params.toString()}`
          : '';

      const users =
        arrayFrom(
          await Api.get(
            `/admin/users${suffix}`
          )
        );

      if (!users.length) {
        table.innerHTML =
          emptyState(
            'No users found.'
          );

        return users;
      }

      table.innerHTML = `
        <table class="admin-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Contact</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined</th>
              <th>Action</th>
            </tr>
          </thead>

          <tbody>
            ${users
              .map(
                user => `
                  <tr>
                    <td>
                      ${escapeHtml(
                        user.fullName ||
                        'User'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        user.email ||
                        user.phoneNumber ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        user.role ||
                        'CUSTOMER'
                      )}
                    </td>

                    <td>
                      ${badge(
                        user.status ||
                        user.accountStatus ||
                        'UNKNOWN'
                      )}
                    </td>

                    <td>
                      ${formatDate(
                        user.createdAt
                      )}
                    </td>

                    <td>
                      <button
                        class="secondary-button small"
                        type="button"
                        data-manage-user="${
                          user.id ||
                          user.userId
                        }"
                        data-user-name="${escapeHtml(
                          user.fullName ||
                          'User'
                        )}"
                        data-user-status="${escapeHtml(
                          user.status ||
                          user.accountStatus ||
                          ''
                        )}"
                      >
                        Manage
                      </button>
                    </td>
                  </tr>
                `
              )
              .join('')}
          </tbody>
        </table>
      `;

      return users;
    } catch (error) {
      table.innerHTML =
        emptyState(
          error.message
        );

      showError(error);

      return [];
    }
  }

  function openUserManager(
    button
  ) {
    const userId =
      button.dataset.manageUser;

    const userName =
      button.dataset.userName ||
      'User';

    const currentStatus =
      button.dataset.userStatus ||
      '';

    openModal(`
      <h2 id="modalTitle">
        Manage ${escapeHtml(userName)}
      </h2>

      <label for="newUserStatus">
        Account status
      </label>

      <select id="newUserStatus">

        <option
          value="ACTIVE"
          ${
            currentStatus ===
            'ACTIVE'
              ? 'selected'
              : ''
          }
        >
          ACTIVE
        </option>

        <option
          value="SUSPENDED"
          ${
            currentStatus ===
            'SUSPENDED'
              ? 'selected'
              : ''
          }
        >
          SUSPENDED
        </option>

        <option
          value="DISABLED"
          ${
            currentStatus ===
            'DISABLED'
              ? 'selected'
              : ''
          }
        >
          DISABLED
        </option>

        <option
          value="REJECTED"
          ${
            currentStatus ===
            'REJECTED'
              ? 'selected'
              : ''
          }
        >
          REJECTED
        </option>

        <option
          value="PENDING_APPROVAL"
          ${
            currentStatus ===
            'PENDING_APPROVAL'
              ? 'selected'
              : ''
          }
        >
          PENDING_APPROVAL
        </option>

      </select>

      <label
        for="userStatusReason"
        style="display:block;margin-top:14px"
      >
        Reason
      </label>

      <textarea
        id="userStatusReason"
        placeholder="Optional reason"
      ></textarea>

      <div class="modal-actions">

        <button
          class="secondary-button"
          type="button"
          data-close-modal
        >
          Cancel
        </button>

        <button
          class="primary-button"
          type="button"
          data-save-user-status="${userId}"
        >
          Save status
        </button>

      </div>
    `);
  }

  async function saveUserStatus(
    userId
  ) {
    const status =
      document
        .getElementById(
          'newUserStatus'
        )
        ?.value;

    const reason =
      document
        .getElementById(
          'userStatusReason'
        )
        ?.value
        .trim() ||
      null;

    try {
      await Api.patch(
        `/admin/users/${userId}/status`,
        {
          status,
          reason
        }
      );

      closeModal();

      showAlert(
        'User status updated.'
      );

      await loadUsers();
      await loadStatistics();
    } catch (error) {
      showError(error);
    }
  }

  async function loadOrders() {
    const table =
      document.getElementById(
        'ordersTable'
      );

    const overview =
      document.getElementById(
        'overviewOrders'
      );

    if (table) {
      table.innerHTML =
        loadingState();
    }

    if (overview) {
      overview.innerHTML =
        loadingState();
    }

    try {
      const orders =
        arrayFrom(
          await Api.get(
            '/admin/orders'
          )
        );

      const totalElement =
        document.getElementById(
          'totalOrders'
        );

      if (totalElement) {
        totalElement.textContent =
          orders.length
            .toLocaleString();
      }

      if (overview) {
        overview.innerHTML =
          orders.length
            ? orders
                .slice(0, 5)
                .map(
                  order => `
                    <div class="list-item">
                      <div>
                        <h3>
                          Order #${escapeHtml(
                            order.orderNumber ||
                            order.id
                          )}
                        </h3>

                        <p>
                          ${escapeHtml(
                            order.customerName ||
                            order.customer
                              ?.fullName ||
                            'Customer'
                          )}

                          ·

                          ${formatMoney(
                            order.totalAmount ||
                            order.total
                          )}
                        </p>
                      </div>

                      ${badge(
                        order.status ||
                        'PENDING'
                      )}
                    </div>
                  `
                )
                .join('')
            : emptyState(
                'No orders found.'
              );
      }

      if (!table) {
        return orders;
      }

      if (!orders.length) {
        table.innerHTML =
          emptyState(
            'No orders found.'
          );

        return orders;
      }

      table.innerHTML = `
        <table class="admin-table">
          <thead>
            <tr>
              <th>Order</th>
              <th>Customer</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>

          <tbody>
            ${orders
              .map(
                order => `
                  <tr>
                    <td>
                      #${escapeHtml(
                        order.orderNumber ||
                        order.id
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        order.customerName ||
                        order.customer
                          ?.fullName ||
                        'Customer'
                      )}
                    </td>

                    <td>
                      ${formatMoney(
                        order.totalAmount ||
                        order.total
                      )}
                    </td>

                    <td>
                      ${badge(
                        order.status ||
                        'PENDING'
                      )}
                    </td>

                    <td>
                      ${formatDate(
                        order.createdAt
                      )}
                    </td>
                  </tr>
                `
              )
              .join('')}
          </tbody>
        </table>
      `;

      return orders;
    } catch (error) {
      if (table) {
        table.innerHTML =
          emptyState(
            error.message
          );
      }

      if (overview) {
        overview.innerHTML =
          emptyState(
            'Unable to load orders.'
          );
      }

      const totalElement =
        document.getElementById(
          'totalOrders'
        );

      if (totalElement) {
        totalElement.textContent =
          '0';
      }

      showError(error);

      return [];
    }
  }

  async function loadPayments() {
    const table =
      document.getElementById(
        'paymentsTable'
      );

    if (!table) {
      return [];
    }

    table.innerHTML =
      loadingState();

    const selectedStatus =
      document
        .getElementById(
          'paymentStatus'
        )
        ?.value || '';

    try {
      const suffix =
        selectedStatus
          ? `?status=${encodeURIComponent(
              selectedStatus
            )}`
          : '';

      const payments =
        arrayFrom(
          await Api.get(
            `/admin/payments${suffix}`
          )
        );

      const pending =
        payments.filter(
          payment =>
            String(
              payment.status || ''
            ).toUpperCase() ===
            'PENDING'
        ).length;

      const pendingElement =
        document.getElementById(
          'pendingPayments'
        );

      if (pendingElement) {
        pendingElement.textContent =
          pending.toLocaleString();
      }

      if (!payments.length) {
        table.innerHTML =
          emptyState(
            'No payment proofs found.'
          );

        return payments;
      }

      table.innerHTML = `
        <table class="admin-table">
          <thead>
            <tr>
              <th>Payment</th>
              <th>Order</th>
              <th>Customer</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>

          <tbody>
            ${payments
              .map(
                payment => `
                  <tr>
                    <td>
                      #${escapeHtml(
                        payment.id
                      )}
                    </td>

                    <td>
                      #${escapeHtml(
                        payment.orderId ||
                        payment.order?.id ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        payment.customerName ||
                        payment.user
                          ?.fullName ||
                        'Customer'
                      )}
                    </td>

                    <td>
                      ${formatMoney(
                        payment.amount
                      )}
                    </td>

                    <td>
                      ${badge(
                        payment.status ||
                        'PENDING'
                      )}
                    </td>

                    <td>
                      <div class="table-actions">

                        <button
                          class="success-button small"
                          type="button"
                          data-confirm-payment="${payment.id}"
                        >
                          Confirm
                        </button>

                        <button
                          class="danger-button small"
                          type="button"
                          data-reject-payment="${payment.id}"
                        >
                          Reject
                        </button>

                      </div>
                    </td>
                  </tr>
                `
              )
              .join('')}
          </tbody>
        </table>
      `;

      return payments;
    } catch (error) {
      table.innerHTML =
        emptyState(
          error.message
        );

      const pendingElement =
        document.getElementById(
          'pendingPayments'
        );

      if (pendingElement) {
        pendingElement.textContent =
          '0';
      }

      showError(error);

      return [];
    }
  }

  async function confirmPayment(
    paymentId
  ) {
    const confirmed =
      window.confirm(
        'Confirm this payment?'
      );

    if (!confirmed) {
      return;
    }

    try {
      await Api.patch(
        `/admin/payments/${paymentId}/confirm`,
        {
          note:
            'Confirmed by administrator'
        }
      );

      showAlert(
        'Payment confirmed.'
      );

      await loadPayments();
      await loadOrders();
    } catch (error) {
      showError(error);
    }
  }

  function showRejectPayment(
    paymentId
  ) {
    openModal(`
      <h2 id="modalTitle">
        Reject payment
      </h2>

      <textarea
        id="paymentRejectReason"
        placeholder="Enter rejection reason"
      ></textarea>

      <div class="modal-actions">

        <button
          class="secondary-button"
          type="button"
          data-close-modal
        >
          Cancel
        </button>

        <button
          class="danger-button"
          type="button"
          data-confirm-reject-payment="${paymentId}"
        >
          Reject payment
        </button>

      </div>
    `);
  }

  async function rejectPayment(
    paymentId
  ) {
    const reason =
      document
        .getElementById(
          'paymentRejectReason'
        )
        ?.value
        .trim();

    if (!reason) {
      showAlert(
        'Enter a rejection reason.',
        'error'
      );

      return;
    }

    try {
      await Api.patch(
        `/admin/payments/${paymentId}/reject`,
        { reason }
      );

      closeModal();

      showAlert(
        'Payment rejected.'
      );

      await loadPayments();
    } catch (error) {
      showError(error);
    }
  }

  async function loadOverview() {
    await Promise.allSettled([
      loadStatistics(),
      loadApplications(),
      loadOrders(),
      loadPayments()
    ]);
  }

  function bindEvents() {
    elements.menuButton
      ?.addEventListener(
        'click',
        openMobileMenu
      );

    elements.mobileOverlay
      ?.addEventListener(
        'click',
        closeMobileMenu
      );

    elements.logoutButton
      ?.addEventListener(
        'click',
        () => {
          localStorage.removeItem(
            'oyuki_token'
          );

          localStorage.removeItem(
            'oyuki_user'
          );

          window.location.replace(
            'admin-login.html'
          );
        }
      );

    elements.modalClose
      ?.addEventListener(
        'click',
        closeModal
      );

    elements.modal
      ?.querySelectorAll(
        '[data-modal-close]'
      )
      .forEach(element => {
        element.addEventListener(
          'click',
          closeModal
        );
      });

    document.addEventListener(
      'keydown',
      event => {
        if (
          event.key ===
          'Escape'
        ) {
          closeModal();
        }
      }
    );

    document.addEventListener(
      'click',
      async event => {
        const navButton =
          event.target.closest(
            '[data-section]'
          );

        if (navButton) {
          showSection(
            navButton.dataset.section
          );

          return;
        }

        const goButton =
          event.target.closest(
            '[data-go]'
          );

        if (goButton) {
          showSection(
            goButton.dataset.go
          );

          return;
        }

        const viewApplication =
          event.target.closest(
            '[data-view-application]'
          );

        if (viewApplication) {
          openApplication(
            viewApplication
              .dataset
              .viewApplication
          );

          return;
        }

        const approveButton =
          event.target.closest(
            '[data-approve-application]'
          );

        if (approveButton) {
          approveApplication(
            approveButton
              .dataset
              .approveApplication
          );

          return;
        }

        const rejectButton =
          event.target.closest(
            '[data-reject-application]'
          );

        if (rejectButton) {
          showRejectApplication(
            rejectButton
              .dataset
              .rejectApplication
          );

          return;
        }

        const confirmRejectApplication =
          event.target.closest(
            '[data-confirm-reject-application]'
          );

        if (
          confirmRejectApplication
        ) {
          rejectApplication(
            confirmRejectApplication
              .dataset
              .confirmRejectApplication
          );

          return;
        }

        const manageUser =
          event.target.closest(
            '[data-manage-user]'
          );

        if (manageUser) {
          openUserManager(
            manageUser
          );

          return;
        }

        const saveUser =
          event.target.closest(
            '[data-save-user-status]'
          );

        if (saveUser) {
          saveUserStatus(
            saveUser
              .dataset
              .saveUserStatus
          );

          return;
        }

        const confirmPaymentButton =
          event.target.closest(
            '[data-confirm-payment]'
          );

        if (
          confirmPaymentButton
        ) {
          confirmPayment(
            confirmPaymentButton
              .dataset
              .confirmPayment
          );

          return;
        }

        const rejectPaymentButton =
          event.target.closest(
            '[data-reject-payment]'
          );

        if (rejectPaymentButton) {
          showRejectPayment(
            rejectPaymentButton
              .dataset
              .rejectPayment
          );

          return;
        }

        const confirmRejectPayment =
          event.target.closest(
            '[data-confirm-reject-payment]'
          );

        if (
          confirmRejectPayment
        ) {
          rejectPayment(
            confirmRejectPayment
              .dataset
              .confirmRejectPayment
          );

          return;
        }

        const appDownload = event.target.closest('[data-download-application]');
        if (appDownload) { try { await LoadedApi.downloadFile(`/admin/applications/${appDownload.dataset.downloadApplication}/download`, `oyuki-application-${appDownload.dataset.downloadApplication}.json`); } catch (error) { showError(error); } return; }

        const idDownload = event.target.closest('[data-download-id]');
        if (idDownload) { try { await LoadedApi.downloadFile(`/admin/applications/${idDownload.dataset.downloadId}/documents/id`, `provider-id-${idDownload.dataset.downloadId}`); } catch (error) { showError(error); } return; }

        if (event.target.closest('[data-close-modal]')) { closeModal(); }
      }
    );

    document
      .getElementById(
        'refreshApplications'
      )
      ?.addEventListener(
        'click',
        loadApplications
      );

    document
      .getElementById(
        'searchUsers'
      )
      ?.addEventListener(
        'click',
        loadUsers
      );

    document
      .getElementById(
        'refreshOrders'
      )
      ?.addEventListener(
        'click',
        loadOrders
      );

    document
      .getElementById(
        'refreshPayments'
      )
      ?.addEventListener(
        'click',
        loadPayments
      );
  }

  async function initialise() {
    const user =
      requireAdmin();

    if (!user) {
      return;
    }

    closeModal();
    closeMobileMenu();

    initialiseAdminName(
      user
    );

    bindEvents();

    await loadOverview();
  }

  if (
    document.readyState ===
    'loading'
  ) {
    document.addEventListener(
      'DOMContentLoaded',
      initialise
    );
  } else {
    initialise();
  }
})();