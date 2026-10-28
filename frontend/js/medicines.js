/**
 * MEDICARE - MEDICINE MANAGEMENT CONTROLLER
 * Full CRUD, search, filter, stock management, and refill tracking.
 */

let allMedicines = [];

document.addEventListener('DOMContentLoaded', () => {
  loadMedicines();
  setupEventListeners();
});

async function loadMedicines() {
  const tableBody = document.getElementById('medicines-table-body');
  if (!tableBody) return;

  try {
    allMedicines = await API.request('/api/medicines');
    renderMedicinesList(allMedicines);
  } catch (err) {
    console.error('Failed to load medicines:', err);
    API.showToast('Could not load medicines from server.', 'error');
  }
}

function renderMedicinesList(medicines) {
  const tableBody = document.getElementById('medicines-table-body');
  const emptyState = document.getElementById('medicines-empty-state');
  if (!tableBody) return;

  if (medicines.length === 0) {
    tableBody.innerHTML = '';
    if (emptyState) emptyState.style.display = 'block';
    return;
  }

  if (emptyState) emptyState.style.display = 'none';

  tableBody.innerHTML = medicines.map(med => {
    const isLowStock = med.totalStock <= med.lowStockThreshold;
    const stockBadge = isLowStock
      ? `<span class="badge badge-danger">⚠️ ${med.totalStock} ${med.unit || ''} (Low)</span>`
      : `<span class="badge badge-success">${med.totalStock} ${med.unit || ''}</span>`;

    const activeBadge = med.active
      ? `<span class="badge badge-primary">Active</span>`
      : `<span class="badge badge-neutral">Completed</span>`;

    const tagColor = med.colorTag || '#1977cc';

    return `
      <tr>
        <td>
          <div style="display: flex; align-items: center; gap: 0.75rem;">
            <span style="display: inline-block; width: 14px; height: 14px; border-radius: 50%; background-color: ${tagColor}; flex-shrink: 0;" title="Color tag"></span>
            <div>
              <div style="font-weight: 700; color: var(--text-main);">${med.name}</div>
              <div style="font-size: 0.8rem; color: var(--text-muted);">${med.form} • ${med.dosage}</div>
            </div>
          </div>
        </td>
        <td>
          <div>${med.frequency || 'Daily'}</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">${med.foodTiming ? med.foodTiming.replace('_', ' ') : 'General'}</div>
        </td>
        <td>
          <div>From ${med.startDate}</div>
          <div style="font-size: 0.8rem; color: var(--text-muted);">${med.endDate ? 'To ' + med.endDate : 'Ongoing'}</div>
        </td>
        <td>
          ${stockBadge}
        </td>
        <td>
          ${activeBadge}
        </td>
        <td>
          <div style="display: flex; gap: 0.4rem;">
            <button class="btn btn-outline btn-sm" onclick="openRefillModal(${med.id})">+ Stock</button>
            <button class="btn btn-secondary btn-sm" onclick="openEditModal(${med.id})">Edit</button>
            <button class="btn btn-danger btn-sm" onclick="confirmDeleteMedicine(${med.id}, '${med.name.replace(/'/g, "\\'")}')">🗑️</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function setupEventListeners() {
  // Search and filter inputs
  const searchInput = document.getElementById('search-input');
  const formFilter = document.getElementById('form-filter');
  const statusFilter = document.getElementById('status-filter');

  function applyFilters() {
    const query = searchInput ? searchInput.value.toLowerCase().trim() : '';
    const formVal = formFilter ? formFilter.value : 'ALL';
    const statusVal = statusFilter ? statusFilter.value : 'ALL';

    const filtered = allMedicines.filter(m => {
      const matchesSearch = !query || m.name.toLowerCase().includes(query) || (m.prescribingDoctor && m.prescribingDoctor.toLowerCase().includes(query));
      const matchesForm = formVal === 'ALL' || m.form.toUpperCase() === formVal.toUpperCase();
      const matchesStatus = statusVal === 'ALL' || (statusVal === 'ACTIVE' ? m.active : !m.active);
      return matchesSearch && matchesForm && matchesStatus;
    });

    renderMedicinesList(filtered);
  }

  if (searchInput) searchInput.addEventListener('input', applyFilters);
  if (formFilter) formFilter.addEventListener('change', applyFilters);
  if (statusFilter) statusFilter.addEventListener('change', applyFilters);

  // Add Medicine Form
  const addForm = document.getElementById('medicine-form');
  if (addForm) {
    addForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const isEdit = addForm.getAttribute('data-edit-id');

      const payload = {
        name: document.getElementById('med-name').value.trim(),
        dosage: document.getElementById('med-dosage').value.trim(),
        unit: document.getElementById('med-unit').value,
        form: document.getElementById('med-form').value,
        instructions: document.getElementById('med-instructions').value.trim(),
        foodTiming: document.getElementById('med-timing').value,
        startDate: document.getElementById('med-start-date').value,
        endDate: document.getElementById('med-end-date').value || null,
        frequency: document.getElementById('med-frequency').value,
        prescribingDoctor: document.getElementById('med-doctor').value.trim(),
        totalStock: parseInt(document.getElementById('med-stock').value, 10) || 0,
        quantityPerDose: parseInt(document.getElementById('med-quantity-dose').value, 10) || 1,
        lowStockThreshold: parseInt(document.getElementById('med-threshold').value, 10) || 5,
        colorTag: document.getElementById('med-color').value,
        notes: document.getElementById('med-notes').value.trim(),
        active: true
      };

      try {
        if (isEdit) {
          await API.request(`/api/medicines/${isEdit}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
          });
          API.showToast('Medicine updated successfully!', 'success');
        } else {
          await API.request('/api/medicines', {
            method: 'POST',
            body: JSON.stringify(payload)
          });
          API.showToast('New medicine added successfully!', 'success');
        }

        document.getElementById('medicine-modal').classList.remove('active');
        addForm.reset();
        addForm.removeAttribute('data-edit-id');
        loadMedicines();
      } catch (err) {
        API.showToast(err.message || 'Operation failed', 'error');
      }
    });
  }

  // Refill Form
  const refillForm = document.getElementById('refill-form');
  if (refillForm) {
    refillForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const modal = document.getElementById('refill-modal');
      const medId = modal.getAttribute('data-medicine-id');
      const quantity = parseInt(document.getElementById('refill-quantity').value, 10);
      const notes = document.getElementById('refill-notes').value.trim();

      try {
        await API.request(`/api/medicines/${medId}/refill`, {
          method: 'POST',
          body: JSON.stringify({ quantity, notes })
        });
        API.showToast('Stock refilled successfully!', 'success');
        modal.classList.remove('active');
        refillForm.reset();
        loadMedicines();
      } catch (err) {
        API.showToast(err.message || 'Refill failed', 'error');
      }
    });
  }

  // Modal triggers
  const openAddBtn = document.getElementById('open-add-medicine-btn');
  if (openAddBtn) {
    openAddBtn.addEventListener('click', () => {
      const form = document.getElementById('medicine-form');
      if (form) {
        form.reset();
        form.removeAttribute('data-edit-id');
        document.getElementById('medicine-modal-title').textContent = 'Add New Medication';
        document.getElementById('med-start-date').valueAsDate = new Date();
      }
      document.getElementById('medicine-modal').classList.add('active');
    });
  }
}

