/**
 * MEDICARE - PRESCRIPTION MANAGEMENT CONTROLLER
 * Supports local file uploads (PDF, JPG, PNG) and Live Webcam Camera photograph capture.
 */

let cameraStream = null;
let capturedBlob = null;

document.addEventListener('DOMContentLoaded', () => {
  loadPrescriptions();
  setupFileUploadEvents();
  setupCameraCaptureEvents();
});

async function loadPrescriptions() {
  const container = document.getElementById('prescriptions-grid');
  const emptyState = document.getElementById('prescriptions-empty-state');
  if (!container) return;

  try {
    const list = await API.request('/api/prescriptions');

    if (list.length === 0) {
      container.innerHTML = '';
      if (emptyState) emptyState.style.display = 'block';
      return;
    }

    if (emptyState) emptyState.style.display = 'none';

    container.innerHTML = list.map(p => {
      const isPdf = p.fileType && p.fileType.includes('pdf');
      const icon = isPdf ? '📄' : '🖼️';
      const sizeMb = (p.fileSize / (1024 * 1024)).toFixed(2);

      return `
        <div class="card" style="margin-bottom: 0;">
          <div style="display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 1rem;">
            <div style="display: flex; align-items: center; gap: 0.75rem;">
              <div style="width: 48px; height: 48px; border-radius: 12px; background-color: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; font-size: 1.5rem;">
                ${icon}
              </div>
              <div>
                <h4 style="margin: 0; font-size: 1.05rem;">${p.title}</h4>
                <div style="font-size: 0.8rem; color: var(--text-muted);">Dr. ${p.doctorName || 'Not specified'} • ${p.prescriptionDate}</div>
              </div>
            </div>
            <button class="btn btn-danger btn-sm" onclick="confirmDeletePrescription(${p.id}, '${p.title.replace(/'/g, "\\'")}')">🗑️</button>
          </div>
          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1rem; min-height: 38px;">
            ${p.notes || 'No doctor notes attached.'}
          </p>
          <div style="font-size: 0.75rem; color: var(--text-light); margin-bottom: 1rem;">
            File: <strong>${p.originalFileName}</strong> (${sizeMb} MB)
          </div>
          <div style="display: flex; gap: 0.5rem;">
            <button class="btn btn-primary btn-sm" style="flex: 1;" onclick="viewPrescriptionFile(${p.id})">👁️ View / Download</button>
          </div>
        </div>
      `;
    }).join('');
  } catch (err) {
    console.error('Failed to load prescriptions:', err);
    API.showToast('Could not load prescriptions.', 'error');
  }
}

function setupFileUploadEvents() {
  const fileInput = document.getElementById('prescription-file-input');
  const filePreview = document.getElementById('file-preview-container');

  if (fileInput) {
    fileInput.addEventListener('change', () => {
      const file = fileInput.files[0];
      if (!file) {
        if (filePreview) filePreview.style.display = 'none';
        return;
      }

      if (filePreview) {
        filePreview.style.display = 'block';
        if (file.type.startsWith('image/')) {
          const reader = new FileReader();
          reader.onload = (e) => {
            filePreview.innerHTML = `
              <div style="margin-top: 0.75rem; text-align: center;">
                <img src="${e.target.result}" alt="Preview" style="max-height: 200px; max-width: 100%; border-radius: var(--radius-md); border: 1px solid var(--border);">
                <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.25rem;">${file.name}</div>
              </div>
            `;
          };
          reader.readAsDataURL(file);
        } else if (file.type.includes('pdf')) {
          filePreview.innerHTML = `
            <div style="margin-top: 0.75rem; padding: 1rem; background-color: var(--bg-card-subtle); border-radius: var(--radius-md); text-align: center;">
              📄 <strong>${file.name}</strong> (${(file.size / (1024 * 1024)).toFixed(2)} MB)
            </div>
          `;
        }
      }
    });
  }

  const uploadForm = document.getElementById('prescription-upload-form');
  if (uploadForm) {
    uploadForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const file = fileInput ? fileInput.files[0] : null;

      if (!file && !capturedBlob) {
        API.showToast('Please select a prescription file or capture a photo first.', 'error');
        return;
      }

      const submitBtn = uploadForm.querySelector('button[type="submit"]');
      submitBtn.disabled = true;
      submitBtn.innerHTML = '<span class="spinner"></span> Uploading...';

      const formData = new FormData();
      formData.append('title', document.getElementById('presc-title').value.trim() || 'Medical Prescription');
      formData.append('doctorName', document.getElementById('presc-doctor').value.trim());
      formData.append('prescriptionDate', document.getElementById('presc-date').value || new Date().toISOString().split('T')[0]);
      formData.append('notes', document.getElementById('presc-notes').value.trim());

      if (capturedBlob) {
        formData.append('file', capturedBlob, 'camera-prescription.png');
      } else {
        formData.append('file', file);
      }

      try {
        await API.request('/api/prescriptions', {
          method: 'POST',
          body: formData
        });

        API.showToast('Prescription uploaded securely!', 'success');
        document.getElementById('upload-prescription-modal').classList.remove('active');
        uploadForm.reset();
        capturedBlob = null;
        if (filePreview) filePreview.style.display = 'none';
        loadPrescriptions();
      } catch (err) {
        API.showToast(err.message || 'Upload failed', 'error');
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Upload Prescription';
      }
    });
  }

  const openUploadBtn = document.getElementById('open-upload-modal-btn');
  if (openUploadBtn) {
    openUploadBtn.addEventListener('click', () => {
      document.getElementById('presc-date').valueAsDate = new Date();
      document.getElementById('upload-prescription-modal').classList.add('active');
    });
  }
}

