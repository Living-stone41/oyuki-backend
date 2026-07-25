const Api = {
  async request(path, options = {}) {
    const {
      method = 'GET',
      body,
      auth = true,
      headers = {},
      raw = false
    } = options;

    const finalHeaders = {
      Accept: 'application/json',
      ...headers
    };

    const token = localStorage.getItem(STORAGE.token);

    if (auth && token) {
      finalHeaders.Authorization = `Bearer ${token}`;
    }

    let requestBody = body;

    if (
      body !== undefined &&
      body !== null &&
      !(body instanceof FormData)
    ) {
      finalHeaders['Content-Type'] = 'application/json';
      requestBody = JSON.stringify(body);
    }

    let response;

    try {
      response = await fetch(`${API_BASE_URL}${path}`, {
        method,
        headers: finalHeaders,
        body: requestBody
      });
    } catch (error) {
      console.error('API connection error:', error);

      throw new Error(
        'Cannot reach the Oyuki server. Check your internet connection and try again.'
      );
    }

    if (raw) {
      if (!response.ok) {
        throw new Error(`Request failed (${response.status})`);
      }

      return response;
    }

    const text = await response.text();
    let payload = null;

    if (text) {
      try {
        payload = JSON.parse(text);
      } catch {
        payload = text;
      }
    }

    if (!response.ok) {
      if (response.status === 401 && auth && token) {
        localStorage.removeItem(STORAGE.token);
        localStorage.removeItem(STORAGE.user);
      }

      throw new Error(
        errorMessage(
          payload,
          `Request failed (${response.status})`
        )
      );
    }

    return payload;
  },

  get(path, auth = true) {
    return this.request(path, { auth });
  },

  post(path, body, auth = true) {
    return this.request(path, {
      method: 'POST',
      body,
      auth
    });
  },

  put(path, body, auth = true) {
    return this.request(path, {
      method: 'PUT',
      body,
      auth
    });
  },

  patch(path, body, auth = true) {
    return this.request(path, {
      method: 'PATCH',
      body,
      auth
    });
  },

  delete(path, auth = true) {
    return this.request(path, {
      method: 'DELETE',
      auth
    });
  }
};