/**
 * MEDICARE - AUTHENTICATION CONTROLLER
 * Handles registration, login, logout, and protected route access guards.
 */

document.addEventListener('DOMContentLoaded', () => {
  const isAuthPage = window.location.pathname.endsWith('login.html') || window.location.pathname.endsWith('register.html');
  const isLandingPage = window.location.pathname.endsWith('index.html') || window.location.pathname === '/' || window.location.pathname.endsWith('/');

  // Route protection
  if (!isAuthPage && !isLandingPage && !API.isAuthenticated()) {
    window.location.href = 'login.html';
    return;
  }

  if (isAuthPage && API.isAuthenticated()) {
    window.location.href = 'dashboard.html';
    return;
  }

  // Populate mini user info in sidebar if present
  const user = API.getUser();
  if (user) {
    const nameEl = document.querySelector('.user-info-name');
    const avatarEl = document.querySelector('.user-avatar');
    if (nameEl) nameEl.textContent = user.name || 'User';
    if (avatarEl) {
      const initials = (user.name || 'U').split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
      avatarEl.textContent = initials;
    }
  }

  // Setup Logout Buttons
  document.querySelectorAll('.logout-btn').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      try {
        await API.request('/api/auth/logout', { method: 'POST' });
      } catch (err) {
        // Continue clearing session locally regardless
      }
      API.clearSession();
      API.showToast('You have been logged out safely.', 'info');
      setTimeout(() => {
        window.location.href = 'login.html';
      }, 500);
    });
  });

  // Password visibility toggle helper
  document.querySelectorAll('.password-toggle-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const input = btn.previousElementSibling;
      if (input && input.type === 'password') {
        input.type = 'text';
        btn.textContent = '🔒';
      } else if (input) {
        input.type = 'password';
        btn.textContent = '👁️';
      }
    });
  });

  function isValidEmail(email) {
    const re = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,64}$/;
    return re.test(String(email).trim());
  }

  // Handle Login Form
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;
      const submitBtn = loginForm.querySelector('button[type="submit"]');

      if (!email || !password) {
        API.showToast('Please enter both email and password.', 'error');
        return;
      }

      if (!isValidEmail(email)) {
        API.showToast('Please enter a valid email address (e.g. user@example.com).', 'error');
        return;
      }

      const origText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<span class="spinner"></span> Signing In...';

      try {
        const resp = await API.request('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password })
        });

        API.setSession(resp.token, {
          id: resp.id,
          name: resp.name,
          email: resp.email,
          role: resp.role,
          timezone: resp.timezone
        });

        API.showToast(`Welcome back, ${resp.name}!`, 'success');
        setTimeout(() => {
          window.location.href = 'dashboard.html';
        }, 800);
      } catch (error) {
        API.showToast(error.message || 'Login failed. Please check credentials.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = origText;
      }
    });
  }

  // Handle Register Form
  const registerForm = document.getElementById('register-form');
  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('name').value.trim();
      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;
      const confirmPassword = document.getElementById('confirmPassword').value;
      const phone = document.getElementById('phone').value.trim();
      const dob = document.getElementById('dob').value;
      const emergencyContact = document.getElementById('emergencyContact').value.trim();
      const timezone = document.getElementById('timezone') ? document.getElementById('timezone').value : 'UTC';

      if (!isValidEmail(email)) {
        API.showToast('Please enter a valid email address (e.g. user@example.com).', 'error');
        return;
      }

      if (password !== confirmPassword) {
        API.showToast('Passwords do not match.', 'error');
        return;
      }

      if (password.length < 6) {
        API.showToast('Password must be at least 6 characters.', 'error');
        return;
      }

      const submitBtn = registerForm.querySelector('button[type="submit"]');
      const origText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<span class="spinner"></span> Creating Account...';

      try {
        const payload = {
          name,
          email,
          password,
          phone,
          dateOfBirth: dob || null,
          emergencyContact,
          timezone
        };

        const resp = await API.request('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify(payload)
        });

        API.setSession(resp.token, {
          id: resp.id,
          name: resp.name,
          email: resp.email,
          role: resp.role,
          timezone: resp.timezone
        });

        API.showToast('Account registered successfully!', 'success');
        setTimeout(() => {
          window.location.href = 'dashboard.html';
        }, 800);
      } catch (error) {
        API.showToast(error.message || 'Registration failed.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = origText;
      }
    });
  }
});
