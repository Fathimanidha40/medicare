/**
 * MEDICARE - DASHBOARD CONTROLLER
 * Real-time dashboard metrics, today's schedule, dose actions (Taken/Skipped/Snooze),
 * low stock alerts, and upcoming doctor appointments.
 */

document.addEventListener('DOMContentLoaded', () => {
  loadDashboardData();
  setupQuickActionModals();
  setupNotificationEngine();
});

let notifiedDoseIds = new Set();

function setupNotificationEngine() {
  const topBtn = document.getElementById('enable-notifications-btn');
  const banner = document.getElementById('notification-banner');
  const bannerAllowBtn = document.getElementById('banner-enable-notifications-btn');
  const bannerDismissBtn = document.getElementById('banner-dismiss-notifications-btn');

  if (!('Notification' in window)) {
    if (topBtn) topBtn.style.display = 'none';
    if (banner) banner.style.display = 'none';
    return;
  }

  function updateUiState() {
    if (Notification.permission === 'granted') {
      if (topBtn) {
        topBtn.textContent = '🔔 Alerts Active';
        topBtn.classList.remove('btn-outline');
        topBtn.classList.add('btn-secondary');
      }
      if (banner) banner.style.display = 'none';
    } else if (Notification.permission === 'denied') {
      if (topBtn) {
        topBtn.textContent = '🔕 Alerts Blocked';
        topBtn.title = 'Notifications are blocked in your browser site settings';
      }
      if (banner) banner.style.display = 'none';
    } else {
      if (sessionStorage.getItem('medicare_dismiss_notif_banner') !== 'true') {
        if (banner) banner.style.display = 'block';
      }
    }
  }

  async function requestNotificationPermission() {
    try {
      const permission = await Notification.requestPermission();
      updateUiState();
      if (permission === 'granted') {
        API.showToast('Browser notifications enabled successfully!', 'success');
        new Notification('MediCare Notifications Active', {
          body: 'You will receive timely reminders when your medicines are due.',
        });
        checkAndNotifyPendingDoses();
      } else {
        API.showToast('Notifications were not granted.', 'warning');
      }
    } catch (e) {
      console.error('Notification error:', e);
    }
  }

  if (topBtn) topBtn.addEventListener('click', requestNotificationPermission);
  if (bannerAllowBtn) bannerAllowBtn.addEventListener('click', requestNotificationPermission);
  if (bannerDismissBtn) {
    bannerDismissBtn.addEventListener('click', () => {
      sessionStorage.setItem('medicare_dismiss_notif_banner', 'true');
      if (banner) banner.style.display = 'none';
    });
  }

  updateUiState();

  // Periodically check doses every 60 seconds
  setInterval(checkAndNotifyPendingDoses, 60000);
}

let cachedTodayDoses = [];

function checkAndNotifyPendingDoses() {
  if (!('Notification' in window) || Notification.permission !== 'granted') return;

  const now = new Date();
  cachedTodayDoses.forEach(dose => {
    if (dose.status === 'PENDING' || dose.status === 'SNOOZED') {
      const schedTime = new Date(dose.status === 'SNOOZED' && dose.snoozeUntil ? dose.snoozeUntil : dose.scheduledDateTime);
      const diffMins = (schedTime.getTime() - now.getTime()) / (1000 * 60);

      // Trigger if due within 10 minutes or overdue by up to 2 hours
      if (diffMins <= 10 && diffMins >= -120 && !notifiedDoseIds.has(dose.id)) {
        notifiedDoseIds.add(dose.id);
        const foodStr = dose.medicine.foodTiming ? ` (${dose.medicine.foodTiming.replace('_', ' ')})` : '';
        const title = `💊 MediCare: Time for ${dose.medicine.name}`;
        const body = `Dosage: ${dose.medicine.dosage}${foodStr}. Scheduled for ${schedTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}.`;

        const n = new Notification(title, {
          body: body,
          tag: 'medicare-dose-' + dose.id,
          requireInteraction: true
        });

        n.onclick = () => {
          window.focus();
          n.close();
        };
      }
    }
  });
}

