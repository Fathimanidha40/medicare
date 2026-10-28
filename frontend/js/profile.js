/**
 * MEDICARE - USER PROFILE & SECURITY CONTROLLER
 * Personal profile details, emergency contact management, and password change handler.
 */

document.addEventListener('DOMContentLoaded', () => {
  loadUserProfile();
  setupProfileForms();
  loadCaregiverNetwork();
  setupCaregiverEvents();
});

async function loadUserProfile() {
  try {
    const profile = await API.request('/api/profile');

    document.getElementById('profile-name').value = profile.name || '';
    document.getElementById('profile-email').value = profile.email || '';
    document.getElementById('profile-phone').value = profile.phone || '';
    document.getElementById('profile-dob').value = profile.dateOfBirth || '';
    document.getElementById('profile-emergency').value = profile.emergencyContact || '';
    if (document.getElementById('profile-timezone')) {
      document.getElementById('profile-timezone').value = profile.timezone || 'UTC';
    }

    // Update avatar and header text
    const dispName = document.getElementById('profile-display-name');
    const dispEmail = document.getElementById('profile-display-email');
    const dispAvatar = document.getElementById('profile-display-avatar');

    if (dispName) dispName.textContent = profile.name;
    if (dispEmail) dispEmail.textContent = profile.email;
    if (dispAvatar) {
      const initials = (profile.name || 'U').split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
      dispAvatar.textContent = initials;
    }
  } catch (err) {
    console.error('Failed to load profile:', err);
    API.showToast('Could not load profile information.', 'error');
  }
}

function setupProfileForms() {
  const profileForm = document.getElementById('profile-form');
  if (profileForm) {
    profileForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const submitBtn = profileForm.querySelector('button[type="submit"]');
      const origText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<span class="spinner"></span> Saving...';

      const payload = {
        name: document.getElementById('profile-name').value.trim(),
        phone: document.getElementById('profile-phone').value.trim(),
        dateOfBirth: document.getElementById('profile-dob').value || null,
        emergencyContact: document.getElementById('profile-emergency').value.trim(),
        timezone: document.getElementById('profile-timezone') ? document.getElementById('profile-timezone').value : 'UTC'
      };

      try {
        const updated = await API.request('/api/profile', {
          method: 'PUT',
          body: JSON.stringify(payload)
        });

        // Update stored session name
        const user = API.getUser() || {};
        user.name = updated.name;
        API.setSession(null, user);

        API.showToast('Profile updated successfully!', 'success');
        loadUserProfile();
      } catch (err) {
        API.showToast(err.message || 'Failed to update profile', 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = origText;
      }
    });
  }

  const passwordForm = document.getElementById('password-form');
  if (passwordForm) {
    passwordForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const currentPassword = document.getElementById('current-password').value;
      const newPassword = document.getElementById('new-password').value;
      const confirmPassword = document.getElementById('confirm-password').value;

      if (newPassword !== confirmPassword) {
        API.showToast('New passwords do not match.', 'error');
        return;
      }

      if (newPassword.length < 6) {
        API.showToast('New password must be at least 6 characters long.', 'error');
        return;
      }

      const submitBtn = passwordForm.querySelector('button[type="submit"]');
      const origText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<span class="spinner"></span> Updating...';

      try {
        await API.request('/api/profile/change-password', {
          method: 'POST',
          body: JSON.stringify({ currentPassword, newPassword })
        });

        API.showToast('Password changed successfully!', 'success');
        passwordForm.reset();
      } catch (err) {
        API.showToast(err.message || 'Could not change password. Check current password.', 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = origText;
      }
    });
  }
}

// =====================================================================
// CAREGIVER & FAMILY SHARING NETWORK
// =====================================================================

