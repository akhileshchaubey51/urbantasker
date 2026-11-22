/**
 * Customer & Booking Management - Owners: Akansha & Khushi
 * Rule 3: Only approved providers show to customers (WHERE status='Approved')
 * Features: Service filter, Booking placement (Cash/Online), Dashboard metrics, Review submission
 */

const CustomerModule = {
  // Load Customer Dashboard
  async loadDashboard() {
    if (!UrbanTasker.session.isLoggedIn()) {
      window.location.href = '/login.html';
      return;
    }

    try {
      const res = await UrbanTasker.api('/api/customer/dashboard');
      if (res.success && res.data) {
        const d = res.data;
        
        // Welcome and User Info
        const welcomeElem = document.getElementById('welcomeUserName');
        if (welcomeElem) welcomeElem.textContent = d.user.name;

        // Stat Counters
        const setVal = (id, val) => {
          const el = document.getElementById(id);
          if (el) el.textContent = val;
        };

        setVal('statTotalBookings', d.totalBookings);
        setVal('statPendingBookings', d.pendingBookings);
        setVal('statAcceptedBookings', d.acceptedBookings);
        setVal('statCompletedBookings', d.completedBookings);

        // Recent Bookings Table
        this.renderRecentBookings(d.recentBookings);

        // Recommended Providers Cards
        this.renderRecommendedProviders(d.recommendedProviders);

        // Notifications List
        this.renderNotifications(d.notifications);
      }
    } catch (err) {
      console.error("Dashboard load failed:", err);
    }
  },

  renderRecentBookings(bookings) {
    const tbody = document.getElementById('recentBookingsTbody');
    if (!tbody) return;

    if (!bookings || bookings.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No bookings found yet. Explore services to make your first booking!</td></tr>`;
      return;
    }

    tbody.innerHTML = bookings.map(b => {
      let badgeClass = 'badge-pending';
      if (b.status === 'Accepted') badgeClass = 'badge-accepted';
      if (b.status === 'Completed') badgeClass = 'badge-completed';
      if (b.status === 'Rejected' || b.status === 'Cancelled') badgeClass = 'badge-rejected';

      const canCancel = (b.status === 'Pending' || b.status === 'Accepted');
      const canReview = (b.status === 'Completed');

      return `
        <tr>
          <td><strong>#${b.id}</strong></td>
          <td>
            <div style="font-weight:600;">${b.serviceName}</div>
            <small class="text-muted"><i class="bi bi-person"></i> ${b.providerName}</small>
          </td>
          <td>
            <div>${b.bookingDate}</div>
            <small class="text-muted"><i class="bi bi-clock"></i> ${b.bookingTime}</small>
          </td>
          <td><strong>₹${b.estimatedPrice}</strong></td>
          <td>
            <span class="badge ${b.paymentStatus === 'PAID' ? 'badge-completed' : 'badge-pending'}">
              ${b.paymentMode}: ${b.paymentStatus}
            </span>
          </td>
          <td><span class="badge ${badgeClass}">${b.status}</span></td>
          <td>
            <div style="display:flex; gap:0.4rem;">
              ${canCancel ? `<button onclick="CustomerModule.cancelBooking(${b.id})" class="btn btn-outline btn-sm text-danger" title="Cancel Booking"><i class="bi bi-x-circle"></i> Cancel</button>` : ''}
              ${canReview ? `<button onclick="CustomerModule.openReviewModal(${b.id}, '${b.serviceName}', '${b.providerName}')" class="btn btn-secondary btn-sm" title="Rate & Review"><i class="bi bi-star-fill"></i> Review</button>` : ''}
              ${!canCancel && !canReview ? `<span class="text-muted small">No actions</span>` : ''}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  },

  renderRecommendedProviders(providers) {
    const container = document.getElementById('recommendedProvidersGrid');
    if (!container) return;

    if (!providers || providers.length === 0) {
      container.innerHTML = `<div class="text-muted">No verified providers available at the moment.</div>`;
      return;
    }

    container.innerHTML = providers.map(p => `
      <div class="card card-hover" style="display:flex; flex-direction:column; justify-content:space-between;">
        <div>
          <div style="display:flex; align-items:center; gap: 0.9rem; margin-bottom: 0.8rem;">
            <div style="width:48px; height:48px; border-radius:50%; background:var(--secondary-light); color:var(--secondary); display:flex; align-items:center; justify-content:center; font-weight:700; font-size:1.1rem;">
              ${p.providerName.charAt(0)}
            </div>
            <div>
              <h4 style="font-size:1rem; margin-bottom:0.1rem;">${p.providerName}</h4>
              <span class="badge badge-verified"><i class="bi bi-patch-check-fill"></i> Verified</span>
            </div>
          </div>
          <div style="color:var(--secondary); font-weight:600; font-size:0.85rem; margin-bottom:0.4rem;">
            ${p.serviceType}
          </div>
          <p class="text-muted" style="font-size:0.82rem; line-height:1.4; margin-bottom:0.8rem;">
            ${p.serviceDetails ? p.serviceDetails.substring(0, 75) + '...' : 'Certified local service professional.'}
          </p>
        </div>
        <div style="display:flex; align-items:center; justify-content:space-between; pt-2 border-top:1px solid var(--border-light);">
          <div style="font-weight:700; font-size:1.05rem;">₹${p.hourlyRate} <small class="text-muted" style="font-weight:400; font-size:0.75rem;">/hr</small></div>
          <a href="/booking.html?providerId=${p.id}&serviceName=${encodeURIComponent(p.serviceType)}" class="btn btn-primary btn-sm">Book</a>
        </div>
      </div>
    `).join('');
  },

  renderNotifications(notifications) {
    const list = document.getElementById('notificationsList');
    if (!list) return;

    if (!notifications || notifications.length === 0) {
      list.innerHTML = `<div class="p-3 text-center text-muted small">No new notifications</div>`;
      return;
    }

    list.innerHTML = notifications.map(n => `
      <div style="padding: 0.85rem 1rem; border-bottom: 1px solid var(--border-light); display:flex; gap:0.75rem;">
        <i class="bi bi-bell-fill text-secondary" style="font-size:1.1rem; margin-top:2px;"></i>
        <div>
          <div style="font-weight:600; font-size:0.88rem;">${n.title || 'Update'}</div>
          <div style="font-size:0.82rem; color:var(--text-muted);">${n.message}</div>
          <small style="color:#94a3b8; font-size:0.72rem;">${new Date(n.createdAt).toLocaleString()}</small>
        </div>
      </div>
    `).join('');
  },

  // Load Service Listing & Providers Catalog (Akansha's providers.html)
  async loadProvidersCatalog() {
    const query = new URLSearchParams(window.location.search);
    const serviceType = query.get('serviceType') || '';
    const city = query.get('city') || '';
    const sortBy = query.get('sortBy') || 'rating';

    const serviceInput = document.getElementById('filterService');
    const cityInput = document.getElementById('filterCity');
    const sortInput = document.getElementById('filterSort');

    if (serviceInput && serviceType) serviceInput.value = serviceType;
    if (cityInput && city) cityInput.value = city;
    if (sortInput && sortBy) sortInput.value = sortBy;

    try {
      const endpoint = `/api/customer/providers?serviceType=${encodeURIComponent(serviceType)}&city=${encodeURIComponent(city)}&sortBy=${encodeURIComponent(sortBy)}`;
      const res = await UrbanTasker.api(endpoint);

      const grid = document.getElementById('providersGrid');
      if (!grid) return;

      if (!res.success || !res.data || res.data.length === 0) {
        grid.innerHTML = `
          <div class="card p-5 text-center" style="grid-column: 1/-1;">
            <i class="bi bi-search" style="font-size: 2.5rem; color: var(--text-muted); margin-bottom: 1rem;"></i>
            <h3>No Approved Providers Found</h3>
            <p class="text-muted">We couldn't find verified professionals matching your criteria. Try adjusting your search filters.</p>
          </div>
        `;
        return;
      }

      grid.innerHTML = res.data.map(p => `
        <div class="provider-card">
          <div class="provider-card-header">
            <div style="width:68px; height:68px; border-radius:50%; background:var(--secondary-light); color:var(--secondary); display:flex; align-items:center; justify-content:center; font-weight:700; font-size:1.6rem; flex-shrink:0;">
              ${p.providerName.charAt(0)}
            </div>
            <div class="provider-info">
              <h3>${p.providerName} <i class="bi bi-patch-check-fill text-accent" title="Verified Provider"></i></h3>
              <div class="provider-service-tag">${p.serviceType}</div>
              <div class="provider-meta">
                <span class="rating-star"><i class="bi bi-star-fill"></i> ${p.rating || '4.8'} (${p.totalReviews || 0})</span>
                <span><i class="bi bi-geo-alt-fill"></i> ${p.city || 'Delhi NCR'}</span>
                <span><i class="bi bi-briefcase-fill"></i> ${p.experienceYears || 3} yrs exp</span>
              </div>
            </div>
          </div>
          <div class="provider-card-body">
            <p class="provider-desc">${p.serviceDetails || 'Dedicated professional providing high quality home and maintenance services.'}</p>
          </div>
          <div class="provider-card-footer">
            <div class="provider-rate">₹${p.hourlyRate} <span>/ hr</span></div>
            <div style="display:flex; gap:0.5rem;">
              <a href="/provider-info.html?id=${p.id}" class="btn btn-outline btn-sm">Profile</a>
              <a href="/booking.html?providerId=${p.id}&serviceName=${encodeURIComponent(p.serviceType)}" class="btn btn-secondary btn-sm">Book Now</a>
            </div>
          </div>
        </div>
      `).join('');
    } catch (err) {
      console.error("Failed to load providers:", err);
    }
  },

  // Load Provider Profile Page (provider-info.html)
  async loadProviderProfile() {
    const params = new URLSearchParams(window.location.search);
    const providerId = params.get('id');
    if (!providerId) {
      window.location.href = '/providers.html';
      return;
    }

    try {
      const res = await UrbanTasker.api(`/api/customer/providers/${providerId}`);
      if (res.success && res.data) {
        const { provider, reviews } = res.data;

        document.getElementById('providerName').textContent = provider.providerName;
        document.getElementById('providerService').textContent = provider.serviceType;
        document.getElementById('providerCity').textContent = provider.city || 'Delhi NCR';
        document.getElementById('providerExp').textContent = (provider.experienceYears || 3) + ' Years Experience';
        document.getElementById('providerRating').textContent = provider.rating || '4.8';
        document.getElementById('providerReviewsCount').textContent = `(${provider.totalReviews || 0} reviews)`;
        document.getElementById('providerRate').textContent = '₹' + provider.hourlyRate;
        document.getElementById('providerDetails').textContent = provider.serviceDetails;
        document.getElementById('bookNowBtn').href = `/booking.html?providerId=${provider.id}&serviceName=${encodeURIComponent(provider.serviceType)}`;

        // Render reviews
        const reviewsContainer = document.getElementById('providerReviewsContainer');
        if (reviewsContainer) {
          if (!reviews || reviews.length === 0) {
            reviewsContainer.innerHTML = `<div class="text-muted p-4 text-center">No reviews yet. Be the first customer to book and rate!</div>`;
          } else {
            reviewsContainer.innerHTML = reviews.map(r => `
              <div style="border-bottom:1px solid var(--border-light); padding: 1.1rem 0;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:0.3rem;">
                  <strong>${r.customerName}</strong>
                  <div class="text-warning small">${'★'.repeat(r.rating)}${'☆'.repeat(5 - r.rating)}</div>
                </div>
                <p class="text-muted" style="font-size:0.9rem; margin-bottom:0.3rem;">${r.comment || 'Great service rendered on time.'}</p>
                <small style="color:#94a3b8; font-size:0.75rem;">${new Date(r.createdAt).toLocaleDateString()}</small>
              </div>
            `).join('');
          }
        }
      }
    } catch (err) {
      UrbanTasker.toast(err.message || 'Error loading profile', 'error');
    }
  },

  // Cancel Booking
  async cancelBooking(bookingId) {
    const confirm = await UrbanTasker.confirm(
      'Cancel Service Booking?',
      'Are you sure you want to cancel this booking? This action cannot be reversed.'
    );

    if (confirm.isConfirmed) {
      try {
        const res = await UrbanTasker.api(`/api/bookings/${bookingId}/cancel`, {
          method: 'PUT',
          body: JSON.stringify({ reason: 'Cancelled by customer via dashboard' })
        });
        if (res.success) {
          UrbanTasker.toast('Booking cancelled successfully', 'info');
          this.loadDashboard();
        }
      } catch (err) {
        UrbanTasker.toast(err.message || 'Failed to cancel booking', 'error');
      }
    }
  },

  // Open Review Modal
  openReviewModal(bookingId, serviceName, providerName) {
    document.getElementById('reviewBookingId').value = bookingId;
    document.getElementById('reviewModalTitle').textContent = `Review: ${serviceName} by ${providerName}`;
    const modal = document.getElementById('reviewModal');
    if (modal) modal.classList.add('active');
  },

  closeReviewModal() {
    const modal = document.getElementById('reviewModal');
    if (modal) modal.classList.remove('active');
  },

  async submitReview(e) {
    e.preventDefault();
    const bookingId = document.getElementById('reviewBookingId').value;
    const rating = document.getElementById('reviewRating').value;
    const comment = document.getElementById('reviewComment').value.trim();

    try {
      const res = await UrbanTasker.api('/api/bookings/review', {
        method: 'POST',
        body: JSON.stringify({ bookingId: Number(bookingId), rating: Number(rating), comment })
      });
      if (res.success) {
        UrbanTasker.alert('Review Submitted', 'Thank you for rating your service experience!', 'success');
        this.closeReviewModal();
        this.loadDashboard();
      }
    } catch (err) {
      UrbanTasker.toast(err.message || 'Failed to submit review', 'error');
    }
  }
};