function openEditModal(medId) {
  const med = allMedicines.find(m => m.id === medId);
  if (!med) return;

  const form = document.getElementById('medicine-form');
  form.setAttribute('data-edit-id', medId);
  document.getElementById('medicine-modal-title').textContent = 'Edit Medication';

  document.getElementById('med-name').value = med.name;
  document.getElementById('med-dosage').value = med.dosage;
  document.getElementById('med-unit').value = med.unit || 'mg';
  document.getElementById('med-form').value = med.form || 'TABLET';
  document.getElementById('med-instructions').value = med.instructions || '';
  document.getElementById('med-timing').value = med.foodTiming || 'AFTER_FOOD';
  document.getElementById('med-start-date').value = med.startDate;
  document.getElementById('med-end-date').value = med.endDate || '';
  document.getElementById('med-frequency').value = med.frequency || 'DAILY';
  document.getElementById('med-doctor').value = med.prescribingDoctor || '';
  document.getElementById('med-stock').value = med.totalStock != null ? med.totalStock : 0;
  document.getElementById('med-quantity-dose').value = med.quantityPerDose || 1;
  document.getElementById('med-threshold').value = med.lowStockThreshold || 5;
  document.getElementById('med-color').value = med.colorTag || '#1977cc';
  document.getElementById('med-notes').value = med.notes || '';

  document.getElementById('medicine-modal').classList.add('active');
}

function openRefillModal(medId) {
  const med = allMedicines.find(m => m.id === medId);
  if (!med) return;

  const modal = document.getElementById('refill-modal');
  modal.setAttribute('data-medicine-id', medId);
  document.getElementById('refill-med-name').textContent = `${med.name} (${med.dosage})`;
  document.getElementById('refill-current-stock').textContent = `${med.totalStock} ${med.unit || 'units'}`;
  document.getElementById('refill-quantity').value = 30;

  modal.classList.add('active');
}

async function confirmDeleteMedicine(medId, medName) {
  if (confirm(`Are you sure you want to delete "${medName}"? This will also remove its associated schedules and dose history.`)) {
    try {
      await API.request(`/api/medicines/${medId}`, { method: 'DELETE' });
      API.showToast('Medicine deleted successfully.', 'info');
      loadMedicines();
    } catch (err) {
      API.showToast(err.message || 'Could not delete medicine', 'error');
    }
  }
}