async function loadDashboardData() {
  const container = document.getElementById('dashboard-content');
  if (!container) return;

  try {
    const summary = await API.request('/api/reports/dashboard');

    // Update Greeting
    const greetingEl = document.getElementById('dashboard-greeting');
    if (greetingEl) {
      const hour = new Date().getHours();
      let timeGreeting = 'Good Morning';
      if (hour >= 12 && hour < 17) timeGreeting = 'Good Afternoon';
      else if (hour >= 17) timeGreeting = 'Good Evening';
      greetingEl.textContent = `${timeGreeting}, ${summary.userName || 'Patient'}!`;
    }

    // Update Stats
    document.getElementById('stat-taken').textContent = summary.takenCount || 0;
    document.getElementById('stat-skipped').textContent = summary.skippedCount || 0;
    document.getElementById('stat-pending').textContent = summary.pendingCount || 0;
    document.getElementById('stat-adherence').textContent = `${summary.adherenceRate || 100}%`;

    // Render Today's Doses
    renderTodayDoses(summary.todayDoses || []);

    // Render Next Reminder
    renderNextReminder(summary.nextDose);

    // Render Upcoming Appointments
    renderUpcomingAppointments(summary.upcomingAppointments || []);

    // Render Low Stock Warnings
    renderLowStockAlerts(summary.lowStockMedicines || []);

    // Load active medicines list on dashboard
    loadDashboardMedicines();
  } catch (err) {
    console.error('Failed to load dashboard metrics:', err);
    API.showToast('Could not load dashboard data from server.', 'error');
  }
}

