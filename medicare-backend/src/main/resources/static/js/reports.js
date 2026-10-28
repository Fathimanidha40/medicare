/**
 * MEDICARE - HEALTH REPORTS & ADHERENCE ANALYTICS
 * Date-range filtering, adherence calculation, interactive charts, CSV and PDF report export.
 */

document.addEventListener('DOMContentLoaded', () => {
  setupDateFilters();
  loadReportData();
  setupExportButtons();
});

function setupDateFilters() {
  const startInput = document.getElementById('report-start-date');
  const endInput = document.getElementById('report-end-date');

  const today = new Date();
  const past30 = new Date();
  past30.setDate(today.getDate() - 30);

  if (startInput) startInput.value = past30.toISOString().split('T')[0];
  if (endInput) endInput.value = today.toISOString().split('T')[0];

  const filterForm = document.getElementById('report-filter-form');
  if (filterForm) {
    filterForm.addEventListener('submit', (e) => {
      e.preventDefault();
      loadReportData();
    });
  }

  // Quick range buttons
  document.querySelectorAll('.date-preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const days = parseInt(btn.getAttribute('data-days'), 10);
      const d = new Date();
      d.setDate(today.getDate() - days);
      if (startInput) startInput.value = d.toISOString().split('T')[0];
      if (endInput) endInput.value = today.toISOString().split('T')[0];
      loadReportData();
    });
  });
}

async function loadReportData() {
  const startDate = document.getElementById('report-start-date').value;
  const endDate = document.getElementById('report-end-date').value;

  try {
    const report = await API.request(`/api/reports/adherence?startDate=${startDate}&endDate=${endDate}`);

    // Update Adherence summary cards
    document.getElementById('report-adherence-pct').textContent = `${report.adherencePercentage}%`;
    document.getElementById('report-total-doses').textContent = report.totalScheduledDoses;
    document.getElementById('report-taken-doses').textContent = report.takenDoses;
    document.getElementById('report-skipped-doses').textContent = report.skippedDoses;
    document.getElementById('report-pending-doses').textContent = report.pendingDoses;

    // Render adherence meter bar
    const bar = document.getElementById('adherence-progress-bar');
    if (bar) {
      bar.style.width = `${Math.min(100, report.adherencePercentage)}%`;
      if (report.adherencePercentage >= 80) bar.style.backgroundColor = 'var(--success)';
      else if (report.adherencePercentage >= 60) bar.style.backgroundColor = 'var(--warning)';
      else bar.style.backgroundColor = 'var(--danger)';
    }

    // Render Daily Breakdown Chart (CSS Bar Chart)
    renderDailyBars(report.dailyTakenCounts || {}, report.dailySkippedCounts || {});

    // Render Dose Logs Table
    renderLogsTable(report.recentLogs || []);
  } catch (err) {
    console.error('Failed to load report:', err);
    API.showToast('Could not load health reports.', 'error');
  }
}

function renderDailyBars(takenMap, skippedMap) {
  const chart = document.getElementById('daily-chart-container');
  if (!chart) return;

  const dates = Object.keys(takenMap);
  if (dates.length === 0) {
    chart.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 2rem;">No dose history within this range.</div>';
    return;
  }

  // Find max value for scaling
  let maxVal = 1;
  dates.forEach(d => {
    const sum = (takenMap[d] || 0) + (skippedMap[d] || 0);
    if (sum > maxVal) maxVal = sum;
  });

  chart.innerHTML = `
    <div style="display: flex; align-items: flex-end; gap: 0.5rem; height: 180px; padding-top: 1rem; overflow-x: auto;">
      ${dates.map(d => {
        const taken = takenMap[d] || 0;
        const skipped = skippedMap[d] || 0;
        const total = taken + skipped;
        const heightPct = (total / maxVal) * 100;
        const takenPct = total > 0 ? (taken / total) * 100 : 0;
        const skippedPct = total > 0 ? (skipped / total) * 100 : 0;

        const dateObj = new Date(d);
        const label = dateObj.toLocaleDateString([], { month: 'numeric', day: 'numeric' });

        return `
          <div style="flex: 1; min-width: 28px; display: flex; flex-direction: column; align-items: center; gap: 0.25rem;">
            <div style="font-size: 0.7rem; color: var(--text-muted);">${total}</div>
            <div style="width: 100%; height: ${Math.max(6, heightPct * 1.4)}px; display: flex; flex-direction: column; border-radius: 4px; overflow: hidden;" title="${d}: ${taken} Taken, ${skipped} Skipped">
              <div style="height: ${takenPct}%; background-color: var(--success);" title="${taken} Taken"></div>
              <div style="height: ${skippedPct}%; background-color: var(--danger);" title="${skipped} Skipped"></div>
            </div>
            <div style="font-size: 0.7rem; color: var(--text-muted);">${label}</div>
          </div>
        `;
      }).join('')}
    </div>
    <div style="display: flex; justify-content: center; gap: 1.5rem; margin-top: 1rem; font-size: 0.85rem;">
      <span style="display: flex; align-items: center; gap: 0.4rem;"><span style="width: 12px; height: 12px; border-radius: 2px; background: var(--success); display: inline-block;"></span> Taken</span>
      <span style="display: flex; align-items: center; gap: 0.4rem;"><span style="width: 12px; height: 12px; border-radius: 2px; background: var(--danger); display: inline-block;"></span> Skipped</span>
    </div>
  `;
}

function renderLogsTable(logs) {
  const tbody = document.getElementById('report-logs-body');
  if (!tbody) return;

  if (logs.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 1.5rem;">No action logs recorded in this period.</td></tr>';
    return;
  }

  tbody.innerHTML = logs.map(l => {
    const schedDate = new Date(l.scheduledTime).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    const respDate = new Date(l.responseTime).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    const badge = l.action === 'TAKEN'
      ? '<span class="badge badge-success">✓ Taken</span>'
      : '<span class="badge badge-danger">✕ Skipped</span>';

    return `
      <tr>
        <td style="font-weight: 600;">${l.medicine ? l.medicine.name : 'Unknown'}</td>
        <td>${l.medicine ? l.medicine.dosage : '-'}</td>
        <td style="font-size: 0.85rem;">${schedDate}</td>
        <td style="font-size: 0.85rem;">${respDate}</td>
        <td>${badge}</td>
        <td style="font-size: 0.85rem; color: var(--text-muted);">${l.notes || '-'}</td>
      </tr>
    `;
  }).join('');
}

function setupExportButtons() {
  const csvBtn = document.getElementById('export-csv-btn');
  if (csvBtn) {
    csvBtn.addEventListener('click', async () => {
      const startDate = document.getElementById('report-start-date').value;
      const endDate = document.getElementById('report-end-date').value;

      try {
        const blob = await API.request(`/api/reports/export/csv?startDate=${startDate}&endDate=${endDate}`);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `medicare_adherence_report_${startDate}_to_${endDate}.csv`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        API.showToast('CSV report downloaded.', 'success');
      } catch (err) {
        API.showToast('Failed to export CSV: ' + err.message, 'error');
      }
    });
  }

  const printPdfBtn = document.getElementById('export-pdf-btn');
  if (printPdfBtn) {
    printPdfBtn.addEventListener('click', () => {
      window.print();
    });
  }
}
