/**
 * MEDICARE - MEDICINE SCHEDULING CONTROLLER
 * Daily dose times, multi-reminder configuration, weekday selection, and schedule timelines.
 */

let userSchedules = [];
let userMedicines = [];
let todayDoses = [];

document.addEventListener('DOMContentLoaded', () => {
  loadSchedulesAndMedicines();
  setupScheduleModals();
});

async function loadSchedulesAndMedicines() {
  try {
    const [schedules, medicines, doses] = await Promise.all([
      API.request('/api/schedules'),
      API.request('/api/medicines?activeOnly=true'),
      API.request('/api/doses/today')
    ]);

    userSchedules = schedules;
    userMedicines = medicines;
    todayDoses = doses || [];

    populateMedicineDropdown(medicines);
    renderScheduleTimeline(schedules, todayDoses);
  } catch (err) {
    console.error('Failed to load schedules:', err);
    API.showToast('Could not load schedules from server.', 'error');
  }
}

function populateMedicineDropdown(medicines) {
  const select = document.getElementById('schedule-medicine-id');
  if (!select) return;

  if (medicines.length === 0) {
    select.innerHTML = '<option value="">No active medicines found. Please add a medicine first.</option>';
    return;
  }

  select.innerHTML = medicines.map(m => `
    <option value="${m.id}">${m.name} (${m.dosage}) - ${m.form}</option>
  `).join('');
}