function renderTodayDoses(doses) {
  cachedTodayDoses = doses || [];
  checkAndNotifyPendingDoses();
  const listEl = document.getElementById('today-doses-list');
  if (!listEl) return;

  if (doses.length === 0) {
    listEl.innerHTML = `
      <div class="empty-state">
        <div class="empty-state-icon">💊</div>
        <h4 class="empty-state-title">No scheduled doses for today</h4>
        <p class="empty-state-text">You have no medications scheduled for today. Add a new medicine or schedule to start reminders.</p>
        <a href="medicines.html" class="btn btn-primary btn-sm">+ Add Medicine</a>
      </div>
    `;
    return;
  }

  listEl.innerHTML = doses.map(dose => {
    const timeStr = new Date(dose.scheduledDateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    let statusBadge = '<span class="badge badge-warning">Pending</span>';
    let actionButtons = `
      <button class="btn btn-success btn-sm" onclick="handleDoseAction(${dose.id}, 'taken')">✓ Taken</button>
      <button class="btn btn-secondary btn-sm" onclick="handleDoseAction(${dose.id}, 'skipped')">✕ Skip</button>
      <button class="btn btn-outline btn-sm" onclick="handleSnoozeModal(${dose.id})">⏰ Snooze</button>
      <button class="btn btn-outline btn-sm" onclick="openEditDoseModal(${dose.id})" title="Edit scheduled dose time or status">✏️ Edit</button>
      <button class="btn btn-danger btn-sm" onclick="confirmDeleteDose(${dose.id}, '${dose.medicine.name.replace(/'/g, "\\'")}')" title="Delete scheduled dose">🗑️</button>
    `;

    if (dose.status === 'TAKEN') {
      statusBadge = '<span class="badge badge-success">✓ Taken</span>';
      actionButtons = `
        <span style="font-size: 0.85rem; color: var(--success); font-weight: 600;">Recorded at ${new Date(dose.actionTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
        <button class="btn btn-outline btn-sm" onclick="openEditDoseModal(${dose.id})" title="Edit dose">✏️</button>
        <button class="btn btn-danger btn-sm" onclick="confirmDeleteDose(${dose.id}, '${dose.medicine.name.replace(/'/g, "\\'")}')" title="Delete dose">🗑️</button>
      `;
    } else if (dose.status === 'SKIPPED') {
      statusBadge = '<span class="badge badge-danger">✕ Skipped</span>';
      actionButtons = `
        <span style="font-size: 0.85rem; color: var(--danger); font-weight: 600;">Skipped</span>
        <button class="btn btn-outline btn-sm" onclick="openEditDoseModal(${dose.id})" title="Edit dose">✏️</button>
        <button class="btn btn-danger btn-sm" onclick="confirmDeleteDose(${dose.id}, '${dose.medicine.name.replace(/'/g, "\\'")}')" title="Delete dose">🗑️</button>
      `;
    } else if (dose.status === 'SNOOZED') {
      statusBadge = '<span class="badge badge-primary">⏰ Snoozed</span>';
      actionButtons = `
        <button class="btn btn-success btn-sm" onclick="handleDoseAction(${dose.id}, 'taken')">✓ Taken</button>
        <button class="btn btn-secondary btn-sm" onclick="handleDoseAction(${dose.id}, 'skipped')">✕ Skip</button>
        <button class="btn btn-outline btn-sm" onclick="openEditDoseModal(${dose.id})" title="Edit dose">✏️ Edit</button>
        <button class="btn btn-danger btn-sm" onclick="confirmDeleteDose(${dose.id}, '${dose.medicine.name.replace(/'/g, "\\'")}')" title="Delete dose">🗑️</button>
      `;
    }

    const color = dose.medicine.colorTag || 'var(--primary)';

    return `
      <div class="card" style="margin-bottom: 0.75rem; border-left: 5px solid ${color}; padding: 1rem 1.25rem;">
        <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 0.75rem;">
          <div style="display: flex; align-items: center; gap: 1rem;">
            <div style="width: 44px; height: 44px; border-radius: 10px; background-color: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; font-size: 1.2rem;">
              💊
            </div>
            <div>
              <div style="font-weight: 700; font-size: 1.05rem;">${dose.medicine.name} <span style="font-size: 0.85rem; color: var(--text-muted); font-weight: 500;">(${dose.medicine.dosage})</span></div>
              <div style="font-size: 0.85rem; color: var(--text-muted); display: flex; gap: 0.75rem; align-items: center;">
                <span>⏰ ${timeStr}</span>
                <span>•</span>
                <span>🍽️ ${dose.medicine.foodTiming ? dose.medicine.foodTiming.replace('_', ' ') : 'General'}</span>
                <span>•</span>
                ${statusBadge}
              </div>
            </div>
          </div>
          <div style="display: flex; gap: 0.5rem; align-items: center;">
            ${actionButtons}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function renderNextReminder(nextDose) {
  const card = document.getElementById('next-reminder-card');
  if (!card) return;

  if (!nextDose) {
    card.innerHTML = `
      <div style="text-align: center; color: var(--text-muted); padding: 1rem 0;">
        <p>No more pending doses remaining today!</p>
      </div>
    `;
    return;
  }

  const doseTime = new Date(nextDose.scheduledDateTime);
  const timeFormatted = doseTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  card.innerHTML = `
    <div style="display: flex; align-items: center; gap: 1rem;">
      <div style="width: 50px; height: 50px; border-radius: 12px; background: linear-gradient(135deg, var(--primary) 0%, var(--teal) 100%); color: white; display: flex; align-items: center; justify-content: center; font-size: 1.4rem;">
        ⏰
      </div>
      <div style="flex: 1;">
        <div style="font-size: 0.8rem; text-transform: uppercase; font-weight: 700; color: var(--primary);">Upcoming Dose</div>
        <div style="font-size: 1.15rem; font-weight: 700;">${nextDose.medicine.name} (${nextDose.medicine.dosage})</div>
        <div style="font-size: 0.85rem; color: var(--text-muted);">Scheduled for <strong>${timeFormatted}</strong> • ${nextDose.medicine.instructions || 'Follow doctor directions'}</div>
      </div>
      <div>
        <button class="btn btn-success btn-sm" onclick="handleDoseAction(${nextDose.id}, 'taken')">Take Now</button>
      </div>
    </div>
  `;
}

function renderUpcomingAppointments(appointments) {
  const container = document.getElementById('upcoming-appointments-list');
  if (!container) return;

  if (appointments.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; color: var(--text-muted); padding: 1.5rem 0;">
        <p>No upcoming doctor appointments.</p>
        <a href="appointments.html" class="btn btn-outline btn-sm" style="margin-top: 0.5rem;">Book Appointment</a>
      </div>
    `;
    return;
  }

  container.innerHTML = appointments.slice(0, 3).map(app => {
    const dt = new Date(app.appointmentDateTime);
    const dateFormatted = dt.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
    const timeFormatted = dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    return `
      <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.85rem 0; border-bottom: 1px solid var(--border);">
        <div>
          <div style="font-weight: 600; font-size: 0.95rem;">${app.doctor.name}</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">${app.doctor.specialty} • ${app.appointmentType.replace('_', ' ')}</div>
          <div style="font-size: 0.8rem; color: var(--primary); font-weight: 500;">📅 ${dateFormatted} at ${timeFormatted}</div>
        </div>
        <a href="appointments.html" class="btn btn-secondary btn-sm">Manage</a>
      </div>
    `;
  }).join('');
}

function renderLowStockAlerts(medicines) {
  const container = document.getElementById('low-stock-list');
  if (!container) return;

  if (medicines.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; color: var(--text-muted); padding: 1.5rem 0;">
        <span style="font-size: 1.5rem;">🛡️</span>
        <p style="margin-top: 0.25rem;">All medicine inventories are sufficiently stocked.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = medicines.map(med => `
    <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 0; border-bottom: 1px solid var(--border);">
      <div>
        <div style="font-weight: 600; font-size: 0.95rem; color: var(--danger);">${med.name}</div>
        <div style="font-size: 0.8rem; color: var(--text-muted);">Current: <strong>${med.totalStock}</strong> left (Threshold: ${med.lowStockThreshold})</div>
      </div>
      <a href="medicines.html" class="btn btn-outline btn-sm">+ Refill</a>
    </div>
  `).join('');
}

async function handleDoseAction(doseId, action) {
  try {
    if (action === 'taken') {
      await API.request(`/api/doses/${doseId}/taken`, { method: 'POST' });
      API.showToast('Dose recorded as Taken. Inventory updated.', 'success');
    } else if (action === 'skipped') {
      await API.request(`/api/doses/${doseId}/skipped`, { method: 'POST' });
      API.showToast('Dose recorded as Skipped.', 'info');
    }
    loadDashboardData();
  } catch (err) {
    API.showToast(err.message || 'Failed to update dose status', 'error');
  }
}

function handleSnoozeModal(doseId) {
  const modal = document.getElementById('snooze-modal');
  if (!modal) return;
  modal.setAttribute('data-dose-id', doseId);
  modal.classList.add('active');
}

function openEditDoseModal(doseId) {
  const dose = cachedTodayDoses.find(d => d.id === doseId);
  if (!dose) {
    API.showToast('Could not find dose details', 'error');
    return;
  }

  const modal = document.getElementById('edit-dose-modal');
  if (!modal) return;

  document.getElementById('edit-dose-id').value = dose.id;
  document.getElementById('edit-dose-med-name').textContent = `${dose.medicine.name} (${dose.medicine.dosage}) - ${dose.medicine.form}`;

  // Format scheduledDateTime for datetime-local: YYYY-MM-DDTHH:mm
  const dt = new Date(dose.scheduledDateTime);
  const year = dt.getFullYear();
  const month = String(dt.getMonth() + 1).padStart(2, '0');
  const day = String(dt.getDate()).padStart(2, '0');
  const hours = String(dt.getHours()).padStart(2, '0');
  const minutes = String(dt.getMinutes()).padStart(2, '0');
  document.getElementById('edit-dose-datetime').value = `${year}-${month}-${day}T${hours}:${minutes}`;

  document.getElementById('edit-dose-status').value = dose.status || 'PENDING';
  document.getElementById('edit-dose-notes').value = '';

  modal.classList.add('active');
}

async function confirmDeleteDose(doseId, medicineName) {
  if (confirm(`Are you sure you want to delete this scheduled dose for "${medicineName}"?`)) {
    try {
      await API.request(`/api/doses/${doseId}`, { method: 'DELETE' });
      API.showToast('Scheduled dose removed successfully.', 'info');
      loadDashboardData();
    } catch (err) {
      API.showToast(err.message || 'Could not delete dose', 'error');
    }
  }
}

let cachedDashboardMedicines = [];

async function loadDashboardMedicines() {
  const container = document.getElementById('dashboard-medicines-list');
  if (!container) return;

  try {
    const medicines = await API.request('/api/medicines?activeOnly=true');
    cachedDashboardMedicines = medicines || [];

    if (cachedDashboardMedicines.length === 0) {
      container.innerHTML = `
        <div style="text-align: center; padding: 1.5rem 0; color: var(--text-muted);">
          <p>No active medications found in your profile.</p>
          <a href="medicines.html" class="btn btn-outline btn-sm" style="margin-top: 0.5rem;">+ Add Your First Medicine</a>
        </div>
      `;
      return;
    }

    container.innerHTML = `
      <div style="overflow-x: auto;">
        <table class="table" style="margin: 0; font-size: 0.9rem;">
          <thead>
            <tr>
              <th>Medicine</th>
              <th>Timing</th>
              <th>Current Stock</th>
              <th style="text-align: right;">Dashboard Actions</th>
            </tr>
          </thead>
          <tbody>
            ${cachedDashboardMedicines.map(m => {
              const lowStock = m.totalStock <= m.lowStockThreshold;
              const stockBadge = lowStock
                ? `<span class="badge badge-danger">⚠️ ${m.totalStock} left</span>`
                : `<span class="badge badge-success">${m.totalStock} in stock</span>`;

              return `
                <tr>
                  <td>
                    <div style="font-weight: 700; color: var(--text-main);">${m.name}</div>
                    <div style="font-size: 0.8rem; color: var(--text-muted);">${m.dosage} • ${m.form}</div>
                  </td>
                  <td>
                    <span style="font-size: 0.85rem; color: var(--text-muted);">🍽️ ${(m.foodTiming || 'GENERAL').replace('_', ' ')}</span>
                  </td>
                  <td>${stockBadge}</td>
                  <td style="text-align: right;">
                    <div style="display: inline-flex; gap: 0.4rem;">
                      <button class="btn btn-secondary btn-sm" onclick="openQuickEditMedicineModal(${m.id})" title="Edit medication dosage, instructions or stock">✏️ Edit</button>
                      <button class="btn btn-danger btn-sm" onclick="confirmDeleteDashboardMedicine(${m.id}, '${m.name.replace(/'/g, "\\'")}')" title="Delete medication">🗑️ Delete</button>
                    </div>
                  </td>
                </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      </div>
    `;
  } catch (err) {
    console.error('Failed to load dashboard medicines:', err);
    container.innerHTML = `<div style="color: var(--danger); padding: 1rem;">Failed to load medicines.</div>`;
  }
}

function openQuickEditMedicineModal(medId) {
  const med = cachedDashboardMedicines.find(m => m.id === medId);
  if (!med) return;

  const modal = document.getElementById('quick-edit-medicine-modal');
  if (!modal) return;

  document.getElementById('quick-med-id').value = med.id;
  document.getElementById('quick-med-name').value = med.name || '';
  document.getElementById('quick-med-dosage').value = med.dosage || '';
  document.getElementById('quick-med-form').value = med.form || 'TABLET';
  document.getElementById('quick-med-food').value = med.foodTiming || 'NO_RESTRICTION';
  document.getElementById('quick-med-stock').value = med.totalStock != null ? med.totalStock : 0;
  document.getElementById('quick-med-instructions').value = med.instructions || '';

  modal.classList.add('active');
}

async function confirmDeleteDashboardMedicine(medId, medName) {
  if (confirm(`Are you sure you want to delete "${medName}"? This will permanently remove its dosage schedules and dose logs.`)) {
    try {
      await API.request(`/api/medicines/${medId}`, { method: 'DELETE' });
      API.showToast(`Medicine "${medName}" deleted successfully.`, 'info');
      loadDashboardData();
    } catch (err) {
      API.showToast(err.message || 'Could not delete medication', 'error');
    }
  }
}

function setupQuickActionModals() {
  // Snooze Form
  const snoozeForm = document.getElementById('snooze-form');
  if (snoozeForm) {
    snoozeForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const modal = document.getElementById('snooze-modal');
      const doseId = modal.getAttribute('data-dose-id');
      const minutes = parseInt(document.getElementById('snooze-minutes').value, 10);

      try {
        await API.request(`/api/doses/${doseId}/snooze`, {
          method: 'POST',
          body: JSON.stringify({ minutes })
        });
        API.showToast(`Reminder snoozed for ${minutes} minutes.`, 'info');
        modal.classList.remove('active');
        loadDashboardData();
      } catch (err) {
        API.showToast(err.message || 'Could not snooze dose', 'error');
      }
    });
  }

  // Edit Dose Form
  const editDoseForm = document.getElementById('edit-dose-form');
  if (editDoseForm) {
    editDoseForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const modal = document.getElementById('edit-dose-modal');
      const doseId = document.getElementById('edit-dose-id').value;
      const scheduledDateTime = document.getElementById('edit-dose-datetime').value;
      const status = document.getElementById('edit-dose-status').value;
      const notes = document.getElementById('edit-dose-notes').value.trim();

      try {
        await API.request(`/api/doses/${doseId}`, {
          method: 'PUT',
          body: JSON.stringify({
            scheduledDateTime,
            status,
            notes
          })
        });
        API.showToast('Scheduled dose updated successfully.', 'success');
        modal.classList.remove('active');
        loadDashboardData();
      } catch (err) {
        API.showToast(err.message || 'Could not update scheduled dose', 'error');
      }
    });
  }

  // Quick Edit Medicine Form
  const quickMedForm = document.getElementById('quick-edit-medicine-form');
  if (quickMedForm) {
    quickMedForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const modal = document.getElementById('quick-edit-medicine-modal');
      const medId = document.getElementById('quick-med-id').value;
      const original = cachedDashboardMedicines.find(m => m.id == medId) || {};

      const payload = {
        name: document.getElementById('quick-med-name').value.trim(),
        dosage: document.getElementById('quick-med-dosage').value.trim(),
        form: document.getElementById('quick-med-form').value,
        foodTiming: document.getElementById('quick-med-food').value,
        totalStock: parseInt(document.getElementById('quick-med-stock').value, 10),
        instructions: document.getElementById('quick-med-instructions').value.trim(),
        frequency: original.frequency || 'DAILY',
        quantityPerDose: original.quantityPerDose || 1,
        lowStockThreshold: original.lowStockThreshold || 5,
        startDate: original.startDate || new Date().toISOString().split('T')[0],
        endDate: original.endDate || null,
        prescribingDoctor: original.prescribingDoctor || '',
        colorTag: original.colorTag || '#1977cc',
        active: original.active !== undefined ? original.active : true
      };

      try {
        await API.request(`/api/medicines/${medId}`, {
          method: 'PUT',
          body: JSON.stringify(payload)
        });
        API.showToast('Medicine details updated successfully.', 'success');
        modal.classList.remove('active');
        loadDashboardData();
      } catch (err) {
        API.showToast(err.message || 'Could not update medicine', 'error');
      }
    });
  }

  // Close modals on click of cancel/close buttons
  document.querySelectorAll('.modal-close, .modal-cancel').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
    });
  });
}
