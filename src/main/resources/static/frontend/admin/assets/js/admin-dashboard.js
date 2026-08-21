(function () {
  'use strict';

  /* =========================================================
     API
  ========================================================= */

  const LoadedApi =
    window.OyukiAdminApi ||
    window.AdminApi;

  if (!LoadedApi) {
    console.error(
      'admin-api.js did not load.'
    );

    document
      .querySelectorAll('.loading-block')
      .forEach(element => {
        element.textContent =
          'Admin API JavaScript did not load.';
      });

    return;
  }

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

  /* =========================================================
     ELEMENTS
  ========================================================= */

  const elements = {

    sidebar:
      document.getElementById(
        'sidebar'
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

  /* =========================================================
     HELPERS
  ========================================================= */

  function escapeHtml(value) {

    return String(
      value ?? ''
    ).replace(
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
      Object.prototype.hasOwnProperty.call(
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

    if (
      Array.isArray(value)
    ) {
      return value;
    }

    const possibleArrays = [
      'content',
      'items',
      'users',
      'orders',
      'payments',
      'applications',
      'marketers',
      'agents',
      'markets'
    ];

    for (
      const key of possibleArrays
    ) {

      if (
        Array.isArray(
          value?.[key]
        )
      ) {
        return value[key];
      }
    }

    return [];
  }

  function formatDate(value) {

    if (!value) {
      return '—';
    }

    const date =
      new Date(value);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return escapeHtml(value);
    }

    return date
      .toLocaleString(
        'en-NG'
      );
  }

  function formatMoney(value) {

    return new Intl.NumberFormat(
      'en-NG',
      {
        style: 'currency',
        currency: 'NGN',
        maximumFractionDigits: 2
      }
    ).format(
      Number(value || 0)
    );
  }

  function statusClass(status) {

    return String(
      status || ''
    )
      .toLowerCase()
      .replaceAll(
        '_',
        '-'
      );
  }

  function badge(status) {

    const value =
      String(
        status ||
        'UNKNOWN'
      );

    return `
      <span
        class="status-badge status-${statusClass(
          value
        )}"
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

    if (
      !elements.globalAlert
    ) {
      return;
    }

    elements.globalAlert
      .textContent =
      message;

    elements.globalAlert
      .className =
      `alert ${type}`;

    elements.globalAlert
      .hidden =
      false;

    setTimeout(
      () => {
        elements.globalAlert
          .hidden =
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

  /* =========================================================
     MODAL
  ========================================================= */

  function openModal(content) {

    if (
      !elements.modal ||
      !elements.modalContent
    ) {
      return;
    }

    elements.modalContent
      .innerHTML =
      content;

    elements.modal.hidden =
      false;

    elements.modal.setAttribute(
      'aria-hidden',
      'false'
    );

    document.body
      .classList.add(
        'modal-open'
      );
  }

  function closeModal() {

    if (
      !elements.modal
    ) {
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
        .innerHTML =
        '';
    }

    document.body
      .classList.remove(
        'modal-open'
      );
  }

  /* =========================================================
     AUTH
  ========================================================= */

  function currentAdmin() {

    try {

      return JSON.parse(
        localStorage.getItem(
          'oyuki_user'
        ) ||
        'null'
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

      elements.adminName
        .textContent =
        name;
    }

    if (
      elements.adminInitial
    ) {

      elements.adminInitial
        .textContent =
        name
          .charAt(0)
          .toUpperCase();
    }
  }

  /* =========================================================
     NAVIGATION
  ========================================================= */

  function showSection(
    sectionName
  ) {

    document
      .querySelectorAll(
        '.page-section'
      )
      .forEach(
        section => {

          section.classList
            .toggle(
              'active',
              section.id ===
                sectionName
            );
        }
      );

    document
      .querySelectorAll(
        '.nav-item[data-section]'
      )
      .forEach(
        button => {

          button.classList
            .toggle(
              'active',
              button.dataset
                .section ===
                sectionName
            );
        }
      );

    const titles = {

      overview:
        'Overview',

      applications:
        'Applications',

      users:
        'Users',

      marketers:
        'Marketers',

      marketAgents:
        'Market Agents',

      markets:
        'Markets',

      orders:
        'Orders',

      payments:
        'Payments'
    };

    if (
      elements.pageTitle
    ) {

      elements.pageTitle
        .textContent =
        titles[
          sectionName
        ] ||
        'Overview';
    }

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
      'marketers'
    ) {
      loadMarketers();
    }

    if (
      sectionName ===
      'marketAgents'
    ) {
      loadMarketAgents();
    }

    if (
      sectionName ===
      'markets'
    ) {
      loadMarkets();
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

    if (
      window.innerWidth <
      900
    ) {

      elements.sidebar
        ?.classList.remove(
          'open'
        );
    }
  }

  /* =========================================================
     STATISTICS
  ========================================================= */

  async function loadStatistics() {

    const totalElement =
      document.getElementById(
        'totalUsers'
      );

    try {

      const data =
        unwrap(
          await Api.get(
            '/admin/users/statistics'
          )
        );

      const total =
        Number(
          data?.totalUsers ??
          data?.total ??
          data?.users ??
          0
        );

      if (
        totalElement
      ) {

        totalElement.textContent =
          total.toLocaleString();
      }

      return total;

    } catch (
      error
    ) {

      if (
        totalElement
      ) {

        totalElement.textContent =
          '0';
      }

      console.warn(
        'Unable to load statistics',
        error
      );

      return 0;
    }
  }

  /* =========================================================
     APPLICATIONS
  ========================================================= */

  async function loadApplications() {

    const table =
      document.getElementById(
        'applicationsTable'
      );

    const overview =
      document.getElementById(
        'overviewApplications'
      );

    if (
      table
    ) {
      table.innerHTML =
        loadingState();
    }

    if (
      overview
    ) {
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

      const pending =
        document.getElementById(
          'pendingApplications'
        );

      const badgeElement =
        document.getElementById(
          'applicationBadge'
        );

      if (
        pending
      ) {
        pending.textContent =
          count;
      }

      if (
        badgeElement
      ) {
        badgeElement.textContent =
          count;
      }

      if (
        overview
      ) {

        overview.innerHTML =
          applications.length

            ? applications
                .slice(
                  0,
                  4
                )
                .map(
                  application => `
                    <div class="list-item">

                      <div>

                        <h3>
                          ${escapeHtml(
                            application.fullName ||
                            application.businessName ||
                            'Provider'
                          )}
                        </h3>

                        <p>
                          ${escapeHtml(
                            application.role ||
                            'PROVIDER'
                          )}
                        </p>

                      </div>

                      <button
                        class="secondary-button small"
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

      if (
        !table
      ) {
        return applications;
      }

      if (
        !applications.length
      ) {

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
                        application.accountStatus ||
                        application.status ||
                        'PENDING_APPROVAL'
                      )}
                    </td>

                    <td>

                      <button
                        class="secondary-button small"
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

    } catch (
      error
    ) {

      if (
        table
      ) {
        table.innerHTML =
          emptyState(
            error.message
          );
      }

      if (
        overview
      ) {
        overview.innerHTML =
          emptyState(
            'Unable to load applications.'
          );
      }

      showError(error);

      return [];
    }
  }

  async function openApplication(
    userId
  ) {

    try {

      openModal(
        `
          <h2 id="modalTitle">
            Loading application…
          </h2>

          ${loadingState()}
        `
      );

      const application =
        unwrap(
          await Api.get(
            `/admin/applications/${userId}`
          )
        );

      openModal(`
        <h2 id="modalTitle">
          Provider Application
        </h2>

        <div class="modal-detail-grid">

          <div class="modal-detail">
            <span>Full name</span>
            <strong>
              ${escapeHtml(
                application.fullName ||
                '—'
              )}
            </strong>
          </div>

          <div class="modal-detail">
            <span>Role</span>
            <strong>
              ${escapeHtml(
                application.role ||
                '—'
              )}
            </strong>
          </div>

          <div class="modal-detail">
            <span>Business</span>
            <strong>
              ${escapeHtml(
                application.businessName ||
                '—'
              )}
            </strong>
          </div>

          <div class="modal-detail">
            <span>Email</span>
            <strong>
              ${escapeHtml(
                application.email ||
                '—'
              )}
            </strong>
          </div>

          <div class="modal-detail">
            <span>Phone</span>
            <strong>
              ${escapeHtml(
                application.phoneNumber ||
                '—'
              )}
            </strong>
          </div>

          <div class="modal-detail">
            <span>Status</span>
            <strong>
              ${escapeHtml(
                application.accountStatus ||
                '—'
              )}
            </strong>
          </div>

        </div>

        <div class="application-bio">

          <span>
            About business
          </span>

          <p>
            ${escapeHtml(
              application.bio ||
              'No bio provided.'
            )}
          </p>

        </div>

        <div class="modal-actions">

          <button
            class="danger-button"
            data-reject-application="${userId}"
          >
            Reject
          </button>

          <button
            class="success-button"
            data-approve-application="${userId}"
          >
            Approve
          </button>

        </div>
      `);

    } catch (
      error
    ) {

      closeModal();

      showError(error);
    }
  }

  async function approveApplication(
    userId
  ) {

    if (
      !window.confirm(
        'Approve this application?'
      )
    ) {
      return;
    }

    try {

      await Api.patch(
        `/admin/applications/${userId}/approve`,
        {}
      );

      closeModal();

      showAlert(
        'Application approved.'
      );

      await loadApplications();

    } catch (
      error
    ) {

      showError(error);
    }
  }

  function showRejectApplication(
    userId
  ) {

    openModal(`
      <h2 id="modalTitle">
        Reject Application
      </h2>

      <label>
        Rejection reason
      </label>

      <textarea
        id="applicationRejectReason"
        placeholder="Enter reason"
      ></textarea>

      <div class="modal-actions">

        <button
          class="secondary-button"
          data-close-modal
        >
          Cancel
        </button>

        <button
          class="danger-button"
          data-confirm-reject-application="${userId}"
        >
          Reject
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

    if (
      !reason
    ) {

      showAlert(
        'Enter a rejection reason.',
        'error'
      );

      return;
    }

    try {

      await Api.patch(
        `/admin/applications/${userId}/reject`,
        {
          reason
        }
      );

      closeModal();

      showAlert(
        'Application rejected.'
      );

      await loadApplications();

    } catch (
      error
    ) {

      showError(error);
    }
  }

  /* =========================================================
     USERS
  ========================================================= */

  async function loadUsers() {

    const table =
      document.getElementById(
        'usersTable'
      );

    if (
      !table
    ) {
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
        .trim() ||
      '';

    const role =
      document
        .getElementById(
          'userRole'
        )
        ?.value ||
      '';

    const status =
      document
        .getElementById(
          'userStatus'
        )
        ?.value ||
      '';

    const params =
      new URLSearchParams();

    if (
      search
    ) {
      params.set(
        'search',
        search
      );
    }

    if (
      role
    ) {
      params.set(
        'role',
        role
      );
    }

    if (
      status
    ) {
      params.set(
        'status',
        status
      );
    }

    try {

      const suffix =
        params.toString()
          ? `?${params}`
          : '';

      const users =
        arrayFrom(
          await Api.get(
            `/admin/users${suffix}`
          )
        );

      if (
        !users.length
      ) {

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

    } catch (
      error
    ) {

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
      button.dataset
        .manageUser;

    const userName =
      button.dataset
        .userName ||
      'User';

    const currentStatus =
      button.dataset
        .userStatus ||
      '';

    openModal(`
      <h2 id="modalTitle">
        Manage ${escapeHtml(
          userName
        )}
      </h2>

      <label>
        Account status
      </label>

      <select id="newUserStatus">

        ${[
          'ACTIVE',
          'SUSPENDED',
          'DISABLED',
          'REJECTED',
          'PENDING_APPROVAL',
          'PENDING_VERIFICATION'
        ]
          .map(
            status => `
              <option
                value="${status}"
                ${
                  currentStatus ===
                  status
                    ? 'selected'
                    : ''
                }
              >
                ${status}
              </option>
            `
          )
          .join('')}

      </select>

      <label
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
          data-close-modal
        >
          Cancel
        </button>

        <button
          class="primary-button"
          data-save-user-status="${userId}"
        >
          Save
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

    } catch (
      error
    ) {

      showError(error);
    }
  }

  /* =========================================================
     MARKETERS
  ========================================================= */

  async function loadMarketers() {

    const table =
      document.getElementById(
        'marketersTable'
      );

    if (
      !table
    ) {
      return [];
    }

    table.innerHTML =
      loadingState();

    try {

      const marketers =
        arrayFrom(
          await Api.get(
            '/admin/marketers'
          )
        );

      if (
        !marketers.length
      ) {

        table.innerHTML =
          emptyState(
            'No marketers have been created yet.'
          );

        return [];
      }

      table.innerHTML = `
        <table class="admin-table">

          <thead>
            <tr>
              <th>Marketer</th>
              <th>Contact</th>
              <th>Referral code</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>

          <tbody>

            ${marketers
              .map(
                marketer => `
                  <tr>

                    <td>
                      <strong>
                        ${escapeHtml(
                          marketer.fullName ||
                          'Marketer'
                        )}
                      </strong>
                    </td>

                    <td>

                      ${escapeHtml(
                        marketer.email ||
                        '—'
                      )}

                      <br>

                      <small>
                        ${escapeHtml(
                          marketer.phoneNumber ||
                          ''
                        )}
                      </small>

                    </td>

                    <td>
                      <strong>
                        ${escapeHtml(
                          marketer.referralCode ||
                          '—'
                        )}
                      </strong>
                    </td>

                    <td>
                      ${badge(
                        marketer.status ||
                        marketer.accountStatus ||
                        'PENDING_VERIFICATION'
                      )}
                    </td>

                    <td>
                      ${formatDate(
                        marketer.createdAt ||
                        marketer.registeredAt
                      )}
                    </td>

                  </tr>
                `
              )
              .join('')}

          </tbody>

        </table>
      `;

      return marketers;

    } catch (
      error
    ) {

      table.innerHTML =
        emptyState(
          error.message ||
          'Unable to load marketers.'
        );

      showError(error);

      return [];
    }
  }

  function openCreateMarketer() {

    openModal(`
      <h2 id="modalTitle">
        Create Marketer
      </h2>

      <p>
        Marketer accounts are created by Oyuki Admin.
        An activation OTP will be sent automatically.
      </p>

      <form id="createMarketerForm">

        <label>
          Full name
        </label>

        <input
          name="fullName"
          type="text"
          required
          placeholder="Marketer full name"
        >

        <label>
          Email address
        </label>

        <input
          name="email"
          type="email"
          required
          placeholder="name@example.com"
        >

        <label>
          Phone number
        </label>

        <input
          name="phoneNumber"
          type="tel"
          required
          placeholder="+234..."
        >

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
            type="submit"
          >
            Create Marketer
          </button>

        </div>

      </form>
    `);

    document
      .getElementById(
        'createMarketerForm'
      )
      ?.addEventListener(
        'submit',
        createMarketer
      );
  }

  async function createMarketer(
    event
  ) {

    event.preventDefault();

    const form =
      event.currentTarget;

    const button =
      form.querySelector(
        'button[type="submit"]'
      );

    const payload =
      Object.fromEntries(
        new FormData(form)
          .entries()
      );

    try {

      if (
        button
      ) {

        button.disabled =
          true;

        button.textContent =
          'Creating…';
      }

      await Api.post(
        '/admin/marketers',
        payload
      );

      closeModal();

      showAlert(
        'Marketer created. OTP has been sent for account activation.'
      );

      await loadMarketers();

    } catch (
      error
    ) {

      showError(error);

      if (
        button
      ) {

        button.disabled =
          false;

        button.textContent =
          'Create Marketer';
      }
    }
  }

  /* =========================================================
     MARKETS
  ========================================================= */

  async function loadMarkets() {

    const table =
      document.getElementById(
        'marketsTable'
      );

    if (
      !table
    ) {
      return [];
    }

    table.innerHTML =
      loadingState();

    try {

      const markets =
        arrayFrom(
          await Api.get(
            '/admin/markets'
          )
        );

      if (
        !markets.length
      ) {

        table.innerHTML =
          emptyState(
            'No markets have been added yet.'
          );

        return [];
      }

      table.innerHTML = `
        <table class="admin-table">

          <thead>
            <tr>
              <th>Market</th>
              <th>State</th>
              <th>LGA</th>
              <th>Address</th>
              <th>Status</th>
            </tr>
          </thead>

          <tbody>

            ${markets
              .map(
                market => `
                  <tr>

                    <td>
                      <strong>
                        ${escapeHtml(
                          market.name ||
                          market.marketName ||
                          'Market'
                        )}
                      </strong>
                    </td>

                    <td>
                      ${escapeHtml(
                        market.stateName ||
                        market.state?.name ||
                        market.state ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        market.lgaName ||
                        market.lga?.name ||
                        market.lga ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        market.address ||
                        market.location ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${badge(
                        market.status ||
                        (
                          market.active ===
                          false
                            ? 'INACTIVE'
                            : 'ACTIVE'
                        )
                      )}
                    </td>

                  </tr>
                `
              )
              .join('')}

          </tbody>

        </table>
      `;

      return markets;

    } catch (
      error
    ) {

      table.innerHTML =
        emptyState(
          error.message ||
          'Unable to load markets.'
        );

      showError(error);

      return [];
    }
  }

  function openCreateMarket() {

    openModal(`
      <h2 id="modalTitle">
        Add Market
      </h2>

      <p>
        Add a local market to Oyuki Market Square.
      </p>

      <form id="createMarketForm">

        <label>
          Market name
        </label>

        <input
          name="name"
          required
          placeholder="Mile 12 Market"
        >

        <label>
          State
        </label>

        <input
          name="state"
          required
          value="Lagos"
        >

        <label>
          LGA
        </label>

        <input
          name="lga"
          required
          placeholder="Kosofe"
        >

        <label>
          Address
        </label>

        <input
          name="address"
          placeholder="Market address"
        >

        <label>
          Categories
        </label>

        <input
          name="categories"
          placeholder="Vegetables, Fish, Fruits..."
        >

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
            type="submit"
          >
            Add Market
          </button>

        </div>

      </form>
    `);

    document
      .getElementById(
        'createMarketForm'
      )
      ?.addEventListener(
        'submit',
        createMarket
      );
  }

  async function createMarket(
    event
  ) {

    event.preventDefault();

    const form =
      event.currentTarget;

    const button =
      form.querySelector(
        'button[type="submit"]'
      );

    const payload =
      Object.fromEntries(
        new FormData(form)
          .entries()
      );

    if (
      payload.categories
    ) {

      payload.categories =
        payload.categories
          .split(',')
          .map(
            item =>
              item.trim()
          )
          .filter(Boolean);
    }

    try {

      if (
        button
      ) {

        button.disabled =
          true;

        button.textContent =
          'Adding…';
      }

      await Api.post(
        '/admin/markets',
        payload
      );

      closeModal();

      showAlert(
        'Market added successfully.'
      );

      await loadMarkets();

    } catch (
      error
    ) {

      showError(error);

      if (
        button
      ) {

        button.disabled =
          false;

        button.textContent =
          'Add Market';
      }
    }
  }

  /* =========================================================
     MARKET AGENTS
  ========================================================= */

  async function loadMarketAgents() {

    const table =
      document.getElementById(
        'marketAgentsTable'
      );

    if (
      !table
    ) {
      return [];
    }

    table.innerHTML =
      loadingState();

    try {

      const agents =
        arrayFrom(
          await Api.get(
            '/admin/market-agents'
          )
        );

      if (
        !agents.length
      ) {

        table.innerHTML =
          emptyState(
            'No market agents have been created yet.'
          );

        return [];
      }

      table.innerHTML = `
        <table class="admin-table">

          <thead>
            <tr>
              <th>Agent</th>
              <th>Contact</th>
              <th>LGA</th>
              <th>Market</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>

          <tbody>

            ${agents
              .map(
                agent => `
                  <tr>

                    <td>
                      <strong>
                        ${escapeHtml(
                          agent.fullName ||
                          'Market Agent'
                        )}
                      </strong>
                    </td>

                    <td>

                      ${escapeHtml(
                        agent.email ||
                        '—'
                      )}

                      <br>

                      <small>
                        ${escapeHtml(
                          agent.phoneNumber ||
                          ''
                        )}
                      </small>

                    </td>

                    <td>
                      ${escapeHtml(
                        agent.lgaName ||
                        agent.lga?.name ||
                        agent.lga ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${escapeHtml(
                        agent.marketName ||
                        agent.market?.name ||
                        '—'
                      )}
                    </td>

                    <td>
                      ${badge(
                        agent.status ||
                        agent.accountStatus ||
                        'PENDING_VERIFICATION'
                      )}
                    </td>

                    <td>
                      ${formatDate(
                        agent.createdAt
                      )}
                    </td>

                  </tr>
                `
              )
              .join('')}

          </tbody>

        </table>
      `;

      return agents;

    } catch (
      error
    ) {

      table.innerHTML =
        emptyState(
          error.message ||
          'Unable to load market agents.'
        );

      showError(error);

      return [];
    }
  }

  async function openCreateMarketAgent() {

    let markets = [];

    try {

      markets =
        arrayFrom(
          await Api.get(
            '/admin/markets'
          )
        );

    } catch (
      error
    ) {

      console.warn(
        'Could not load markets',
        error
      );
    }

    openModal(`
      <h2 id="modalTitle">
        Create Market Agent
      </h2>

      <p>
        Create an Oyuki market agent and assign the agent to a local market.
      </p>

      <form id="createMarketAgentForm">

        <label>
          Full name
        </label>

        <input
          name="fullName"
          required
          placeholder="Agent full name"
        >

        <label>
          Email address
        </label>

        <input
          name="email"
          type="email"
          required
          placeholder="agent@example.com"
        >

        <label>
          Phone number
        </label>

        <input
          name="phoneNumber"
          type="tel"
          required
          placeholder="+234..."
        >

        <label>
          Assigned market
        </label>

        <select
          name="marketId"
          required
        >

          <option value="">
            Select market
          </option>

          ${markets
            .map(
              market => `
                <option
                  value="${escapeHtml(
                    market.id
                  )}"
                >

                  ${escapeHtml(
                    [
                      market.name ||
                      market.marketName,

                      market.lgaName ||
                      market.lga?.name,

                      market.stateName ||
                      market.state?.name
                    ]
                      .filter(Boolean)
                      .join(
                        ' — '
                      )
                  )}

                </option>
              `
            )
            .join('')}

        </select>

        <label>
          Emergency contact
        </label>

        <input
          name="emergencyContact"
          placeholder="Optional emergency contact"
        >

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
            type="submit"
          >
            Create Market Agent
          </button>

        </div>

      </form>
    `);

    document
      .getElementById(
        'createMarketAgentForm'
      )
      ?.addEventListener(
        'submit',
        createMarketAgent
      );
  }

  async function createMarketAgent(
    event
  ) {

    event.preventDefault();

    const form =
      event.currentTarget;

    const button =
      form.querySelector(
        'button[type="submit"]'
      );

    const payload =
      Object.fromEntries(
        new FormData(form)
          .entries()
      );

    if (
      payload.marketId
    ) {

      payload.marketId =
        Number(
          payload.marketId
        );
    }

    try {

      if (
        button
      ) {

        button.disabled =
          true;

        button.textContent =
          'Creating…';
      }

      await Api.post(
        '/admin/market-agents',
        payload
      );

      closeModal();

      showAlert(
        'Market agent created. Activation OTP has been sent.'
      );

      await loadMarketAgents();

    } catch (
      error
    ) {

      showError(error);

      if (
        button
      ) {

        button.disabled =
          false;

        button.textContent =
          'Create Market Agent';
      }
    }
  }

  /* =========================================================
     ORDERS
  ========================================================= */

  async function loadOrders() {

    const table =
      document.getElementById(
        'ordersTable'
      );

    const overview =
      document.getElementById(
        'overviewOrders'
      );

    if (
      table
    ) {
      table.innerHTML =
        loadingState();
    }

    if (
      overview
    ) {
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

      const total =
        document.getElementById(
          'totalOrders'
        );

      if (
        total
      ) {
        total.textContent =
          orders.length;
      }

      if (
        overview
      ) {

        overview.innerHTML =
          orders.length

            ? orders
                .slice(
                  0,
                  5
                )
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

      if (
        !table
      ) {
        return orders;
      }

      if (
        !orders.length
      ) {

        table.innerHTML =
          emptyState(
            'No orders found.'
          );

        return [];
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
                        order.customer?.fullName ||
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

    } catch (
      error
    ) {

      if (
        table
      ) {

        table.innerHTML =
          emptyState(
            error.message
          );
      }

      showError(error);

      return [];
    }
  }

  /* =========================================================
     PAYMENTS
  ========================================================= */

  async function loadPayments() {

    const table =
      document.getElementById(
        'paymentsTable'
      );

    if (
      !table
    ) {
      return [];
    }

    table.innerHTML =
      loadingState();

    const status =
      document
        .getElementById(
          'paymentStatus'
        )
        ?.value ||
      '';

    try {

      const suffix =
        status
          ? `?status=${encodeURIComponent(
              status
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
              payment.status ||
              ''
            ).toUpperCase() ===
            'PENDING'
        ).length;

      const pendingElement =
        document.getElementById(
          'pendingPayments'
        );

      if (
        pendingElement
      ) {

        pendingElement.textContent =
          pending;
      }

      if (
        !payments.length
      ) {

        table.innerHTML =
          emptyState(
            'No payment proofs found.'
          );

        return [];
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
                        payment.user?.fullName ||
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
                          data-confirm-payment="${
                            payment.id
                          }"
                        >
                          Confirm
                        </button>

                        <button
                          class="danger-button small"
                          data-reject-payment="${
                            payment.id
                          }"
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

    } catch (
      error
    ) {

      table.innerHTML =
        emptyState(
          error.message
        );

      showError(error);

      return [];
    }
  }

  async function confirmPayment(
    id
  ) {

    if (
      !window.confirm(
        'Confirm this payment?'
      )
    ) {
      return;
    }

    try {

      await Api.patch(
        `/admin/payments/${id}/confirm`,
        {
          note:
            'Confirmed by administrator'
        }
      );

      showAlert(
        'Payment confirmed.'
      );

      await loadPayments();

    } catch (
      error
    ) {

      showError(error);
    }
  }

  function showRejectPayment(
    id
  ) {

    openModal(`
      <h2 id="modalTitle">
        Reject Payment
      </h2>

      <textarea
        id="paymentRejectReason"
        placeholder="Enter rejection reason"
      ></textarea>

      <div class="modal-actions">

        <button
          class="secondary-button"
          data-close-modal
        >
          Cancel
        </button>

        <button
          class="danger-button"
          data-confirm-reject-payment="${id}"
        >
          Reject
        </button>

      </div>
    `);
  }

  async function rejectPayment(
    id
  ) {

    const reason =
      document
        .getElementById(
          'paymentRejectReason'
        )
        ?.value
        .trim();

    if (
      !reason
    ) {

      showAlert(
        'Enter a rejection reason.',
        'error'
      );

      return;
    }

    try {

      await Api.patch(
        `/admin/payments/${id}/reject`,
        {
          reason
        }
      );

      closeModal();

      showAlert(
        'Payment rejected.'
      );

      await loadPayments();

    } catch (
      error
    ) {

      showError(error);
    }
  }

  /* =========================================================
     OVERVIEW
  ========================================================= */

  async function loadOverview() {

    await Promise.allSettled([
      loadStatistics(),
      loadApplications(),
      loadOrders(),
      loadPayments()
    ]);
  }

  /* =========================================================
     EVENT HANDLERS
  ========================================================= */

  function bindEvents() {

    elements.menuButton
      ?.addEventListener(
        'click',
        () => {

          elements.sidebar
            ?.classList.toggle(
              'open'
            );
        }
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
      .forEach(
        element => {

          element.addEventListener(
            'click',
            closeModal
          );
        }
      );

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

        /* NAVIGATION */

        const nav =
          event.target.closest(
            '[data-section]'
          );

        if (
          nav
        ) {

          showSection(
            nav.dataset.section
          );

          return;
        }

        const go =
          event.target.closest(
            '[data-go]'
          );

        if (
          go
        ) {

          showSection(
            go.dataset.go
          );

          return;
        }

        /* CREATE MARKETER */

        if (
          event.target.closest(
            '#createMarketerButton'
          )
        ) {

          openCreateMarketer();

          return;
        }

        /* CREATE MARKET AGENT */

        if (
          event.target.closest(
            '#createMarketAgentButton'
          )
        ) {

          await openCreateMarketAgent();

          return;
        }

        /* CREATE MARKET */

        if (
          event.target.closest(
            '#createMarketButton'
          )
        ) {

          openCreateMarket();

          return;
        }

        /* APPLICATION */

        const viewApplication =
          event.target.closest(
            '[data-view-application]'
          );

        if (
          viewApplication
        ) {

          openApplication(
            viewApplication
              .dataset
              .viewApplication
          );

          return;
        }

        const approveApplicationButton =
          event.target.closest(
            '[data-approve-application]'
          );

        if (
          approveApplicationButton
        ) {

          approveApplication(
            approveApplicationButton
              .dataset
              .approveApplication
          );

          return;
        }

        const rejectApplicationButton =
          event.target.closest(
            '[data-reject-application]'
          );

        if (
          rejectApplicationButton
        ) {

          showRejectApplication(
            rejectApplicationButton
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

        /* USER */

        const manageUser =
          event.target.closest(
            '[data-manage-user]'
          );

        if (
          manageUser
        ) {

          openUserManager(
            manageUser
          );

          return;
        }

        const saveUser =
          event.target.closest(
            '[data-save-user-status]'
          );

        if (
          saveUser
        ) {

          saveUserStatus(
            saveUser
              .dataset
              .saveUserStatus
          );

          return;
        }

        /* PAYMENT */

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

        if (
          rejectPaymentButton
        ) {

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

        if (
          event.target.closest(
            '[data-close-modal]'
          )
        ) {

          closeModal();
        }
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

  /* =========================================================
     INITIALISE
  ========================================================= */

  async function initialise() {

    const user =
      requireAdmin();

    if (
      !user
    ) {
      return;
    }

    initialiseAdminName(
      user
    );

    closeModal();

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