function renderScheduleTimeline(schedules, doses) {
  const container = document.getElementById('schedules-timeline');
  const emptyState = document.getElementById('schedules-empty-state');
  if (!container) return;

  if (schedules.length === 0) {
    container.innerHTML = '';
    if (emptyState) emptyState.style.display = 'block';
    return;
  }

  if (emptyState) emptyState.style.display = 'none';

  // Sort by time of day
  const sorted = [...schedules].sort((a, b) => a.timeOfDay.localeCompare(b.timeOfDay));

  container.innerHTML = sorted.map(s => {
    const [h, m] = s.timeOfDay.split(':');
    const timeObj = new Date();
    timeObj.setHours(parseInt(h, 10), parseInt(m, 10));
    const timeFormatted = timeObj.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const activeToggle = s.reminderActive
      ? `<button class="btn btn-sm btn-outline" onclick="toggleScheduleStatus(${s.id})" title="Pause reminders">🔔 Enabled</button>`
      : `<button class="btn btn-sm btn-secondary" onclick="toggleScheduleStatus(${s.id})" title="Enable reminders">🔕 Paused</button>`;

    const daysText = s.daysOfWeek === 'ALL' ? 'Every Day' : s.daysOfWeek.split(',').join(', ');

    // Match today's dose for this schedule or medicine
    const matchingDose = (doses || []).find(d => (d.schedule && d.schedule.id === s.id) || (d.medicine && d.medicine.id === s.medicine.id));

    let doseStatusSection = '';
    if (matchingDose) {
      if (matchingDose.status === 'TAKEN') {
        doseStatusSection = `
          <div style="display: flex; align-items: center; gap: 0.5rem; background: rgba(16, 185, 129, 0.1); padding: 0.35rem 0.75rem; border-radius: 8px; border: 1px solid rgba(16, 185, 129, 0.3);">
            <span class="badge badge-success" style="font-size: 0.85rem;">✓ Taken Today</span>
            <button class="btn btn-outline btn-sm" style="font-size: 0.75rem; padding: 0.2rem 0.5rem;" onclick="markScheduleDoseSkipped(${matchingDose.id})" title="Change status to missed">Change to Missed</button>
          </div>
        `;
      } else if (matchingDose.status === 'SKIPPED') {
        doseStatusSection = `
          <div style="display: flex; align-items: center; gap: 0.5rem; background: rgba(239, 68, 68, 0.1); padding: 0.35rem 0.75rem; border-radius: 8px; border: 1px solid rgba(239, 68, 68, 0.3);">
            <span class="badge badge-danger" style="font-size: 0.85rem;">✕ Missed Today</span>
            <button class="btn btn-success btn-sm" style="font-size: 0.75rem; padding: 0.2rem 0.5rem;" onclick="markScheduleDoseTaken(${matchingDose.id})" title="Mark as taken">Mark Taken</button>
          </div>
        `;
      } else {
        doseStatusSection = `
          <div style="display: flex; align-items: center; gap: 0.5rem; background: var(--bg-card-subtle); padding: 0.35rem 0.75rem; border-radius: 8px; border: 1px solid var(--border);">
            <span class="badge badge-warning" style="font-size: 0.85rem;">⏰ Pending</span>
            <button class="btn btn-success btn-sm" onclick="markScheduleDoseTaken(${matchingDose.id})">✓ Taken</button>
            <button class="btn btn-danger btn-sm" onclick="markScheduleDoseSkipped(${matchingDose.id})">✕ Missed</button>
          </div>
        `;
      }
    } else {
      doseStatusSection = `
        <div style="display: flex; align-items: center; gap: 0.5rem;">
          <button class="btn btn-success btn-sm" onclick="quickLogScheduleDose(${s.medicine.id}, 'TAKEN')">✓ Taken</button>
          <button class="btn btn-danger btn-sm" onclick="quickLogScheduleDose(${s.medicine.id}, 'SKIPPED')">✕ Missed</button>
        </div>
      `;
    }

    return `
      <div class="card" style="margin-bottom: 1rem; border-left: 5px solid ${s.medicine.colorTag || '#1977cc'}; padding: 1.1rem 1.35rem;">
        <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem;">
          <div style="display: flex; align-items: center; gap: 1.25rem;">
            <div style="background-color: var(--primary-light); color: var(--primary); padding: 0.75rem 1rem; border-radius: 12px; font-weight: 800; font-size: 1.2rem; text-align: center; min-width: 95px;">
              ${timeFormatted}
            </div>
            <div>
              <div style="font-size: 1.15rem; font-weight: 700;">${s.medicine.name} <span style="font-size: 0.9rem; font-weight: 500; color: var(--text-muted);">(${s.medicine.dosage})</span></div>
              <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.2rem;">
                <span>📅 ${daysText}</span>
                <span style="margin: 0 0.5rem;">•</span>
                <span>🍽️ ${s.medicine.foodTiming ? s.medicine.foodTiming.replace('_', ' ') : 'General'}</span>
              </div>
            </div>
          </div>
          <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 0.75rem;">
            ${doseStatusSection}
            <div style="display: flex; gap: 0.4rem;">
              ${activeToggle}
              <button class="btn btn-danger btn-sm" onclick="confirmDeleteSchedule(${s.id})" title="Delete reminder time">🗑️</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function setupScheduleModals() {
  const addForm = document.getElementById('schedule-form');
  if (addForm) {
    addForm.addEventListener('submit', async (e) => {
      e.preventDefault();

      const medicineId = document.getElementById('schedule-medicine-id').value;
      const timeOfDay = document.getElementById('schedule-time').value;

      // Collect selected weekdays
      const checkboxes = document.querySelectorAll('input[name="schedule-days"]:checked');
      let daysOfWeek = 'ALL';
      if (checkboxes.length > 0 && checkboxes.length < 7) {
        daysOfWeek = Array.from(checkboxes).map(c => c.value).join(',');
      }

      if (!medicineId) {
        API.showToast('Please select a medication.', 'error');
        return;
      }

      if (!timeOfDay) {
        API.showToast('Please set a time of day for the dose.', 'error');
        return;
      }

      try {
        await API.request('/api/schedules', {
          method: 'POST',
          body: JSON.stringify({
            medicineId: parseInt(medicineId, 10),
            timeOfDay: timeOfDay + ':00',
            daysOfWeek,
            intervalDays: 1,
            reminderActive: true
          })
        });

        API.showToast('Reminder schedule created successfully!', 'success');
        document.getElementById('schedule-modal').classList.remove('active');
        addForm.reset();
        loadSchedulesAndMedicines();
      } catch (err) {
        API.showToast(err.message || 'Failed to create schedule', 'error');
      }
    });
  }

  const openBtn = document.getElementById('open-add-schedule-btn');
  if (openBtn) {
    openBtn.addEventListener('click', () => {
      if (userMedicines.length === 0) {
        API.showToast('Please create at least one medication before adding a reminder schedule.', 'warning');
        return;
      }
      document.getElementById('schedule-modal').classList.add('active');
    });
  }
}

async function toggleScheduleStatus(scheduleId) {
  try {
    await API.request(`/api/schedules/${scheduleId}/toggle`, { method: 'PUT' });
    API.showToast('Schedule reminder status updated.', 'info');
    loadSchedulesAndMedicines();
  } catch (err) {
    API.showToast(err.message || 'Failed to toggle schedule', 'error');
  }
}

async function confirmDeleteSchedule(scheduleId) {
  if (confirm('Are you sure you want to delete this reminder time?')) {
    try {
      await API.request(`/api/schedules/${scheduleId}`, { method: 'DELETE' });
      API.showToast('Schedule deleted.', 'info');
      loadSchedulesAndMedicines();
    } catch (err) {
      API.showToast(err.message || 'Failed to delete schedule', 'error');
    }
  }
}

async function markScheduleDoseTaken(doseId) {
  try {
    await API.request(`/api/doses/${doseId}/taken`, { method: 'POST' });
    API.showToast('Dose recorded as Taken! Medicine inventory updated.', 'success');
    loadSchedulesAndMedicines();
  } catch (err) {
    API.showToast(err.message || 'Could not record dose as taken', 'error');
  }
}

async function markScheduleDoseSkipped(doseId) {
  try {
    await API.request(`/api/doses/${doseId}/skipped`, { method: 'POST' });
    API.showToast('Dose recorded as Missed / Skipped.', 'info');
    loadSchedulesAndMedicines();
  } catch (err) {
    API.showToast(err.message || 'Could not record dose as skipped', 'error');
  }
}

async function quickLogScheduleDose(medicineId, action) {
  try {
    const freshDoses = await API.request('/api/doses/today');
    const dose = freshDoses.find(d => d.medicine && d.medicine.id === medicineId);
    if (dose) {
      if (action === 'TAKEN') {
        await markScheduleDoseTaken(dose.id);
      } else {
        await markScheduleDoseSkipped(dose.id);
      }
    } else {
      API.showToast('No pending dose scheduled for today for this medicine.', 'info');
    }
  } catch (err) {
    API.showToast(err.message || 'Could not perform dose check-in', 'error');
  }
}
