/**
 * MEDICARE - DOCTOR APPOINTMENTS CONTROLLER
 * Personal appointment schedule management:
 * Schedule appointment dates, times, specialist names, clinics, and consultation notes.
 * Edit, reschedule, cancel, or delete appointments.
 */

let userAppointments = [];

document.addEventListener('DOMContentLoaded', () => {
  loadUserAppointments();
  setupAppointmentForms();
  setupFilterAndSearch();
});

async function loadUserAppointments() {
  const upcomingContainer = document.getElementById('upcoming-appointments-list');
  const pastContainer = document.getElementById('past-appointments-list');

  try {
    const appointments = await API.request('/api/appointments');
    userAppointments = appointments || [];
    filterAndRenderAppointments();
  } catch (err) {
    console.error('Failed to load user appointments:', err);
    if (upcomingContainer) {
      upcomingContainer.innerHTML = '<div style="color: var(--danger); padding: 1.5rem 0;">Could not load appointment schedules from server.</div>';
    }
    API.showToast('Could not load appointment schedules.', 'error');
  }
}

function setupFilterAndSearch() {
  const searchInput = document.getElementById('appointment-search-input');
  const filterSelect = document.getElementById('appointment-status-filter');

  if (searchInput) searchInput.addEventListener('input', filterAndRenderAppointments);
  if (filterSelect) filterSelect.addEventListener('change', filterAndRenderAppointments);

  const openBtn = document.getElementById('open-schedule-modal-btn');
  if (openBtn) {
    openBtn.addEventListener('click', () => openScheduleModal());
  }
}

function filterAndRenderAppointments() {
  const searchInput = document.getElementById('appointment-search-input');
  const filterSelect = document.getElementById('appointment-status-filter');

  const query = searchInput ? searchInput.value.toLowerCase().trim() : '';
  const filterVal = filterSelect ? filterSelect.value : 'UPCOMING';

  const now = new Date();

  // Search filter
  let list = userAppointments.filter(a => {
    if (!query) return true;
    const doc = (a.doctor && a.doctor.name) ? a.doctor.name.toLowerCase() : '';
    const clinic = (a.doctor && a.doctor.clinicName) ? a.doctor.clinicName.toLowerCase() : '';
    const reason = a.reason ? a.reason.toLowerCase() : '';
    const notes = a.notes ? a.notes.toLowerCase() : '';
    return doc.includes(query) || clinic.includes(query) || reason.includes(query) || notes.includes(query);
  });

  const upcomingContainer = document.getElementById('upcoming-appointments-list');
  const pastContainer = document.getElementById('past-appointments-list');
  const emptyUpcoming = document.getElementById('empty-upcoming-appointments');

  const upcomingList = list.filter(a => new Date(a.appointmentDateTime) >= now && a.status !== 'CANCELLED');
  const pastList = list.filter(a => new Date(a.appointmentDateTime) < now || a.status === 'CANCELLED');

  // Render Upcoming
  if (upcomingContainer) {
    if (filterVal === 'PAST' || filterVal === 'CANCELLED') {
      upcomingContainer.parentElement.style.display = 'none';
    } else {
      upcomingContainer.parentElement.style.display = 'block';
      if (upcomingList.length === 0) {
        upcomingContainer.innerHTML = '';
        if (emptyUpcoming) emptyUpcoming.style.display = 'block';
      } else {
        if (emptyUpcoming) emptyUpcoming.style.display = 'none';
        upcomingContainer.innerHTML = upcomingList.map(a => renderAppointmentCard(a, true)).join('');
      }
    }
  }

  // Render Past
  if (pastContainer) {
    let toShow = pastList;
    if (filterVal === 'CANCELLED') {
      toShow = userAppointments.filter(a => a.status === 'CANCELLED');
    }

    if (filterVal === 'UPCOMING') {
      pastContainer.parentElement.style.display = 'none';
    } else {
      pastContainer.parentElement.style.display = 'block';
      if (toShow.length === 0) {
        pastContainer.innerHTML = '<div style="color: var(--text-muted); padding: 1rem 0;">No past appointment records found.</div>';
      } else {
        pastContainer.innerHTML = toShow.map(a => renderAppointmentCard(a, false)).join('');
      }
    }
  }
}

