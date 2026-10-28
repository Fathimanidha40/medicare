/**
 * MEDICARE - CENTRALIZED API CLIENT
 * Handles authentication tokens, unified Fetch calls, JSON decoding, and toast notifications.
 */

const API = (() => {
  // Automatically detects whether running on Spring Boot (port 8080) or separate dev server
  const BASE_URL = window.location.origin.includes('8080')
    ? window.location.origin
    : 'http://localhost:8080';

  const TOKEN_KEY = 'medicare_jwt_token';
  const USER_KEY = 'medicare_user';

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setSession(token, user) {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function getUser() {
    const userStr = localStorage.getItem(USER_KEY);
    try {
      return userStr ? JSON.parse(userStr) : null;
    } catch (e) {
      return null;
    }
  }

  function isAuthenticated() {
    return !!getToken();
  }

  async function request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;
    const headers = options.headers || {};

    const token = getToken();
    if (token && !headers['Authorization']) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    // Default to JSON if body is not FormData
    if (options.body && !(options.body instanceof FormData) && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    const config = {
      ...options,
      headers
    };

    try {
      const response = await fetch(url, config);

      // Handle 401 Unauthorized (Expired or Invalid Session)
      if (response.status === 401) {
        if (!endpoint.includes('/api/auth/login')) {
          clearSession();
          showToast('Session expired. Please log in again.', 'warning');
          setTimeout(() => {
            if (!window.location.pathname.endsWith('login.html')) {
              window.location.href = 'login.html';
            }
          }, 1200);
          throw new Error('Session expired');
        }
      }

      // Check for Blob or file download
      const contentType = response.headers.get('content-type') || '';
      if (endpoint.includes('/file') || endpoint.includes('/export/') || contentType.includes('text/csv') || contentType.includes('application/pdf') || contentType.includes('image/') || contentType.includes('application/octet-stream')) {
        if (!response.ok) {
          const errText = await response.text();
          let errJson;
          try { errJson = JSON.parse(errText); } catch(e) {}
          throw new Error((errJson && errJson.message) ? errJson.message : 'Failed to retrieve file');
        }
        return await response.blob();
      }

      const isJson = contentType && contentType.includes('application/json');
      const data = isJson ? await response.json() : await response.text();

      if (!response.ok) {
        const errorMsg = (data && data.message) ? data.message : `Error (${response.status}): ${response.statusText}`;
        throw new Error(errorMsg);
      }

      return data;
    } catch (error) {
      console.error(`API Request Error [${endpoint}]:`, error);
      throw error;
    }
  }

  // Toast Notification Engine
  function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '❌';
    if (type === 'warning') icon = '⚠️';

    toast.innerHTML = `
      <div style="display: flex; align-items: center; gap: 0.6rem;">
        <span>${icon}</span>
        <span>${message}</span>
      </div>
      <button style="background: none; border: none; cursor: pointer; color: inherit; opacity: 0.7;" onclick="this.parentElement.remove()">&times;</button>
    `;

    container.appendChild(toast);

    setTimeout(() => {
      toast.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  return {
    BASE_URL,
    getToken,
    setSession,
    clearSession,
    getUser,
    isAuthenticated,
    request,
    showToast
  };
})();

// Theme Management (Light / Dark)
document.addEventListener('DOMContentLoaded', () => {
  const savedTheme = localStorage.getItem('medicare_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);

  const themeToggles = document.querySelectorAll('.theme-toggle-btn');
  themeToggles.forEach(btn => {
    btn.innerHTML = savedTheme === 'dark' ? '☀️' : '🌙';
    btn.setAttribute('aria-label', 'Toggle light/dark mode');
    btn.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('medicare_theme', next);
      themeToggles.forEach(b => b.innerHTML = next === 'dark' ? '☀️' : '🌙');
    });
  });

  // Mobile sidebar drawer toggles
  const menuBtn = document.querySelector('.mobile-menu-btn');
  const sidebar = document.querySelector('.sidebar');
  const backdrop = document.querySelector('.sidebar-backdrop');

  if (menuBtn && sidebar && backdrop) {
    menuBtn.addEventListener('click', () => {
      sidebar.classList.toggle('active');
      backdrop.classList.toggle('active');
    });

    backdrop.addEventListener('click', () => {
      sidebar.classList.remove('active');
      backdrop.classList.remove('active');
    });
  }
});
