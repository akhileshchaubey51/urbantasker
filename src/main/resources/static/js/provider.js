/**
 * Provider Panel Controller Script - Owner: Divyansh
 * Responsibilities:
 * - Rule 1: Uses providerId session key
 * - Rule 2: Booking status words. Use exactly Pending, Accepted and Rejected.
 *   Khushi's booking page saves Pending. Divyansh changes it to Accepted, Rejected or Completed.
 * - Earnings & Requests metrics
 * - Availability toggle
 */

const ProviderModule = {
  async loadDashboard() {
    if (!UrbanTasker.session.isLoggedIn() || UrbanTasker.session.role !== 'provider') {
      window.location.href = '/login.html';
      return;
    }

    try {
      const res = await UrbanTasker.api('/api/provider/dashboard');
      if (res.success && res.data) {
        const d = res.data;
        const p = d.provider;

        // Header info
        const nameEl = document.getElementById('providerDisplayName');
        if (nameEl) nameEl.textContent = p.providerName;

        const serviceEl = document.getElementById('providerServiceBadge');
        if (serviceEl) serviceEl.textContent = p.serviceType;

        const statusEl = document.getElementById('providerStatusBadge');
        if (statusEl) {
          statusEl.textContent = p.status;
          statusEl.className = `badge ${p.status === 'Approved' ? 'badge-completed' : (p.status === 'Pending' ? 'badge-pending' : 'badge-rejected')}`;
        }

        // Availability Toggle
        const availSwitch = document.getElementById('availabilitySwitch');
        if (availSwitch) {
          availSwitch.checked = Boolean(p.isAvailable);
        }

        // Metrics
        const setVal = (id, val) => {
          const el = document.getElementById(id);
          if (el) el.textContent = val;
        };

        setVal('statTotalRequests', d.totalRequests);
        setVal('statPendingRequests', d.pendingRequests);
        setVal('statAcceptedRequests', d.acceptedRequests);
        setVal('statCompletedBookings', d.completedBookings);
        setVal('statTotalEarnings', '₹' + Number(d.totalEarnings).toLocaleString('en-IN'));

        // Render Requests
        this.renderIncomingRequests(d.recentRequests);
      }
    } catch (err) {
      console.error("Provider dashboard failed:", err);
      UrbanTasker.toast("Could not load provider data. Make sure you are an approved provider.", "warning");
    }
  },

  renderIncomingRequests(requests) {
    const container = document.getElementById('requestsContainer');
    if (!container) return;

    if (!requests || requests.length === 0) {
      container.innerHTML = `
        <div class="card p-5 text-center">
          <i class="bi bi-inbox text-muted" style="font-size:3rem; margin-bottom:1rem;"></i>
          <h4>No Service Requests Yet</h4>
          <p class="text-muted">New booking requests from customers in your area will appear here in real time.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = requests.map(r => {
      let badgeClass = 'badge-pending';
      if (r.status === 'Accepted') badgeClass = 'badge-accepted';
      if (r.status === 'Completed') badgeClass = 'badge-completed';
      if (r.status === 'Rejected') badgeClass = 'badge-rejected';

      const isPending = (r.status === 'Pending');
      const isAccepted = (r.status === 'Accepted');

      return `
        <div class="request-card">
          <div class="request-header">
            <div class="request-user-info">
              <div class="request-avatar">${r.userName.charAt(0)}</div>
              <div>
                <h4 style="font-size:1.05rem; margin-bottom:0.1rem;">${r.userName}</h4>
                <small class="text-muted">Booking #${r.id} • ${new Date(r.createdAt).toLocaleDateString()}</small>
              </div>
            </div>
            <div style="display:flex; align-items:center; gap:0.6rem;">
              <span class="badge ${badgeClass}">${r.status}</span>
              <span class="badge ${r.paymentStatus === 'PAID' ? 'badge-completed' : 'badge-pending'}">
                <i class="bi bi-credit-card"></i> ${r.paymentMode}: ${r.paymentStatus}
              </span>
            </div>
          </div>

          <div class="request-body-details">
            <div class="detail-item">
              <i class="bi bi-tools"></i>
              <div>
                <strong>Service</strong>
                <div>${r.serviceName}</div>
              </div>
            </div>
            <div class="detail-item">
              <i class="bi bi-calendar-event"></i>
              <div>
                <strong>Date & Time</strong>
                <div>${r.bookingDate} at ${r.bookingTime}</div>
              </div>
            </div>
            <div class="detail-item">
              <i class="bi bi-geo-alt"></i>
              <div>
                <strong>Location & Contact</strong>
                <div>${r.serviceAddress}</div>
                <small class="text-muted"><i class="bi bi-telephone"></i> ${r.contactNumber || 'N/A'}</small>
              </div>
            </div>
          </div>

          ${r.instructions ? `
            <div style="background:#f8fafc; padding:0.75rem 1rem; border-radius:var(--radius-sm); font-size:0.85rem; border-left:3px solid var(--secondary);">
              <strong>Instructions:</strong> ${r.instructions}
            </div>
          ` : ''}

          <div class="request-footer">
            <div class="request-price-tag">₹${r.estimatedPrice}</div>
            <div class="request-actions">
              ${isPending ? `
                <button onclick="ProviderModule.acceptRequest(${r.id})" class="btn btn-accent btn-sm">
                  <i class="bi bi-check-lg"></i> Accept Request
                </button>
                <button onclick="ProviderModule.promptRejectRequest(${r.id})" class="btn btn-outline btn-sm text-danger">
                  <i class="bi bi-x-lg"></i> Reject
                </button>
              ` : ''}

              ${isAccepted ? `
                <button onclick="ProviderModule.completeRequest(${r.id})" class="btn btn-primary btn-sm">
                  <i class="bi bi-check2-circle"></i> Mark as Completed
                </button>
              ` : ''}

              ${!isPending && !isAccepted ? `
                <span class="text-muted small">Processed (${r.status})</span>
              ` : ''}
            </div>
          </div>
        </div>
      `;
    }).join('');
  },

  // Accept Request (Rule 2)
  async acceptRequest(bookingId) {
    try {
      const res = await UrbanTasker.api(`/api/provider/requests/${bookingId}/accept`, {
        method: 'PUT'
      });
      if (res.success) {
        UrbanTasker.toast('Booking accepted! Customer has been notified.', 'success');
        this.loadDashboard();
      }
    } catch (err) {
      UrbanTasker.toast(err.message || 'Failed to accept request', 'error');
    }
  },

  // Reject Request (Rule 2)
  async promptRejectRequest(bookingId) {
    let reason = "Provider is currently unavailable at this requested time slot.";
    if (window.Swal) {
      const { value: inputReason } = await Swal.fire({
        title: 'Reject Service Request',
        input: 'text',
        inputLabel: 'Please provide a reason for the customer:',
        inputValue: reason,
        showCancelButton: true,
        confirmButtonColor: '#dc4545',
        confirmButtonText: 'Confirm Rejection'
      });
      if (inputReason) reason = inputReason;
      else return; // user cancelled
    }

    try {
      const res = await UrbanTasker.api(`/api/provider/requests/${bookingId}/reject`, {
        method: 'PUT',
        body: JSON.stringify({ reason })
      });
      if (res.success) {
        UrbanTasker.toast('Request rejected and customer notified.', 'info');
        this.loadDashboard();
      }
    } catch (err) {
      UrbanTasker.toast(err.message || 'Failed to reject request', 'error');
    }
  },

  // Complete Request
  async completeRequest(bookingId) {
    const confirm = await UrbanTasker.confirm(
      'Mark Booking Completed?',
      'Have you finished delivering the service? If Cash payment, ensure payment was collected.'
    );

    if (confirm.isConfirmed) {
      try {
        const res = await UrbanTasker.api(`/api/provider/requests/${bookingId}/complete`, {
          method: 'PUT'
        });
        if (res.success) {
          UrbanTasker.alert('Service Completed!', 'Earnings have been credited to your overview and customer can now leave a rating.', 'success');
          this.loadDashboard();
        }
      } catch (err) {
        UrbanTasker.toast(err.message || 'Failed to complete booking', 'error');
      }
    }
  },

  // Toggle Availability
  async toggleAvailability() {
    try {
      const res = await UrbanTasker.api('/api/provider/toggle-availability', { method: 'PUT' });
      if (res.success) {
        const isAvail = Boolean(res.data.isAvailable);
        UrbanTasker.toast(isAvail ? 'You are now marked Available for bookings' : 'You are now marked Busy', 'info');
      }
    } catch (err) {
      UrbanTasker.toast('Failed to update availability status', 'error');
    }
  }
};