function renderAppointmentCard(app, isUpcoming) {
  const dt = new Date(app.appointmentDateTime);
  const dateFormatted = dt.toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
  const timeFormatted = dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  let badge = '<span class="badge badge-primary">Scheduled</span>';
  if (app.status === 'CANCELLED') badge = '<span class="badge badge-danger">Cancelled</span>';
  else if (app.status === 'RESCHEDULED') badge = '<span class="badge badge-warning">Rescheduled</span>';
  else if (app.status === 'COMPLETED') badge = '<span class="badge badge-success">Completed</span>';

  const typeLabel = (app.appointmentType || 'IN_PERSON').replace(/_/g, ' ');

  const docName = app.doctor ? app.doctor.name : 'Healthcare Provider';
  const clinicName = app.doctor ? app.doctor.clinicName : 'Clinic';

  let actionButtons = `
    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 1rem; padding-top: 0.75rem; border-top: 1px solid var(--border);">
      <button class="btn btn-outline btn-sm" onclick="openEditAppointmentModal(${app.id})">✏️ Edit Details</button>
      <button class="btn btn-secondary btn-sm" onclick="openRescheduleModal(${app.id})">🔄 Reschedule</button>
      ${isUpcoming && app.status !== 'CANCELLED' ? `<button class="btn btn-danger btn-sm" onclick="confirmCancelAppointment(${app.id})">✕ Cancel</button>` : ''}
      <button class="btn btn-danger btn-sm" onclick="confirmDeleteAppointment(${app.id})" title="Delete appointment record">🗑️ Delete</button>
    </div>
  `;

  return `
    <div class="card" style="margin-bottom: 1rem; border-left: 5px solid var(--primary); padding: 1.25rem 1.5rem;">
      <div style="display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 1rem;">
        <div style="display: flex; align-items: flex-start; gap: 1rem;">
          <div style="width: 52px; height: 52px; border-radius: 12px; background-color: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; font-size: 1.5rem; flex-shrink: 0;">
            👨‍⚕️
          </div>
          <div>
            <div style="font-size: 1.15rem; font-weight: 700; color: var(--text-main);">${docName}</div>
            <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.15rem;">
              🏥 <strong>${clinicName}</strong> • <span class="badge badge-outline" style="font-size: 0.75rem;">${typeLabel}</span>
            </div>
            <div style="font-size: 0.95rem; font-weight: 700; color: var(--primary); margin-top: 0.5rem; display: flex; align-items: center; gap: 0.5rem;">
              <span>📅 ${dateFormatted}</span>
              <span>•</span>
              <span>⏰ ${timeFormatted}</span>
            </div>
            <div style="font-size: 0.9rem; color: var(--text-main); margin-top: 0.4rem;">
              <strong>Purpose:</strong> ${app.reason}
            </div>
            ${app.notes ? `<div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.25rem; font-style: italic;">📝 Note: ${app.notes}</div>` : ''}
          </div>
        </div>
        <div>
          ${badge}
        </div>
      </div>
      ${actionButtons}
    </div>
  `;
}

function openScheduleModal() {
  const modal = document.getElementById('appointment-modal');
  const form = document.getElementById('appointment-form');
  const title = document.getElementById('appointment-modal-title');
  const submitBtn = document.getElementById('appointment-modal-submit-btn');

  if (!modal || !form) return;

  form.reset();
  document.getElementById('appointment-id').value = '';
  title.textContent = '📅 Schedule Doctor Appointment';
  submitBtn.textContent = 'Save Appointment Schedule';

  // Default to tomorrow 10:00 AM
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const yyyy = tomorrow.getFullYear();
  const mm = String(tomorrow.getMonth() + 1).padStart(2, '0');
  const dd = String(tomorrow.getDate()).padStart(2, '0');
  document.getElementById('app-date').value = `${yyyy}-${mm}-${dd}`;
  document.getElementById('app-time').value = '10:00';

  modal.classList.add('active');
}

function openEditAppointmentModal(appId) {
  const app = userAppointments.find(a => a.id === appId);
  if (!app) return;

  const modal = document.getElementById('appointment-modal');
  const title = document.getElementById('appointment-modal-title');
  const submitBtn = document.getElementById('appointment-modal-submit-btn');

  if (!modal) return;

  document.getElementById('appointment-id').value = app.id;
  title.textContent = '✏️ Edit Appointment Details';
  submitBtn.textContent = 'Update Appointment';

  document.getElementById('app-doctor-name').value = app.doctor ? app.doctor.name : '';
  document.getElementById('app-clinic-name').value = app.doctor ? app.doctor.clinicName : '';

  const dt = new Date(app.appointmentDateTime);
  const yyyy = dt.getFullYear();
  const mm = String(dt.getMonth() + 1).padStart(2, '0');
  const dd = String(dt.getDate()).padStart(2, '0');
  const hh = String(dt.getHours()).padStart(2, '0');
  const min = String(dt.getMinutes()).padStart(2, '0');

  document.getElementById('app-date').value = `${yyyy}-${mm}-${dd}`;
  document.getElementById('app-time').value = `${hh}:${min}`;
  document.getElementById('app-type').value = app.appointmentType || 'IN_PERSON';
  document.getElementById('app-reason').value = app.reason || '';
  document.getElementById('app-notes').value = app.notes || '';

  modal.classList.add('active');
}

function openRescheduleModal(appId) {
  const app = userAppointments.find(a => a.id === appId);
  if (!app) return;

  const modal = document.getElementById('reschedule-modal');
  if (!modal) return;

  document.getElementById('reschedule-app-id').value = app.id;

  const dt = new Date(app.appointmentDateTime);
  const yyyy = dt.getFullYear();
  const mm = String(dt.getMonth() + 1).padStart(2, '0');
  const dd = String(dt.getDate()).padStart(2, '0');
  const hh = String(dt.getHours()).padStart(2, '0');
  const min = String(dt.getMinutes()).padStart(2, '0');

  document.getElementById('reschedule-date').value = `${yyyy}-${mm}-${dd}`;
  document.getElementById('reschedule-time').value = `${hh}:${min}`;

  modal.classList.add('active');
}

function setupAppointmentForms() {
  // Appointment Create/Edit Form
  const appForm = document.getElementById('appointment-form');
  if (appForm) {
    appForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const appId = document.getElementById('appointment-id').value;
      const doctorName = document.getElementById('app-doctor-name').value.trim();
      const clinicName = document.getElementById('app-clinic-name').value.trim();
      const dateVal = document.getElementById('app-date').value;
      const timeVal = document.getElementById('app-time').value;
      const appointmentType = document.getElementById('app-type').value;
      const reason = document.getElementById('app-reason').value.trim();
      const notes = document.getElementById('app-notes').value.trim();

      if (!doctorName || !dateVal || !timeVal || !reason) {
        API.showToast('Please fill all required appointment fields.', 'error');
        return;
      }

      const appointmentDateTime = `${dateVal}T${timeVal.length === 5 ? timeVal + ':00' : timeVal}`;

      const payload = {
        doctorName,
        clinicName,
        appointmentDateTime,
        appointmentType,
        reason,
        notes
      };

      try {
        if (appId) {
          await API.request(`/api/appointments/${appId}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
          });
          API.showToast('Appointment details updated successfully!', 'success');
        } else {
          await API.request('/api/appointments', {
            method: 'POST',
            body: JSON.stringify(payload)
          });
          API.showToast('Appointment scheduled successfully!', 'success');
        }

        document.getElementById('appointment-modal').classList.remove('active');
        loadUserAppointments();
      } catch (err) {
        API.showToast(err.message || 'Failed to save appointment', 'error');
      }
    });
  }

  // Reschedule Form
  const reschedForm = document.getElementById('reschedule-form');
  if (reschedForm) {
    reschedForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const appId = document.getElementById('reschedule-app-id').value;
      const dateVal = document.getElementById('reschedule-date').value;
      const timeVal = document.getElementById('reschedule-time').value;

      if (!dateVal || !timeVal) {
        API.showToast('Please select both date and time to reschedule.', 'error');
        return;
      }

      const newDateTime = `${dateVal}T${timeVal.length === 5 ? timeVal + ':00' : timeVal}`;

      try {
        await API.request(`/api/appointments/${appId}/reschedule?dateTime=${encodeURIComponent(newDateTime)}`, {
          method: 'PUT'
        });
        API.showToast('Appointment rescheduled successfully!', 'success');
        document.getElementById('reschedule-modal').classList.remove('active');
        loadUserAppointments();
      } catch (err) {
        API.showToast(err.message || 'Could not reschedule appointment', 'error');
      }
    });
  }

  // Cancel & Close Modal listeners
  document.querySelectorAll('.modal-close, .modal-cancel').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
    });
  });
}

async function confirmCancelAppointment(appId) {
  if (confirm('Are you sure you want to cancel this scheduled appointment?')) {
    try {
      await API.request(`/api/appointments/${appId}/cancel`, { method: 'POST' });
      API.showToast('Appointment cancelled.', 'info');
      loadUserAppointments();
    } catch (err) {
      API.showToast(err.message || 'Could not cancel appointment', 'error');
    }
  }
}

async function confirmDeleteAppointment(appId) {
  if (confirm('Are you sure you want to permanently delete this appointment record?')) {
    try {
      await API.request(`/api/appointments/${appId}`, { method: 'DELETE' });
      API.showToast('Appointment record deleted successfully.', 'info');
      loadUserAppointments();
    } catch (err) {
      API.showToast(err.message || 'Could not delete appointment', 'error');
    }
  }
}