async function loadCaregiverNetwork() {
  const myCaregiversContainer = document.getElementById('my-caregivers-list');
  const patientConnectionsContainer = document.getElementById('caring-for-patients-list');
  if (!myCaregiversContainer || !patientConnectionsContainer) return;

  try {
    const data = await API.request('/api/caregivers');
    const myCaregivers = data.myCaregivers || [];
    const caringForPatients = data.caringForPatients || [];

    // 1. Render my caregivers
    if (myCaregivers.length === 0) {
      myCaregiversContainer.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem; padding: 0.5rem 0;">No caregivers currently linked.</div>';
    } else {
      myCaregiversContainer.innerHTML = myCaregivers.map(cg => {
        const isPending = cg.status === 'PENDING';
        const isAccepted = cg.status === 'ACCEPTED';
        const badge = isAccepted
          ? '<span class="badge badge-success">✓ Active Caregiver</span>'
          : isPending
            ? '<span class="badge badge-warning">⏳ Pending Acceptance</span>'
            : '<span class="badge badge-danger">Revoked</span>';

        return `
          <div style="background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 0.75rem 1rem; margin-bottom: 0.5rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
            <div>
              <div style="font-weight: 600; font-size: 0.95rem;">${cg.caregiverName}</div>
              <div style="font-size: 0.8rem; color: var(--text-muted);">${cg.caregiverEmail}</div>
              <div style="margin-top: 0.25rem;">${badge} <span style="font-size: 0.75rem; color: var(--text-muted); margin-left: 0.4rem;">${cg.permissions.replace(/_/g, ' ')}</span></div>
            </div>
            ${cg.status !== 'REVOKED' ? `<button class="btn btn-danger btn-sm" onclick="revokeCaregiverAccess(${cg.id})">Revoke</button>` : ''}
          </div>
        `;
      }).join('');
    }

    // 2. Render patients caring for
    if (caringForPatients.length === 0) {
      patientConnectionsContainer.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem; padding: 0.5rem 0;">You are not currently caring for any patients.</div>';
    } else {
      patientConnectionsContainer.innerHTML = caringForPatients.map(p => {
        const isPending = p.status === 'PENDING';
        const isAccepted = p.status === 'ACCEPTED';

        let actionBtn = '';
        if (isPending) {
          actionBtn = `<button class="btn btn-success btn-sm" onclick="acceptCaregiverInvitation(${p.id})">Accept Invitation</button>`;
        } else if (isAccepted) {
          actionBtn = `<button class="btn btn-primary btn-sm" onclick="inspectPatientDoses(${p.patientId}, '${p.patientName.replace(/'/g, "\\'")}')">👁️ View Doses</button>`;
        }

        return `
          <div style="background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 0.75rem 1rem; margin-bottom: 0.5rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
            <div>
              <div style="font-weight: 600; font-size: 0.95rem;">${p.patientName}</div>
              <div style="font-size: 0.8rem; color: var(--text-muted);">${p.patientEmail}</div>
              <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.2rem;">Status: <strong>${p.status}</strong></div>
            </div>
            <div>
              ${actionBtn}
            </div>
          </div>
        `;
      }).join('');
    }
  } catch (err) {
    console.error('Failed to load caregiver network:', err);
  }
}

function setupCaregiverEvents() {
  const inviteForm = document.getElementById('invite-caregiver-form');
  if (inviteForm) {
    inviteForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('caregiver-email').value.trim();
      const missedAlerts = document.getElementById('perm-missed-alerts').checked;
      const permissions = missedAlerts ? 'VIEW_DOSES,MISSED_DOSE_ALERTS' : 'VIEW_DOSES';

      const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,64}$/;
      if (!emailRegex.test(email)) {
        API.showToast('Please enter a valid caregiver email address (e.g. caregiver@example.com).', 'error');
        return;
      }

      const submitBtn = inviteForm.querySelector('button[type="submit"]');
      const origText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<span class="spinner"></span> Sending...';

      try {
        await API.request('/api/caregivers/invite', {
          method: 'POST',
          body: JSON.stringify({ caregiverEmail: email, permissions })
        });
        API.showToast('Caregiver invitation sent successfully!', 'success');
        inviteForm.reset();
        document.getElementById('perm-missed-alerts').checked = true;
        loadCaregiverNetwork();
      } catch (err) {
        API.showToast(err.message || 'Failed to invite caregiver.', 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = origText;
      }
    });
  }

  // Close modals
  document.querySelectorAll('#caregiver-patient-modal .modal-close, #caregiver-patient-modal .modal-cancel').forEach(b => {
    b.addEventListener('click', () => {
      document.getElementById('caregiver-patient-modal').classList.remove('active');
    });
  });
}

async function acceptCaregiverInvitation(relationshipId) {
  try {
    await API.request(`/api/caregivers/${relationshipId}/accept`, { method: 'POST' });
    API.showToast('You accepted the caregiver request.', 'success');
    loadCaregiverNetwork();
  } catch (err) {
    API.showToast(err.message || 'Could not accept invitation.', 'error');
  }
}

