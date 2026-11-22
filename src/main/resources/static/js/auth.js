/**
 * Auth JavaScript Module - Owner: Khushi
 * Handles Login, Registration, Password toggles, and Forgot/Reset Password
 */

document.addEventListener('DOMContentLoaded', () => {
  // 1. Login Form Submission
  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const identifier = document.getElementById('identifier').value.trim();
      const password = document.getElementById('password').value;
      const submitBtn = loginForm.querySelector('button[type="submit"]');

      if (!identifier || !password) {
        UrbanTasker.toast('Please enter both identifier and password', 'warning');
        return;
      }

      submitBtn.disabled = true;
      submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Signing In...';

      try {
        const response = await UrbanTasker.api('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({ identifier, password })
        });

        if (response.success) {
          UrbanTasker.session.save(response.data);
          UrbanTasker.toast('Login successful! Redirecting...', 'success');

          setTimeout(() => {
            if (response.data.role === 'admin') {
              window.location.href = '/admin.html';
            } else if (response.data.role === 'provider') {
              window.location.href = '/provider-requests.html';
            } else {
              window.location.href = '/dashboard.html';
            }
          }, 800);
        }
      } catch (err) {
        UrbanTasker.toast(err.message || 'Login failed. Invalid credentials.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'Sign In to UrbanTasker';
      }
    });
  }

  // 2. Registration Form Submission
  const registerForm = document.getElementById('registerForm');
  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('regName').value.trim();
      const username = document.getElementById('regUsername').value.trim();
      const email = document.getElementById('regEmail').value.trim();
      const mobile = document.getElementById('regMobile').value.trim();
      const password = document.getElementById('regPassword').value;
      const role = document.getElementById('regRole') ? document.getElementById('regRole').value : 'user';
      const securityQuestion = document.getElementById('regQuestion').value;
      const securityAnswer = document.getElementById('regAnswer').value.trim();
      const address = document.getElementById('regAddress') ? document.getElementById('regAddress').value.trim() : '';
      const submitBtn = registerForm.querySelector('button[type="submit"]');

      if (!name || !username || !email || !mobile || !password || !securityAnswer) {
        UrbanTasker.toast('Please fill in all mandatory fields', 'warning');
        return;
      }

      if (password.length < 6) {
        UrbanTasker.toast('Password must be at least 6 characters', 'warning');
        return;
      }

      submitBtn.disabled = true;
      submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Creating Account...';

      try {
        const response = await UrbanTasker.api('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({
            name, username, email, mobile, password, role,
            securityQuestion, securityAnswer, address
          })
        });

        if (response.success) {
          UrbanTasker.alert(
            'Registration Successful!',
            'Your account has been created. If you registered as a service provider, please complete your document verification after logging in.',
            'success'
          ).then(() => {
            window.location.href = '/login.html';
          });
        }
      } catch (err) {
        UrbanTasker.toast(err.message || 'Registration failed', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = 'Create Account';
      }
    });
  }

  // 3. Reset Password Form
  const resetForm = document.getElementById('resetPasswordForm');
  if (resetForm) {
    resetForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const identifier = document.getElementById('resetIdentifier').value.trim();
      const securityAnswer = document.getElementById('resetAnswer').value.trim();
      const newPassword = document.getElementById('resetNewPassword').value;

      if (!identifier || !securityAnswer || !newPassword) {
        UrbanTasker.toast('Please provide all recovery details', 'warning');
        return;
      }

      try {
        const res = await UrbanTasker.api('/api/auth/reset-password', {
          method: 'POST',
          body: JSON.stringify({ identifier, securityAnswer, newPassword })
        });
        if (res.success) {
          UrbanTasker.alert('Password Reset Successful', 'You can now log in with your new password.', 'success')
            .then(() => {
              if (window.bootstrap && bootstrap.Modal.getInstance(document.getElementById('forgotModal'))) {
                bootstrap.Modal.getInstance(document.getElementById('forgotModal')).hide();
              } else {
                document.getElementById('forgotModal').classList.remove('active');
              }
            });
        }
      } catch (err) {
        UrbanTasker.toast(err.message || 'Failed to reset password', 'error');
      }
    });
  }
});

// Password visibility toggle helper
function togglePasswordVisibility(inputId, iconElem) {
  const input = document.getElementById(inputId);
  if (!input) return;
  if (input.type === 'password') {
    input.type = 'text';
    iconElem.classList.remove('bi-eye');
    iconElem.classList.add('bi-eye-slash');
  } else {
    input.type = 'password';
    iconElem.classList.remove('bi-eye-slash');
    iconElem.classList.add('bi-eye');
  }
}