function setupCameraCaptureEvents() {
  const openCameraBtn = document.getElementById('open-camera-modal-btn');
  const cameraModal = document.getElementById('camera-modal');
  const video = document.getElementById('camera-video');
  const canvas = document.getElementById('camera-canvas');
  const snapBtn = document.getElementById('camera-snap-btn');
  const retakeBtn = document.getElementById('camera-retake-btn');
  const usePhotoBtn = document.getElementById('camera-use-btn');
  const capturedPreview = document.getElementById('camera-captured-preview');
  const cameraError = document.getElementById('camera-error');

  if (openCameraBtn) {
    openCameraBtn.addEventListener('click', async () => {
      cameraModal.classList.add('active');
      cameraError.style.display = 'none';
      video.style.display = 'block';
      capturedPreview.style.display = 'none';
      snapBtn.style.display = 'inline-flex';
      retakeBtn.style.display = 'none';
      usePhotoBtn.style.display = 'none';

      try {
        cameraStream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { ideal: 'environment' } },
          audio: false
        });
        video.srcObject = cameraStream;
        await video.play();
      } catch (err) {
        console.error('Camera access error:', err);
        cameraError.textContent = 'Unable to access camera: ' + (err.message || 'Permission denied or no webcam available.');
        cameraError.style.display = 'block';
      }
    });
  }

  if (snapBtn) {
    snapBtn.addEventListener('click', () => {
      if (!video.videoWidth) return;

      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

      canvas.toBlob((blob) => {
        capturedBlob = blob;
        capturedPreview.src = URL.createObjectURL(blob);
        capturedPreview.style.display = 'block';
        video.style.display = 'none';

        snapBtn.style.display = 'none';
        retakeBtn.style.display = 'inline-flex';
        usePhotoBtn.style.display = 'inline-flex';
      }, 'image/png');
    });
  }

  if (retakeBtn) {
    retakeBtn.addEventListener('click', () => {
      capturedBlob = null;
      video.style.display = 'block';
      capturedPreview.style.display = 'none';
      snapBtn.style.display = 'inline-flex';
      retakeBtn.style.display = 'none';
      usePhotoBtn.style.display = 'none';
    });
  }

  if (usePhotoBtn) {
    usePhotoBtn.addEventListener('click', () => {
      stopCamera();
      cameraModal.classList.remove('active');

      // Open the prescription details modal with the captured photo
      const uploadModal = document.getElementById('upload-prescription-modal');
      const filePreview = document.getElementById('file-preview-container');
      if (filePreview && capturedBlob) {
        filePreview.style.display = 'block';
        filePreview.innerHTML = `
          <div style="margin-top: 0.75rem; text-align: center;">
            <img src="${URL.createObjectURL(capturedBlob)}" alt="Captured" style="max-height: 200px; max-width: 100%; border-radius: var(--radius-md); border: 1px solid var(--border);">
            <div style="font-size: 0.8rem; color: var(--success); margin-top: 0.25rem;">✓ Photo captured with camera</div>
          </div>
        `;
      }
      document.getElementById('presc-title').value = 'Camera Captured Prescription';
      document.getElementById('presc-date').valueAsDate = new Date();
      uploadModal.classList.add('active');
    });
  }

  // Stop camera when closing modal
  document.querySelectorAll('#camera-modal .modal-close, #camera-modal .modal-cancel').forEach(btn => {
    btn.addEventListener('click', () => {
      stopCamera();
    });
  });
}