async function revokeCaregiverAccess(relationshipId) {
  if (confirm('Are you sure you want to revoke this caregiver connection?')) {
    try {
      await API.request(`/api/caregivers/${relationshipId}/revoke`, { method: 'POST' });
      API.showToast('Caregiver connection revoked.', 'info');
      loadCaregiverNetwork();
    } catch (err) {
      API.showToast(err.message || 'Could not revoke access.', 'error');
    }
  }
}

async function inspectPatientDoses(patientId, patientName) {
  const modal = document.getElementById('caregiver-patient-modal');
  const title = document.getElementById('caregiver-patient-modal-title');
  const body = document.getElementById('caregiver-patient-modal-body');

  title.textContent = `Medication Status: ${patientName}`;
  body.innerHTML = '<div style="text-align: center; padding: 2rem;"><span class="spinner spinner-primary"></span> Loading doses...</div>';
  modal.classList.add('active');

  try {
    const summary = await API.request(`/api/caregivers/patient/${patientId}/summary`);
    const doses = summary.doses || [];

    let missedAlert = '';
    if (summary.confirmedMissedCount > 0) {
      missedAlert = `
        <div style="background: var(--danger-light); color: var(--danger); padding: 0.75rem 1rem; border-radius: var(--radius-sm); margin-bottom: 1rem; font-size: 0.85rem; font-weight: 600;">
          ⚠️ Alert: ${summary.confirmedMissedCount} dose(s) are overdue by more than 1 hour and remain unlogged.
        </div>
      `;
    }

    body.innerHTML = `
      ${missedAlert}
      <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.5rem; text-align: center; margin-bottom: 1.25rem;">
        <div style="background: var(--success-light); color: var(--success); padding: 0.5rem; border-radius: var(--radius-sm);">
          <div style="font-weight: 800; font-size: 1.2rem;">${summary.takenCount}</div>
          <div style="font-size: 0.75rem;">Taken</div>
        </div>
        <div style="background: var(--warning-light); color: var(--warning); padding: 0.5rem; border-radius: var(--radius-sm);">
          <div style="font-weight: 800; font-size: 1.2rem;">${summary.pendingCount}</div>
          <div style="font-size: 0.75rem;">Pending</div>
        </div>
        <div style="background: var(--danger-light); color: var(--danger); padding: 0.5rem; border-radius: var(--radius-sm);">
          <div style="font-weight: 800; font-size: 1.2rem;">${summary.skippedCount}</div>
          <div style="font-size: 0.75rem;">Skipped</div>
        </div>
        <div style="background: var(--bg-card-subtle); color: var(--danger); padding: 0.5rem; border-radius: var(--radius-sm);">
          <div style="font-weight: 800; font-size: 1.2rem;">${summary.confirmedMissedCount}</div>
          <div style="font-size: 0.75rem;">Missed (>1h)</div>
        </div>
      </div>

      <h5 style="margin-bottom: 0.5rem;">Today's Dosing Log:</h5>
      ${doses.length === 0 ? '<p style="color: var(--text-muted); font-size: 0.85rem;">No scheduled doses for today.</p>' : `
        <div style="display: flex; flex-direction: column; gap: 0.5rem;">
          ${doses.map(d => {
            const time = new Date(d.scheduledTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            let badge = '<span class="badge badge-warning">Pending</span>';
            if (d.status === 'TAKEN') badge = '<span class="badge badge-success">✓ Taken</span>';
            else if (d.status === 'SKIPPED') badge = '<span class="badge badge-danger">✕ Skipped</span>';
            else if (d.isOverdue) badge = '<span class="badge badge-danger">⚠️ Overdue (>1h)</span>';

            return `
              <div style="background: var(--bg-card-subtle); padding: 0.75rem; border-radius: var(--radius-sm); display: flex; justify-content: space-between; align-items: center;">
                <div>
                  <div style="font-weight: 600; font-size: 0.9rem;">${d.medicineName} (${d.dosage})</div>
                  <div style="font-size: 0.8rem; color: var(--text-muted);">Scheduled: <strong>${time}</strong></div>
                </div>
                <div>${badge}</div>
              </div>
            `;
          }).join('')}
        </div>
      `}
    `;
  } catch (err) {
    body.innerHTML = `<div style="color: var(--danger); padding: 1rem;">Failed to load patient summary: ${err.message}</div>`;
  }
}