// Medication Safety Guide & Lookup Tool
const DRUG_DATABASE = {
  'Amoxicillin': {
    category: 'Penicillin-Class Antibiotic',
    indication: 'Bacterial infections of the respiratory tract, ear, nose, throat, skin, and urinary tract.',
    timing: 'Take at evenly spaced intervals with or without food. Complete the full course.',
    effects: 'Mild diarrhea, nausea, skin rash, headache.',
    precautions: 'Do not use if allergic to penicillin or cephalosporins. Report severe watery diarrhea immediately.'
  },
  'Metformin': {
    category: 'Biguanide Antidiabetic Agent',
    indication: 'First-line management of Type 2 Diabetes Mellitus to improve glycemic control.',
    timing: 'Take with or immediately after meals to reduce stomach discomfort and GI side effects.',
    effects: 'Nausea, metallic taste, diarrhea, abdominal discomfort.',
    precautions: 'Avoid excessive alcohol consumption. Temporarily hold before contrast dye procedures.'
  },
  'Atorvastatin': {
    category: 'HMG-CoA Reductase Inhibitor (Statin)',
    indication: 'Lowers LDL cholesterol and triglycerides; reduces risk of myocardial infarction and stroke.',
    timing: 'Take once daily at any time of day (often in evening), with or without food.',
    effects: 'Muscle aches, joint pain, mild headache, mild nausea.',
    precautions: 'Avoid grapefruit and grapefruit juice. Promptly report unexplained muscle pain or brown urine.'
  },
  'Lisinopril': {
    category: 'Angiotensin-Converting Enzyme (ACE) Inhibitor',
    indication: 'Treatment of hypertension (high blood pressure) and heart failure; renal protection in diabetes.',
    timing: 'Take once daily at the same time each day, with or without food.',
    effects: 'Dry persistent cough, dizziness upon standing, fatigue, elevated potassium.',
    precautions: 'Avoid potassium salt substitutes. Stand up slowly to prevent orthostatic hypotension. Do not take during pregnancy.'
  },
  'Omeprazole': {
    category: 'Proton Pump Inhibitor (PPI)',
    indication: 'Gastroesophageal reflux disease (GERD), acid indigestion, stomach ulcers, and erosive esophagitis.',
    timing: 'Take 30 to 60 minutes before breakfast with a full glass of water.',
    effects: 'Headache, stomach pain, flatulence, mild constipation.',
    precautions: 'Designed for short-to-medium term use under medical supervision. May affect magnesium and B12 absorption with prolonged usage.'
  },
  'Paracetamol': {
    category: 'Analgesic & Antipyretic',
    indication: 'Relief of mild to moderate pain (headache, muscle ache, toothache) and reduction of fever.',
    timing: 'Take every 4 to 6 hours as needed with water. Do not exceed 4,000 mg in 24 hours.',
    effects: 'Generally well tolerated at therapeutic doses; rare allergic rash.',
    precautions: 'Never combine with other OTC cold/cough medicines containing acetaminophen to prevent accidental severe liver injury.'
  }
};

document.addEventListener('DOMContentLoaded', () => {
  setupSafetyGuideEvents();
});

function setupSafetyGuideEvents() {
  const toggleBtn = document.getElementById('safety-guide-toggle-btn');
  const toggleHeader = document.getElementById('toggle-safety-guide');
  const content = document.getElementById('safety-guide-content');

  function toggle() {
    if (!content) return;
    const isHidden = content.style.display === 'none';
    content.style.display = isHidden ? 'block' : 'none';
    if (toggleBtn) {
      toggleBtn.textContent = isHidden ? 'Collapse Guide ▴' : 'Expand Guide ▾';
    }
  }

  if (toggleHeader) {
    toggleHeader.addEventListener('click', (e) => {
      if (e.target !== toggleBtn) toggle();
    });
  }
  if (toggleBtn) toggleBtn.addEventListener('click', toggle);

  // Quick Drug Lookup Buttons
  document.querySelectorAll('.quick-drug-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const drugName = btn.getAttribute('data-drug');
      const info = DRUG_DATABASE[drugName];
      if (!info) return;

      const display = document.getElementById('drug-info-display');
      document.getElementById('drug-info-title').textContent = drugName;
      document.getElementById('drug-info-class').textContent = info.category;
      document.getElementById('drug-info-indication').textContent = info.indication;
      document.getElementById('drug-info-timing').textContent = info.timing;
      document.getElementById('drug-info-effects').textContent = info.effects;
      document.getElementById('drug-info-precautions').textContent = info.precautions;

      if (display) display.style.display = 'block';

      // Highlight selected button
      document.querySelectorAll('.quick-drug-btn').forEach(b => {
        b.classList.remove('btn-primary');
        b.classList.add('btn-secondary');
      });
      btn.classList.remove('btn-secondary');
      btn.classList.add('btn-primary');
    });
  });
}