function stopCamera() {
  if (cameraStream) {
    cameraStream.getTracks().forEach(track => track.stop());
    cameraStream = null;
  }
}

async function viewPrescriptionFile(prescriptionId) {
  const modal = document.getElementById('view-prescription-modal');
  const titleEl = document.getElementById('view-modal-title');
  const bodyEl = document.getElementById('view-modal-body');
  const downloadBtn = document.getElementById('view-modal-download-btn');

  if (modal) {
    bodyEl.innerHTML = '<div style="text-align: center; padding: 3rem;"><span class="spinner spinner-primary"></span> Loading document...</div>';
    modal.classList.add('active');
  }

  try {
    const presc = await API.request(`/api/prescriptions/${prescriptionId}`);
    if (titleEl && presc) {
      titleEl.textContent = `📄 ${presc.title} — Dr. ${presc.doctorName || 'Prescription'}`;
    }

    const blob = await API.request(`/api/prescriptions/${prescriptionId}/file`);
    const blobUrl = URL.createObjectURL(blob);
    const isPdf = (blob.type && blob.type.includes('pdf')) || (presc && presc.originalFileName && presc.originalFileName.toLowerCase().endsWith('.pdf'));

    if (downloadBtn) {
      downloadBtn.href = blobUrl;
      downloadBtn.download = presc ? presc.originalFileName : 'prescription-document';
      downloadBtn.style.display = 'inline-flex';
    }

    if (isPdf) {
      bodyEl.innerHTML = `
        <div style="width: 100%; height: 550px;">
          <iframe src="${blobUrl}" style="width: 100%; height: 100%; border: 1px solid var(--border); border-radius: var(--radius-md);"></iframe>
        </div>
        <div style="margin-top: 1rem;">
          <a href="${blobUrl}" target="_blank" class="btn btn-outline btn-sm">🔗 Open PDF in New Window</a>
        </div>
      `;
    } else {
      bodyEl.innerHTML = `
        <div style="text-align: center; max-height: 550px; overflow: auto; padding: 0.5rem;">
          <img src="${blobUrl}" alt="Prescription Document" style="max-width: 100%; max-height: 520px; object-fit: contain; border-radius: var(--radius-md); box-shadow: var(--shadow-md); border: 1px solid var(--border);">
        </div>
      `;
    }
  } catch (err) {
    console.error('Prescription view error:', err);
    if (bodyEl) {
      bodyEl.innerHTML = `<div style="color: var(--danger); padding: 2rem;">Could not display prescription: ${err.message || 'File not readable'}</div>`;
    }
    API.showToast('Could not load prescription: ' + err.message, 'error');
  }
}

// Modal closing setup
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('#view-prescription-modal .modal-close, #view-prescription-modal .modal-cancel').forEach(b => {
    b.addEventListener('click', () => {
      const modal = document.getElementById('view-prescription-modal');
      if (modal) modal.classList.remove('active');
    });
  });
});

async function confirmDeletePrescription(id, title) {
  if (confirm(`Are you sure you want to delete "${title}"?`)) {
    try {
      await API.request(`/api/prescriptions/${id}`, { method: 'DELETE' });
      API.showToast('Prescription deleted successfully.', 'info');
      loadPrescriptions();
    } catch (err) {
      API.showToast(err.message || 'Could not delete prescription', 'error');
    }
  }
}
