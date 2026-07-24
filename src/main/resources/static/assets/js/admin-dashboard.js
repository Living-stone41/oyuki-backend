function renderContacts() {
  const newCount = state.contacts.filter(
    message => message.status === 'NEW'
  ).length;

  $('#view').innerHTML = `
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2">
      <div>
        <h3 class="mb-1">
          Contact messages

          <span
            id="contactMessageBadge"
            class="badge text-bg-danger ms-2"
            style="${newCount > 0 ? '' : 'display:none'}"
          >
            ${newCount}
          </span>
        </h3>

        <p class="text-muted mb-0">
          Read and manage messages sent from the public contact page.
        </p>
      </div>

      <button
        class="btn btn-outline-brand"
        onclick="AdminDashboard.refreshContacts()"
      >
        <i class="bi bi-arrow-clockwise"></i>
        Refresh
      </button>
    </div>

    <div class="panel mt-3">
      ${contactMessagesTable(state.contacts)}
    </div>
  `;
}

function contactMessagesTable(messages) {
  if (!messages.length) {
    return `
      <p class="text-muted text-center py-4 mb-0">
        No contact messages yet.
      </p>
    `;
  }

  return `
    <div class="table-responsive">
      <table class="oy-table">
        <thead>
          <tr>
            <th>Sender</th>
            <th>Subject</th>
            <th>Status</th>
            <th>Received</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody id="contactMessagesBody">
          ${messages.map(contactMessageRow).join('')}
        </tbody>
      </table>
    </div>
  `;
}

function contactMessageRow(message) {
  return `
    <tr data-contact-message-id="${Number(message.id)}">
      <td>
        <strong>
          ${e(message.fullName || 'Unknown sender')}
        </strong>

        <div class="small text-muted">
          ${e(message.email || '')}
        </div>

        <div class="small text-muted">
          ${e(message.phoneNumber || '')}
        </div>
      </td>

      <td>
        <strong>
          ${e(message.subject || 'No subject')}
        </strong>

        <div
          class="small text-muted text-truncate"
          style="max-width:320px"
        >
          ${e(message.message || '')}
        </div>
      </td>

      <td data-contact-status>
        ${statusPill(message.status)}
      </td>

      <td>
        ${e(Oyuki.date(message.createdAt))}
      </td>

      <td class="text-nowrap">
        <button
          class="btn btn-sm btn-ghost"
          onclick="AdminDashboard.viewContactMessage(${Number(message.id)})"
        >
          View
        </button>

        <button
          class="btn btn-sm btn-outline-danger"
          onclick="AdminDashboard.deleteContactMessage(${Number(message.id)})"
        >
          Delete
        </button>
      </td>
    </tr>
  `;
}

async function loadContactMessagesFromApi(status = '') {
  const suffix = status
    ? `?status=${encodeURIComponent(status)}`
    : '';

  const response = await Oyuki.Api.get(
    '/admin/contact-messages' + suffix
  );

  return Oyuki.unwrap(response) || [];
}

async function refreshContacts() {
  try {
    state.contacts =
      await loadContactMessagesFromApi();

    renderContacts();
  } catch (error) {
    Oyuki.Toast.show(
      error.message,
      'error'
    );
  }
}

async function viewContactMessage(id) {
  try {
    const response = await Oyuki.Api.get(
      `/admin/contact-messages/${id}`
    );

    const message = Oyuki.unwrap(response);

    showModal(
      'Contact message',
      `
        <div class="row g-3">
          <div class="col-md-6">
            <strong>Sender</strong>
            <div>
              ${e(message.fullName || '—')}
            </div>
          </div>

          <div class="col-md-6">
            <strong>Status</strong>
            <div>
              ${statusPill(message.status)}
            </div>
          </div>

          <div class="col-md-6">
            <strong>Email</strong>
            <div>
              <a href="mailto:${e(message.email || '')}">
                ${e(message.email || '—')}
              </a>
            </div>
          </div>

          <div class="col-md-6">
            <strong>Phone</strong>
            <div>
              ${e(message.phoneNumber || '—')}
            </div>
          </div>

          <div class="col-12">
            <strong>Subject</strong>
            <div>
              ${e(message.subject || '—')}
            </div>
          </div>

          <div class="col-12">
            <strong>Message</strong>

            <p
              class="mt-2 mb-0"
              style="white-space:pre-wrap"
            >
              ${e(message.message || '—')}
            </p>
          </div>

          <div class="col-md-6">
            <strong>Received</strong>
            <div>
              ${e(Oyuki.date(message.createdAt))}
            </div>
          </div>

          <div class="col-md-6">
            <strong>Updated</strong>
            <div>
              ${e(Oyuki.date(message.updatedAt))}
            </div>
          </div>

          <div class="col-12 d-flex flex-wrap gap-2 pt-2">
            ${['NEW', 'READ', 'REPLIED', 'CLOSED']
              .map(status => `
                <button
                  class="btn btn-sm ${
                    message.status === status
                      ? 'btn-brand'
                      : 'btn-outline-brand'
                  }"
                  onclick="AdminDashboard.updateContactStatus(
                    ${Number(message.id)},
                    '${status}'
                  )"
                >
                  ${e(pretty(status))}
                </button>
              `)
              .join('')}
          </div>
        </div>
      `
    );
  } catch (error) {
    Oyuki.Toast.show(
      error.message,
      'error'
    );
  }
}

async function updateContactStatus(id, status) {
  try {
    const response = await Oyuki.Api.patch(
      `/admin/contact-messages/${id}`,
      { status }
    );

    const updated = Oyuki.unwrap(response);

    state.contacts = state.contacts.map(
      message =>
        Number(message.id) === Number(id)
          ? updated
          : message
    );

    closeModal();
    renderContacts();

    Oyuki.Toast.show(
      'Contact message updated',
      'success'
    );
  } catch (error) {
    Oyuki.Toast.show(
      error.message,
      'error'
    );
  }
}

async function deleteContactMessage(id) {
  const confirmed = confirm(
    'Delete this contact message?'
  );

  if (!confirmed) return;

  try {
    await Oyuki.Api.delete(
      `/admin/contact-messages/${id}`
    );

    state.contacts = state.contacts.filter(
      message =>
        Number(message.id) !== Number(id)
    );

    renderContacts();

    Oyuki.Toast.show(
      'Contact message deleted',
      'success'
    );
  } catch (error) {
    Oyuki.Toast.show(
      error.message,
      'error'
    );
  }
  
}