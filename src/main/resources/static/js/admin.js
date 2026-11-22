/**
 * Admin Panel Controller Script - Owner: Meghna
 * Responsibilities:
 * - Rule 4: The whole flow: register (Khushi), then the verification form (Meghna's provider.html),
 *   then admin approves (Meghna), then the provider dashboard (Divyansh).
 * - Admin analytics & Chart.js reports
 * - Review verification documents (Aadhaar, Police clearance, Address proof, Photo)
 * - Provider approval / rejection with audit trail
 * - User blocking/unblocking
 * - Platform complaints resolution
 */

const AdminModule = {
  chartInstance: null,

  async loadDashboard() {
    if (!UrbanTasker.session.isLoggedIn() || UrbanTasker.session.role !== 'admin') {
      window.location.href = '/login.html';
      return;
    }

    try {
      const res = await UrbanTasker.api('/api/admin/dashboard');
      if (res.success && res.data) {
        const s = res.data;

        // Counters
        const setVal = (id, val) => {
          const el = document.getElementById(id);
          if (el) el.textContent = val;
        };

        setVal('statAdminCustomers', s.totalCustomers);
        setVal('statAdminProviders', s.totalProviders);
        setVal('statAdminPendingProviders', s.pendingProviders);
        setVal('statAdminBookings', s.totalBookings);
        setVal('statAdminCompleted', s.completedBookings);
        setVal('statAdminRevenue', '₹' + Number(s.totalRevenue).toLocaleString('en-IN'));

        // Initialize Chart.js
        this.renderCharts(s);
      }

      // Load sub-sections
      this.loadPendingProviders();
      this.loadUsers();
      this.loadAllBookings();
      this.loadComplaints();
      this.loadLogs();
    } catch (err) {
      console.error("Admin dashboard load failed:", err);
      UrbanTasker.toast("Admin authorization required", "error");
    }
  },

  renderCharts(stats) {
    const ctx = document.getElementById('bookingTrendsChart');
    if (!ctx || !window.Chart) return;

    if (this.chartInstance) {
      this.chartInstance.destroy();
    }

    this.chartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Completed', 'Pending', 'Accepted', 'Cancelled'],
        datasets: [{
          data: [
            stats.completedBookings || 1,
            stats.pendingBookings || 1,
            stats.acceptedBookings || 1,
            stats.cancelledBookings || 0
          ],
          backgroundColor: ['#16A36A', '#F59E0B', '#7043B8', '#DC4545'],
          borderWidth: 2,
          borderColor: '#ffffff'
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { position: 'bottom' }
        }
      }
    });
  },

  // Load Pending Providers for Verification (Meghna)
  async loadPendingProviders() {
    const tbody = document.getElementById('pendingProvidersTbody');
    if (!tbody) return;

    try {
      const res = await UrbanTasker.api('/api/admin/providers/pending');
      if (res.success && res.data) {
        const list = res.data;
        if (list.length === 0) {
          tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No pending provider applications to review. All caught up!</td></tr>`;
          return;
        }

        tbody.innerHTML = list.map(p => `
          <tr>
            <td><strong>#${p.id}</strong></td>
            <td>
              <div style="font-weight:600;">${p.providerName}</div>
              <small class="text-muted"><i class="bi bi-geo-alt"></i> ${p.city || 'Delhi NCR'}</small>
            </td>
            <td><span class="badge badge-accepted">${p.serviceType}</span></td>
            <td>${p.experienceYears || 1} Years</td>
            <td>
              <button onclick="AdminModule.viewProviderDocs(${p.id})" class="btn btn-outline btn-sm">
                <i class="bi bi-file-earmark-text"></i> Audit Docs
              </button>
            </td>
            <td>
              <div style="display:flex; gap:0.4rem;">
                <button onclick="AdminModule.approveProvider(${p.id}, '${p.providerName}')" class="btn btn-accent btn-sm" title="Approve">
                  <i class="bi bi-check-lg"></i> Approve
                </button>
                <button onclick="AdminModule.promptRejectProvider(${p.id}, '${p.providerName}')" class="btn btn-outline btn-sm text-danger" title="Reject">
                  <i class="bi bi-x-lg"></i> Reject
                </button>
              </div>
            </td>
          </tr>
        `).join('');
      }
    } catch (err) {
      console.error("Error loading pending providers:", err);
    }
  },

  // View Provider Details & Documents in Modal (Meghna)
  async viewProviderDocs(providerId) {
    try {
      const res = await UrbanTasker.api(`/api/admin/providers/${providerId}`);
      if (res.success && res.data) {
        const p = res.data;
        document.getElementById('docModalProviderName').textContent = p.providerName;
        document.getElementById('docModalService').textContent = p.serviceType;
        document.getElementById('docModalAddress').textContent = p.address;
        document.getElementById('docModalDetails').textContent = p.serviceDetails || 'No additional details submitted.';
        
        const renderDocLink = (elemId, fileUrl, label) => {
          const el = document.getElementById(elemId);
          if (!el) return;
          if (fileUrl) {
            el.innerHTML = `
              <div class="document-preview-box">
                <i class="bi bi-file-earmark-pdf-fill document-icon"></i>
                <div class="doc-name">${label}</div>
                <div class="doc-actions">
                  <a href="${fileUrl}" target="_blank" class="btn btn-primary btn-sm"><i class="bi bi-eye"></i> View</a>
                </div>
              </div>
            `;
          } else {
            el.innerHTML = `<div class="p-3 text-muted border rounded small">Not Uploaded (Simulated Demo)</div>`;
          }
        };

        renderDocLink('docAadhaarContainer', p.aadhaarFile, 'Govt ID / Aadhaar');
        renderDocLink('docPoliceContainer', p.policeFile, 'Police Clearance Verification');
        renderDocLink('docAddressContainer', p.addressProofFile, 'Address Proof Document');

        // Store active provider id for modal action buttons
        document.getElementById('docModalApproveBtn').onclick = () => AdminModule.approveProvider(p.id, p.providerName);
        document.getElementById('docModalRejectBtn').onclick = () => AdminModule.promptRejectProvider(p.id, p.providerName);

        const modal = document.getElementById('providerDocModal');
        if (modal) modal.classList.add('active');
      }
    } catch (err) {
      UrbanTasker.toast("Could not fetch provider details", "error");
    }
  },

  closeDocModal() {
    const modal = document.getElementById('providerDocModal');
    if (modal) modal.classList.remove('active');
  },

  // Approve Provider (Meghna)
  async approveProvider(providerId, providerName) {
    const confirm = await UrbanTasker.confirm(
      `Approve ${providerName}?`,
      `This will mark the provider as Approved and make them publicly visible in Akansha's service listing.`
    );

    if (confirm.isConfirmed) {
      try {
        const res = await UrbanTasker.api(`/api/admin/providers/${providerId}/approve`, {
          method: 'PUT'
        });
        if (res.success) {
          UrbanTasker.toast(`Provider ${providerName} is now Approved!`, 'success');
          this.closeDocModal();
          this.loadPendingProviders();
          this.loadDashboard();
        }
      } catch (err) {
        UrbanTasker.toast(err.message || 'Approval failed', 'error');
      }
    }
  },

  // Reject Provider with Reason (Meghna)
  async promptRejectProvider(providerId, providerName) {
    let reason = "Submitted verification documents could not be validated or were unclear.";
    if (window.Swal) {
      const { value: inputReason } = await Swal.fire({
        title: `Reject Application: ${providerName}`,
        input: 'textarea',
        inputLabel: 'Specify reason for rejection:',
        inputValue: reason,
        showCancelButton: true,
        confirmButtonColor: '#dc4545',
        confirmButtonText: 'Confirm Rejection'
      });
      if (inputReason) reason = inputReason;
      else return;
    }

    try {
      const res = await UrbanTasker.api(`/api/admin/providers/${providerId}/reject`, {
        method: 'PUT',
        body: JSON.stringify({ reason })
      });
      if (res.success) {
        UrbanTasker.toast('Provider application rejected', 'info');
        this.closeDocModal();
        this.loadPendingProviders();
        this.loadDashboard();
      }
    } catch (err) {
      UrbanTasker.toast(err.message || 'Rejection failed', 'error');
    }
  },

  // Load Users Management
  async loadUsers() {
    const tbody = document.getElementById('usersManagementTbody');
    if (!tbody) return;

    try {
      const res = await UrbanTasker.api('/api/admin/users');
      if (res.success && res.data) {
        tbody.innerHTML = res.data.map(u => `
          <tr>
            <td>#${u.id}</td>
            <td><strong>${u.name}</strong> (@${u.username})</td>
            <td>${u.email}</td>
            <td><span class="badge ${u.role === 'provider' ? 'badge-accepted' : 'badge-pending'}">${u.role}</span></td>
            <td><span class="badge ${u.status === 'ACTIVE' ? 'badge-completed' : 'badge-rejected'}">${u.status || 'ACTIVE'}</span></td>
            <td>
              <button onclick="AdminModule.toggleUserBlock(${u.id}, '${u.username}')" class="btn btn-outline btn-sm ${u.status === 'ACTIVE' ? 'text-danger' : 'text-accent'}">
                ${u.status === 'ACTIVE' ? '<i class="bi bi-slash-circle"></i> Block' : '<i class="bi bi-check-circle"></i> Unblock'}
              </button>
            </td>
          </tr>
        `).join('');
      }
    } catch (e) {}
  },

  async toggleUserBlock(userId, username) {
    try {
      const res = await UrbanTasker.api(`/api/admin/users/${userId}/toggle-block`, { method: 'PUT' });
      if (res.success) {
        UrbanTasker.toast(`User @${username} status updated`, 'info');
        this.loadUsers();
      }
    } catch (err) {
      UrbanTasker.toast(err.message || 'Action failed', 'error');
    }
  },

  // Load All Bookings for Admin Monitoring
  async loadAllBookings() {
    const tbody = document.getElementById('adminBookingsTbody');
    if (!tbody) return;

    try {
      const res = await UrbanTasker.api('/api/admin/bookings');
      if (res.success && res.data) {
        tbody.innerHTML = res.data.map(b => `
          <tr>
            <td><strong>#${b.id}</strong></td>
            <td>${b.userName}</td>
            <td>${b.serviceName}</td>
            <td>${b.providerName}</td>
            <td>${b.bookingDate}</td>
            <td>₹${b.estimatedPrice} (${b.paymentMode})</td>
            <td><span class="badge badge-${b.status.toLowerCase()}">${b.status}</span></td>
          </tr>
        `).join('');
      }
    } catch (e) {}
  },

  // Load Complaints & Logs
  async loadComplaints() {
    const tbody = document.getElementById('complaintsTbody');
    if (!tbody) return;

    try {
      const res = await UrbanTasker.api('/api/admin/complaints');
      if (res.success && res.data) {
        if (res.data.length === 0) {
          tbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">No platform complaints reported.</td></tr>`;
          return;
        }
        tbody.innerHTML = res.data.map(c => `
          <tr>
            <td>#${c.id}</td>
            <td><strong>${c.subject}</strong><br><small class="text-muted">By: ${c.userName}</small></td>
            <td>${c.description}</td>
            <td><span class="badge ${c.status === 'RESOLVED' ? 'badge-completed' : 'badge-pending'}">${c.status}</span></td>
            <td>
              ${c.status !== 'RESOLVED' ? `
                <button onclick="AdminModule.promptResolveComplaint(${c.id})" class="btn btn-secondary btn-sm">Resolve</button>
              ` : `<small class="text-muted">${c.adminResponse || 'Resolved'}</small>`}
            </td>
          </tr>
        `).join('');
      }
    } catch (e) {}
  },

  async promptResolveComplaint(complaintId) {
    let response = "Investigated and issue resolved with customer.";
    if (window.Swal) {
      const { value: inputResp } = await Swal.fire({
        title: 'Resolve Complaint',
        input: 'textarea',
        inputLabel: 'Admin Resolution Notes:',
        inputValue: response,
        showCancelButton: true,
        confirmButtonColor: '#7043b8',
        confirmButtonText: 'Mark Resolved'
      });
      if (inputResp) response = inputResp;
      else return;
    }

    try {
      const res = await UrbanTasker.api(`/api/admin/complaints/${complaintId}/resolve`, {
        method: 'PUT',
        body: JSON.stringify({ adminResponse: response })
      });
      if (res.success) {
        UrbanTasker.toast('Complaint marked resolved', 'success');
        this.loadComplaints();
      }
    } catch (err) {
      UrbanTasker.toast('Action failed', 'error');
    }
  },

  async loadLogs() {
    const list = document.getElementById('adminLogsList');
    if (!list) return;

    try {
      const res = await UrbanTasker.api('/api/admin/logs');
      if (res.success && res.data) {
        list.innerHTML = res.data.slice(0, 15).map(l => `
          <div class="log-item">
            <div class="log-badge"><i class="bi bi-shield-check"></i></div>
            <div class="log-content">
              <h5>${l.action}</h5>
              <p>${l.details}</p>
            </div>
            <div class="log-time">${new Date(l.createdAt).toLocaleTimeString()}</div>
          </div>
        `).join('');
      }
    } catch (e) {}
  }
};
