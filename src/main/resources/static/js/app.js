/**
 * UrbanTasker - Central Application Engine
 * Session Management, Fetch API Helper, UI Utilities & Toast Alerts
 */

const UrbanTasker = {
  // PBL Image 2 Rule 1: Session Keys
  session: {
    userId: null,
    name: null,
    role: null, // 'user', 'provider', 'admin'
    providerId: null,

    init() {
      const stored = localStorage.getItem('urbantasker_session');
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          this.userId = parsed.userId || null;
          this.name = parsed.name || null;
          this.role = parsed.role ? parsed.role.toLowerCase() : null;
          this.providerId = parsed.providerId || null;
        } catch (e) {
          console.error("Session parse error", e);
        }
      }
      this.updateNavbar();
    },

    save(data) {
      this.userId = data.userId;
      this.name = data.name;
      this.role = data.role ? data.role.toLowerCase() : 'user';
      this.providerId = data.providerId || null;

      localStorage.setItem('urbantasker_session', JSON.stringify({
        userId: this.userId,
        name: this.name,
        role: this.role,
        providerId: this.providerId
      }));
      this.updateNavbar();
    },

    clear() {
      this.userId = null;
      this.name = null;
      this.role = null;
      this.providerId = null;
      localStorage.removeItem('urbantasker_session');
      this.updateNavbar();
    },

    isLoggedIn() {
      return !!this.userId;
    },

    updateNavbar() {
      const authContainer = document.getElementById('navbarAuthSection');
      if (!authContainer) return;

      if (this.isLoggedIn()) {
        let dashboardUrl = '/dashboard.html';
        let roleBadge = 'Customer';

        if (this.role === 'admin') {
          dashboardUrl = '/admin.html';
          roleBadge = 'Admin';
        } else if (this.role === 'provider') {
          dashboardUrl = '/provider-requests.html';
          roleBadge = 'Provider';
        }

        authContainer.innerHTML = `
          <div style="display:flex; align-items:center; gap: 0.85rem;">
            <a href="${dashboardUrl}" class="btn btn-outline btn-sm">
              <i class="bi bi-grid-fill"></i> ${this.name} (${roleBadge})
            </a>
            <button onclick="UrbanTasker.auth.logout()" class="btn btn-danger btn-sm" title="Logout">
              <i class="bi bi-box-arrow-right"></i>
            </button>
          </div>
        `;
      } else {
        authContainer.innerHTML = `
          <a href="/login.html" class="btn btn-outline btn-sm">Log In</a>
          <a href="/register.html" class="btn btn-primary btn-sm">Sign Up</a>
        `;
      }
    }
  },

  // Centralized Fetch API Caller
  async api(endpoint, options = {}) {
    const defaultHeaders = {
      'Accept': 'application/json'
    };

    if (!(options.body instanceof FormData)) {
      defaultHeaders['Content-Type'] = 'application/json';
    }

    const config = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options.headers
      }
    };

    try {
      const res = await fetch(endpoint, config);
      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || 'Server error occurred');
      }
      return json;
    } catch (err) {
      console.error(`API Error on ${endpoint}:`, err);
      throw err;
    }
  },

  // Polished Toast / Alert using SweetAlert2 if loaded, fallback to native notification
  toast(message, icon = 'success') {
    if (window.Swal) {
      const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3500,
        timerProgressBar: true
      });
      Toast.fire({ icon, title: message });
    } else {
      alert(message);
    }
  },

  alert(title, text, icon = 'info') {
    if (window.Swal) {
      return Swal.fire({ title, text, icon, confirmButtonColor: '#7043b8' });
    } else {
      alert(`${title}: ${text}`);
      return Promise.resolve({ isConfirmed: true });
    }
  },

  confirm(title, text, confirmText = 'Yes, Proceed') {
    if (window.Swal) {
      return Swal.fire({
        title,
        text,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#7043b8',
        cancelButtonColor: '#dc4545',
        confirmButtonText: confirmText
      });
    } else {
      return Promise.resolve({ isConfirmed: confirm(text) });
    }
  },

  auth: {
    async logout() {
      try {
        await UrbanTasker.api('/api/auth/logout', { method: 'POST' });
      } catch (e) {}
      UrbanTasker.session.clear();
      UrbanTasker.toast('Logged out successfully', 'info');
      setTimeout(() => {
        window.location.href = '/index.html';
      }, 700);
    }
  },

  // Initialize interactive features
  setupCards3DTilt() {
    document.querySelectorAll('.card-tilt').forEach(card => {
      card.addEventListener('mousemove', (e) => {
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left - rect.width / 2;
        const y = e.clientY - rect.top - rect.height / 2;
        card.style.transform = `perspective(1000px) rotateX(${-y / 15}deg) rotateY(${x / 15}deg) scale3d(1.02, 1.02, 1.02)`;
      });
      card.addEventListener('mouseleave', () => {
        card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)';
      });
    });
  }
};

document.addEventListener('DOMContentLoaded', () => {
  UrbanTasker.session.init();
  UrbanTasker.setupCards3DTilt();
});
