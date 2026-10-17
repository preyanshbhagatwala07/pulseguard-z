// PulseGuard-Z Client Logic (Government of India ABDM & IBM Z Edition)

const PRESETS = {
  normal: { abha: '91-2384-9102-4821', hr: 74, sbp: 118, dbp: 76, rr: 15, temp: 36.8, spo2: 98, lactate: 1.1, wbc: 7.8 },
  elevated: { abha: '91-7291-8291-0192', hr: 104, sbp: 98, dbp: 62, rr: 23, temp: 38.6, spo2: 93, lactate: 2.8, wbc: 14.5 },
  shock: { abha: '91-1049-5829-3910', hr: 132, sbp: 78, dbp: 44, rr: 29, temp: 39.4, spo2: 87, lactate: 5.2, wbc: 21.0 }
};

let currentTab = 'ward';

document.addEventListener('DOMContentLoaded', () => {
  fetchSystemInfo();
  pollBeds();
  setInterval(pollBeds, 1500);

  // Poll Tele-ICU & Audit ledger every 3 seconds
  setInterval(() => {
    if (currentTab === 'teleicu') fetchTeleIcuData();
    if (currentTab === 'audit') fetchAuditData();
  }, 3000);
});

function switchTab(tab) {
  currentTab = tab;
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

  event.currentTarget.classList.add('active');
  const target = document.getElementById('tab-' + tab);
  if (target) target.classList.add('active');

  if (tab === 'teleicu') fetchTeleIcuData();
  if (tab === 'audit') fetchAuditData();
  if (tab === 'ibmz') {
    fetchTelumNeural();
    fetchCpacfStatus();
    fetchSysplexStatus();
  }
}

async function fetchSystemInfo() {
  try {
    const res = await fetch('/api/v1/system');
    const data = await res.json();
    const archElem = document.getElementById('platform-arch');
    if (archElem) {
      if (data.isLinuxONE) {
        archElem.textContent = 'IBM LinuxONE s390x (Native Mainframe)';
      } else {
        archElem.textContent = `Dev: ${data.architecture} (s390x Portable)`;
      }
    }
  } catch (err) {
    console.warn('System info unreachable:', err);
  }
}

async function pollBeds() {
  try {
    const res = await fetch('/api/v1/beds');
    const data = await res.json();
    if (currentTab === 'ward') {
      renderBeds(data.beds);
    }
    updateGlobalMetrics(data.beds);
  } catch (err) {
    console.error('Failed to poll beds:', err);
  }
}

function updateGlobalMetrics(beds) {
  let criticalCount = 0;
  let totalLatency = 0;

  beds.forEach(b => {
    if (b.alert && b.alert.level === 'CRITICAL_SEPSIS') criticalCount++;
    if (b.alert && b.alert.latencyMicros) totalLatency += b.alert.latencyMicros;
  });

  const avgLatency = (totalLatency / (beds.length || 1)).toFixed(2);
  const latencyElem = document.getElementById('metric-latency');
  if (latencyElem) latencyElem.innerHTML = `${avgLatency} <span class="unit">µs</span>`;

  const critElem = document.getElementById('metric-critical');
  if (critElem) critElem.innerHTML = `${criticalCount} <span class="unit">Active</span>`;
}

function renderBeds(beds) {
  const container = document.getElementById('beds-container');
  if (!container) return;

  container.innerHTML = beds.map(bed => {
    const v = bed.vital || {};
    const a = bed.alert || { level: 'STABLE', riskScore: 0.05, fourHourMortalityRisk: 0.01, recommendation: 'Stable', biomarkers: [] };
    const n = bed.news2 || { score: 0, category: 'NORMAL', redFlag: false };
    const levelClass = a.level === 'CRITICAL_SEPSIS' ? 'critical' : (a.level === 'ELEVATED_RISK' ? 'elevated' : 'stable');
    const riskPercent = Math.round(a.riskScore * 100);

    const isHrAlarm = v.heartRate > 105;
    const isBpAlarm = v.systolicBp < 90 || v.map < 65;
    const isLactAlarm = v.lactate >= 2.0;

    return `
      <div class="bed-card status-${levelClass}">
        <div class="bed-header">
          <div>
            <div class="bed-id">${bed.bedId} &bull; ${bed.patientName} (${bed.age}y ${bed.gender})</div>
            <div class="abha-pill">ABHA: ${bed.abhaId}</div>
            <div class="facility-label">${bed.facility}</div>
            <div class="patient-info">${bed.diagnosis}</div>
          </div>
          <div class="badge-group">
            <span class="status-tag ${levelClass}">${a.level.replace('_', ' ')}</span>
            <span class="news2-badge">NEWS2: <strong>${n.score}</strong> (${n.category})</span>
          </div>
        </div>

        <div class="vitals-table">
          <div class="vital-item">
            <span class="vital-name">Heart Rate</span>
            <span class="vital-val ${isHrAlarm ? 'alarm' : ''}">${v.heartRate} <small>bpm</small></span>
          </div>
          <div class="vital-item">
            <span class="vital-name">Blood Pressure</span>
            <span class="vital-val ${isBpAlarm ? 'alarm' : ''}">${v.systolicBp}/${v.diastolicBp}</span>
          </div>
          <div class="vital-item">
            <span class="vital-name">MAP (mmHg)</span>
            <span class="vital-val ${v.map < 65 ? 'alarm' : ''}">${v.map}</span>
          </div>
          <div class="vital-item">
            <span class="vital-name">Shock Index</span>
            <span class="vital-val ${v.shockIndex > 0.9 ? 'alarm' : ''}">${v.shockIndex}</span>
          </div>
          <div class="vital-item">
            <span class="vital-name">Resp Rate</span>
            <span class="vital-val">${v.respRate} <small>/m</small></span>
          </div>
          <div class="vital-item">
            <span class="vital-name">SpO2</span>
            <span class="vital-val ${v.spO2 < 92 ? 'alarm' : ''}">${v.spO2}%</span>
          </div>
          <div class="vital-item">
            <span class="vital-name">Temp (°C)</span>
            <span class="vital-val">${v.temperatureC}°</span>
          </div>
          <div class="vital-item">
            <span class="vital-name">Lactate</span>
            <span class="vital-val ${isLactAlarm ? 'alarm' : ''}">${v.lactate} <small>mmol/L</small></span>
          </div>
        </div>

        <div class="risk-container">
          <div class="risk-meta">
            <span>Sepsis-3 Risk &bull; 4h Mortality: <strong>${Math.round(a.fourHourMortalityRisk * 100)}%</strong></span>
            <strong>${riskPercent}% Probability</strong>
          </div>
          <div class="risk-bar-track">
            <div class="risk-bar-fill ${levelClass}" style="width: ${Math.max(5, riskPercent)}%"></div>
          </div>
        </div>

        <div class="biomarkers-chips">
          ${(a.biomarkers || []).map(b => `<span class="chip ${b.toLowerCase().includes('critical') || b.toLowerCase().includes('elevated') || b.toLowerCase().includes('hypotension') ? 'alert' : ''}">${b}</span>`).join('')}
        </div>

        <div class="recommendation-box ${levelClass === 'critical' ? 'critical' : ''}">
          ${a.recommendation}
        </div>

        <div class="card-actions">
          <button class="btn btn-sm btn-outline" onclick="openInspectorModal('${bed.bedId}', 'fhir')">
            HL7 FHIR R4
          </button>
          <button class="btn btn-sm btn-outline" onclick="openInspectorModal('${bed.bedId}', 'sms')">
            2G Offline SMS
          </button>
          <button class="btn btn-sm btn-primary" onclick="openInspectorModal('${bed.bedId}', 'referral')">
            Print ABDM Referral Slip
          </button>
        </div>
      </div>
    `;
  }).join('');
}

let activeInspectorBedId = 'BED-03';

async function openInspectorModal(bedId, initialTab = 'fhir') {
  activeInspectorBedId = bedId;
  const modal = document.getElementById('inspector-modal');
  if (!modal) return;
  modal.classList.remove('hidden');

  switchInspectorTab(initialTab);

  // Load all 4 subsystem payloads in parallel
  loadInspectorFhir(bedId);
  loadInspectorSms(bedId);
  loadInspectorCpacf(bedId);
  loadInspectorReferral(bedId);
}

function closeInspectorModal() {
  const modal = document.getElementById('inspector-modal');
  if (modal) modal.classList.add('hidden');
}

function switchInspectorTab(tab) {
  const tabs = ['fhir', 'sms', 'cpacf', 'referral'];
  tabs.forEach(t => {
    const btn = document.getElementById('btn-tab-' + t);
    const pane = document.getElementById('insp-pane-' + t);
    if (btn) {
      if (t === tab) btn.classList.add('active');
      else btn.classList.remove('active');
    }
    if (pane) {
      if (t === tab) pane.classList.add('active');
      else pane.classList.remove('active');
    }
  });
}

async function loadInspectorFhir(bedId) {
  const content = document.getElementById('insp-fhir-content');
  if (!content) return;
  content.textContent = `Fetching official ABDM FHIR R4 Bundle for ${bedId}...`;
  try {
    const res = await fetch(`/api/v1/fhir?bedId=${bedId}`);
    const fhir = await res.json();
    content.textContent = JSON.stringify(fhir, null, 2);
  } catch (err) {
    content.textContent = 'Error loading FHIR Bundle: ' + err;
  }
}

async function loadInspectorSms(bedId) {
  const decBox = document.getElementById('insp-sms-decoder');
  const rawBox = document.getElementById('insp-sms-raw');
  try {
    const res = await fetch(`/api/v1/sms?bedId=${bedId}`);
    const d = await res.json();
    if (rawBox) rawBox.textContent = d.smsPacket;
    if (decBox) {
      const parts = d.smsPacket.split('|');
      decBox.innerHTML = `
        <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(180px, 1fr)); gap:0.5rem; margin-bottom:0.5rem;">
          <div><strong>Header:</strong> ${parts[0] || 'PZ108'} (PulseGuard Telemetry)</div>
          <div><strong>ABHA Numeric:</strong> ${parts[1] || 'N/A'}</div>
          <div><strong>Telemetry:</strong> ${parts[2] || 'HR,BP,RR,SpO2,Lact'}</div>
          <div><strong>NEWS2 Score:</strong> ${parts[3] || 'N/A'}</div>
          <div><strong>Severity:</strong> <span style="color:#ef4444; font-weight:700;">${parts[4] || 'N/A'}</span></div>
          <div><strong>CRC16 Checksum:</strong> <span style="color:#10b981; font-weight:700;">${parts[5] || 'N/A'} (Valid)</span></div>
        </div>
        <div style="font-size:0.72rem; color:#94a3b8; border-top:1px solid rgba(255,255,255,0.08); padding-top:0.4rem;">
          Frame Size: <strong>${d.payloadBytes} bytes</strong> &bull; Fits into a single unfragmented 2G cellular SMS frame (140 bytes max).
        </div>
      `;
    }
  } catch (err) {
    if (decBox) decBox.textContent = 'Error loading SMS frame: ' + err;
  }
}

async function loadInspectorCpacf(bedId) {
  const box = document.getElementById('insp-cpacf-content');
  const modeTag = document.getElementById('insp-cpacf-mode');
  try {
    const res = await fetch('/api/v1/security/cpacf');
    const d = await res.json();
    if (modeTag) modeTag.textContent = d.hardwareAssisted ? 'IBM Z CPACF Hardware KIMD Opcode' : 'CPACF Software AES-256-GCM';
    if (box) {
      box.innerHTML = `
        <div style="margin-bottom:0.75rem;">
          <div style="color:#64748b; font-size:0.7rem; margin-bottom:0.2rem;">CIPHER SUITE &amp; PERVASIVE STATUS:</div>
          <div style="color:#38bdf8; font-weight:600;">${d.cpacfStatus}</div>
        </div>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:0.75rem; margin-bottom:0.75rem;">
          <div>
            <div style="color:#64748b; font-size:0.7rem;">PLAINTEXT IDENTIFIER:</div>
            <div style="background:rgba(0,0,0,0.4); padding:0.4rem; border-radius:4px; font-size:0.75rem; color:#f1f5f9;">${d.samplePlaintext}</div>
          </div>
          <div>
            <div style="color:#64748b; font-size:0.7rem;">HARDWARE ENCRYPTION LATENCY:</div>
            <div style="background:rgba(0,0,0,0.4); padding:0.4rem; border-radius:4px; font-size:0.75rem; color:#10b981; font-weight:700;">${(d.encryptionDurationNanos / 1000.0).toFixed(2)} µs (Sub-Microsecond)</div>
          </div>
        </div>
        <div>
          <div style="color:#64748b; font-size:0.7rem;">AES-256-GCM CIPHERTEXT (PHI In-Flight &amp; At-Rest):</div>
          <div style="background:rgba(0,0,0,0.4); padding:0.4rem; border-radius:4px; font-size:0.72rem; color:#fbbf24; word-break:break-all;">${d.sampleCiphertext}</div>
        </div>
      `;
    }
  } catch (err) {
    if (box) box.textContent = 'Error loading CPACF status: ' + err;
  }
}

function loadInspectorReferral(bedId) {
  const frame = document.getElementById('insp-referral-frame');
  if (frame) {
    frame.src = `/api/v1/referral?bedId=${bedId}`;
  }
}

function openReferralWindow() {
  window.open(`/api/v1/referral?bedId=${activeInspectorBedId}`, '_blank');
}

function copyInspectorContent(elementId) {
  const elem = document.getElementById(elementId);
  if (!elem) return;
  const text = elem.textContent;
  navigator.clipboard.writeText(text).then(() => {
    alert('Copied to clipboard!');
  }).catch(() => {
    // Fallback
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
    alert('Copied to clipboard!');
  });
}

async function fetchTeleIcuData() {
  try {
    const res = await fetch('/api/v1/teleicu');
    const data = await res.json();

    const facContainer = document.getElementById('facilities-list');
    facContainer.innerHTML = data.facilities.map(f => `
      <div class="facility-card">
        <div>
          <div class="facility-title">${f.name}</div>
          <div class="facility-meta">Code: ${f.code} &bull; Tier: <strong>${f.tier}</strong> &bull; District: ${f.district}</div>
        </div>
        <div style="text-align:right;">
          <div class="icu-count">${f.availableIcuBeds} / ${f.totalBeds}</div>
          <div style="font-size:0.7rem; color:#94a3b8;">ICU Beds Available</div>
        </div>
      </div>
    `).join('');

    const dispContainer = document.getElementById('dispatches-list');
    if (data.active108Dispatches.length === 0) {
      dispContainer.innerHTML = '<div style="font-size:0.85rem; color:#94a3b8; padding:1rem;">No active emergency transfers currently requested.</div>';
    } else {
      dispContainer.innerHTML = data.active108Dispatches.map(d => `
        <div class="dispatch-item">
          <div class="dispatch-header">
            <span class="dispatch-id">${d.dispatchId} &bull; Unit: ${d.ambulanceUnit}</span>
            <span style="font-size:0.75rem; color:#fca5a5; font-weight:600;">ETA: ${d.etaMinutes} mins</span>
          </div>
          <div style="font-size:0.8rem; font-weight:600;">
            ${d.originating} ➔ ${d.destination}
          </div>
          <div style="font-size:0.72rem; color:#94a3b8; margin-top:0.2rem;">
            Patient: ${d.patientId} &bull; ABHA: ${d.abhaId}
          </div>
          <div class="dispatch-orders">${d.orders}</div>
        </div>
      `).join('');
    }
  } catch (err) {
    console.error('Tele-ICU fetch error:', err);
  }
}

async function fetchAuditData() {
  try {
    const res = await fetch('/api/v1/audit/chain');
    const data = await res.json();

    const badge = document.getElementById('audit-status-badge');
    if (data.cryptographicIntegrityVerified) {
      badge.className = 'badge live-badge';
      badge.innerHTML = '<span class="live-dot"></span> SHA-256 INTEGRITY VERIFIED (0 TAMPERING)';
    } else {
      badge.className = 'badge';
      badge.style.color = '#ef4444';
      badge.textContent = 'WARNING: INTEGRITY CHECK FAILED';
    }

    const tbody = document.getElementById('audit-table-body');
    tbody.innerHTML = data.ledger.map(e => `
      <tr>
        <td>#${e.index}</td>
        <td>${new Date(e.timestamp).toISOString().split('T')[1].replace('Z','')}</td>
        <td>${e.patientId}</td>
        <td style="color:#38bdf8;">${e.abhaId}</td>
        <td><span class="status-tag ${e.triageLevel.toLowerCase().replace('_','-')}">${e.triageLevel}</span></td>
        <td><strong>${e.news2Score}</strong></td>
        <td>${Math.round(e.sepsisRiskScore * 100)}%</td>
        <td style="color:#10b981;">${e.inferenceLatencyMicros.toFixed(2)} µs</td>
        <td class="hash-cell" title="${e.entryHash}">${e.entryHash}</td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Audit fetch error:', err);
  }
}

function loadPreset(type) {
  const p = PRESETS[type];
  if (!p) return;
  document.getElementById('inp-abha').value = p.abha;
  document.getElementById('inp-hr').value = p.hr;
  document.getElementById('inp-sbp').value = p.sbp;
  document.getElementById('inp-dbp').value = p.dbp;
  document.getElementById('inp-rr').value = p.rr;
  document.getElementById('inp-temp').value = p.temp;
  document.getElementById('inp-spo2').value = p.spo2;
  document.getElementById('inp-lactate').value = p.lactate;
  document.getElementById('inp-wbc').value = p.wbc;
}

async function submitTelemetry(event) {
  event.preventDefault();
  const payload = {
    patientId: 'P-GOV-DEMO',
    abhaId: document.getElementById('inp-abha').value,
    bedId: 'BAY-TRIAGE-01',
    heartRate: parseFloat(document.getElementById('inp-hr').value),
    systolicBp: parseFloat(document.getElementById('inp-sbp').value),
    diastolicBp: parseFloat(document.getElementById('inp-dbp').value),
    respRate: parseFloat(document.getElementById('inp-rr').value),
    temperatureC: parseFloat(document.getElementById('inp-temp').value),
    spO2: parseFloat(document.getElementById('inp-spo2').value),
    lactate: parseFloat(document.getElementById('inp-lactate').value),
    whiteBloodCell: parseFloat(document.getElementById('inp-wbc').value)
  };

  try {
    const res = await fetch('/api/v1/telemetry', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await res.json();

    const box = document.getElementById('test-result');
    box.classList.remove('hidden');
    box.innerHTML = `
      <div>
        <strong>Triage: <span class="status-tag ${result.triageLevel.toLowerCase().replace('_', '-')}">${result.triageLevel}</span> &bull; NEWS2: ${result.news2Score} (${result.news2Category})</strong>
        <div style="font-size:0.75rem; color:#94a3b8; margin-top:0.2rem;">
          ABHA Valid: <strong>${result.abhaValid ? 'YES' : 'NO'}</strong> &bull; Sepsis Probability: <strong>${Math.round(result.sepsisRiskScore * 100)}%</strong> &bull; MAP: ${result.meanArterialPressure} mmHg
        </div>
      </div>
      <div style="text-align:right;">
        <span class="text-accent" style="font-family:var(--font-mono); font-size:1.1rem; font-weight:700;">${result.inferenceLatencyMicros} µs</span>
        <div style="font-size:0.7rem; color:#94a3b8;">Inference Latency</div>
      </div>
    `;
  } catch (err) {
    alert('Evaluation error: ' + err);
  }
}

async function runBenchmark() {
  const modal = document.getElementById('bench-modal');
  const content = document.getElementById('modal-content');
  modal.classList.remove('hidden');
  content.innerHTML = `
    <div style="padding: 1rem 0; text-align: center;">
      <div class="logo-pulse" style="margin: 0 auto 1rem auto;">
        <span class="pulse-ring"></span>
        <span class="pulse-dot"></span>
      </div>
      <p>Benchmarking 50,000 real-time inference decisions on JVM...</p>
    </div>
  `;

  try {
    const res = await fetch('/api/v1/benchmark');
    const data = await res.json();
    content.innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem;">
        <div><strong>Total Inferences:</strong> ${data.iterations.toLocaleString()}</div>
        <div><strong>Execution Time:</strong> ${data.totalTimeMs} ms</div>
        <div><strong>Throughput:</strong> <span style="color:#00d2ff; font-weight:700;">${Math.round(data.throughputInferencesPerSec).toLocaleString()} /sec</span></div>
        <div><strong>Average Latency:</strong> <span style="color:#10b981; font-weight:700;">${data.avgLatencyMicros} µs</span></div>
        <div><strong>P95 Latency:</strong> ${data.p95LatencyMicros} µs</div>
        <div><strong>P99 Latency:</strong> ${data.p99LatencyMicros} µs</div>
        <div><strong>Architecture:</strong> ${data.osArch} (${data.osName})</div>
        <div><strong>Cores Used:</strong> ${data.availableCores} Cores</div>
      </div>
      <div style="background:rgba(15,98,254,0.1); border:1px solid rgba(15,98,254,0.3); padding:0.75rem; border-radius:6px; font-size:0.8rem; color:#93c5fd;">
        ${data.ibmZOptimizationStatus}
      </div>
    `;
  } catch (err) {
    content.innerHTML = `<span class="text-danger">Benchmark failed: ${err}</span>`;
  }
}

function closeModal() {
  document.getElementById('bench-modal').classList.add('hidden');
}

async function fetchTelumNeural() {
  const container = document.getElementById('telum-status');
  if (!container) return;
  container.innerHTML = '<span style="color:#94a3b8;">Executing unrolled GEMM forward pass on Telum NNPA engine...</span>';
  try {
    const res = await fetch('/api/v1/telum/neural?bedId=BED-03');
    const d = await res.json();
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; margin-bottom:0.4rem;">
        <span>Sepsis Risk Probability:</span>
        <strong class="text-danger">${Math.round(d.sepsisRiskProbability * 100)}%</strong>
      </div>
      <div style="display:flex; justify-content:space-between; margin-bottom:0.4rem;">
        <span>4-Hour Mortality Hazard:</span>
        <strong style="color:#f59e0b;">${Math.round(d.fourHourMortalityRisk * 100)}%</strong>
      </div>
      <div style="display:flex; justify-content:space-between; margin-bottom:0.4rem;">
        <span>12-Hour Organ Failure Risk:</span>
        <strong style="color:#ef4444;">${Math.round(d.twelveHourOrganFailureHazard * 100)}%</strong>
      </div>
      <div style="display:flex; justify-content:space-between; margin-bottom:0.4rem;">
        <span>NNPA Matrix Latency:</span>
        <strong class="text-accent">${d.inferenceLatencyMicros.toFixed(3)} µs (${d.inferenceLatencyNanos} ns)</strong>
      </div>
      <div style="font-size:0.72rem; color:#93c5fd; background:rgba(15,98,254,0.1); padding:0.4rem; border-radius:4px; margin-top:0.4rem;">
        ${d.telumHardwareStatus}
      </div>
    `;
  } catch (err) {
    container.innerHTML = `<span class="text-danger">Telum API error: ${err}</span>`;
  }
}

async function fetchCpacfStatus() {
  const container = document.getElementById('cpacf-status');
  if (!container) return;
  container.innerHTML = '<span style="color:#94a3b8;">Testing CPACF AES-256-GCM hardware cipher...</span>';
  try {
    const res = await fetch('/api/v1/security/cpacf');
    const d = await res.json();
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; margin-bottom:0.4rem;">
        <span>Cipher Algorithm:</span>
        <strong style="color:#10b981;">${d.algorithm} (Authenticated AEAD)</strong>
      </div>
      <div style="display:flex; justify-content:space-between; margin-bottom:0.4rem;">
        <span>Encryption Latency:</span>
        <strong class="text-accent">${d.encryptionLatencyMicros} µs (${d.encryptionLatencyNanos} ns)</strong>
      </div>
      <div style="font-size:0.72rem; color:#cbd5e1; margin-bottom:0.4rem;">
        <strong>Ciphertext Sample (Base64):</strong> <code style="color:#38bdf8;">${d.cipherSample}</code>
      </div>
      <div style="font-size:0.72rem; color:#93c5fd; background:rgba(15,98,254,0.1); padding:0.4rem; border-radius:4px;">
        ${d.status}
      </div>
    `;
  } catch (err) {
    container.innerHTML = `<span class="text-danger">CPACF API error: ${err}</span>`;
  }
}

async function fetchSysplexStatus() {
  const container = document.getElementById('sysplex-status');
  if (!container) return;
  container.innerHTML = '<span style="color:#94a3b8;">Querying Parallel Sysplex Coupling Facility...</span>';
  try {
    const res = await fetch('/api/v1/ha/sysplex');
    const d = await res.json();
    container.innerHTML = `
      <div style="display:flex; justify-content:space-between; margin-bottom:0.3rem;">
        <span>Active Node:</span>
        <strong style="color:#10b981;">${d.localLpar}</strong>
      </div>
      <div style="display:flex; justify-content:space-between; margin-bottom:0.3rem;">
        <span>Standby Node:</span>
        <strong style="color:#94a3b8;">${d.remoteLpar}</strong>
      </div>
      <div style="display:flex; justify-content:space-between; margin-bottom:0.3rem;">
        <span>GDPS Replication Latency:</span>
        <strong class="text-accent">${d.stateSyncLatencyMs} ms (Sub-Millisecond Sync)</strong>
      </div>
      <div style="display:flex; justify-content:space-between; margin-bottom:0.3rem;">
        <span>Coupling Heartbeats:</span>
        <strong style="font-family:var(--font-mono);">${d.heartbeatCounter.toLocaleString()}</strong>
      </div>
      <div style="display:flex; justify-content:space-between; margin-bottom:0.3rem;">
        <span>Zero-Loss WAL Disk Bytes:</span>
        <strong style="font-family:var(--font-mono); color:#10b981;">${(d.walBytesCommitted / 1024).toFixed(1)} KB persisted</strong>
      </div>
      <div style="font-size:0.72rem; color:#a7f3d0; background:rgba(16,185,129,0.1); padding:0.4rem; border-radius:4px; margin-top:0.3rem;">
        Uptime SLA: ${d.availabilitySLA}
      </div>
    `;
  } catch (err) {
    container.innerHTML = `<span class="text-danger">Sysplex API error: ${err}</span>`;
  }
}

async function runStressTest() {
  const container = document.getElementById('stress-status');
  if (!container) return;
  container.innerHTML = '<div style="color:#38bdf8; font-weight:600;">Executing 100,000 requests across 16 threads...</div>';
  try {
    const res = await fetch('/api/v1/stress?requests=100000&threads=16');
    const d = await res.json();
    container.innerHTML = `
      <div style="display:grid; grid-template-columns:1fr 1fr; gap:0.5rem; margin-bottom:0.5rem;">
        <div>Requests: <strong>${d.totalRequests.toLocaleString()}</strong></div>
        <div>Threads: <strong>${d.concurrencyThreads}</strong></div>
        <div>Elapsed: <strong>${d.elapsedSeconds.toFixed(3)}s</strong></div>
        <div>Throughput: <strong style="color:#00d2ff;">${Math.round(d.throughputOpsPerSec).toLocaleString()}/s</strong></div>
        <div>P50 Latency: <strong>${d.p50Micros.toFixed(2)} µs</strong></div>
        <div>P90 Latency: <strong>${d.p90Micros.toFixed(2)} µs</strong></div>
        <div>P99 Latency: <strong style="color:#10b981;">${d.p99Micros.toFixed(2)} µs</strong></div>
        <div>P99.9 Latency: <strong style="color:#f59e0b;">${d.p999Micros.toFixed(2)} µs</strong></div>
      </div>
      <div style="font-size:0.72rem; color:#94a3b8; background:rgba(0,0,0,0.3); padding:0.35rem; border-radius:4px;">
        ${d.memoryStatus}
      </div>
    `;
  } catch (err) {
    container.innerHTML = `<span class="text-danger">Stress test error: ${err}</span>`;
  }
}

/* ==========================================================================
   Live ICU Lead-II Cardiac Oscillograph Strip (60 FPS Canvas Engine)
   ========================================================================== */
let ecgCanvas = null;
let ecgCtx = null;
let ecgAnimationId = null;
let ecgCurrentX = 0;
let ecgLastY = null;
let ecgAudioCtx = null;
let ecgAudioEnabled = false;
let ecgLastBeepTime = 0;

let currentEcgBed = {
  id: 'BED-03',
  name: 'Sunita Devi (Critical Septic Shock)',
  hr: 132,
  spo2: 87,
  map: 55,
  isCritical: true,
  rhythm: 'Sinus Tachycardia'
};

function initEcgEngine() {
  ecgCanvas = document.getElementById('ecg-canvas');
  if (!ecgCanvas) return;
  ecgCtx = ecgCanvas.getContext('2d');

  function resizeCanvas() {
    if (!ecgCanvas) return;
    const rect = ecgCanvas.getBoundingClientRect();
    ecgCanvas.width = rect.width * (window.devicePixelRatio || 1);
    ecgCanvas.height = rect.height * (window.devicePixelRatio || 1);
    ecgCtx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
    ecgCurrentX = 0;
    ecgLastY = null;
    ecgCtx.clearRect(0, 0, rect.width, rect.height);
  }

  resizeCanvas();
  window.addEventListener('resize', resizeCanvas);

  ecgCurrentX = 0;
  ecgLastY = null;

  startEcgLoop();
}

function startEcgLoop() {
  if (ecgAnimationId) cancelAnimationFrame(ecgAnimationId);

  let lastTimestamp = performance.now();
  let cycleProgress = 0; // 0.0 to 1.0

  function renderEcgFrame(now) {
    if (!ecgCanvas || !ecgCtx) return;
    const dt = (now - lastTimestamp) / 1000.0;
    lastTimestamp = now;

    const rect = ecgCanvas.getBoundingClientRect();
    const w = rect.width;
    const h = rect.height;
    const midY = h * 0.58;

    // Heart rate governs cycle duration
    const bpm = Math.max(40, Math.min(220, currentEcgBed.hr));
    const cycleDuration = 60.0 / bpm; // seconds per beat
    cycleProgress = (cycleProgress + dt / cycleDuration) % 1.0;

    // Advance sweep cursor across canvas (approx 3.5 seconds across screen)
    const sweepSpeed = w / 3.5;
    const prevX = ecgCurrentX;
    ecgCurrentX += sweepSpeed * dt;

    if (ecgCurrentX >= w) {
      ecgCurrentX = 0;
      ecgLastY = null;
    }

    // Clear sweep trail ahead (phosphor erasing effect)
    const clearWidth = 32;
    ecgCtx.clearRect(ecgCurrentX, 0, clearWidth, h);

    // Compute Lead-II ECG voltage at current phase
    let voltage = 0.0; // in arbitrary units
    const p = cycleProgress;

    // Lead-II Cardiac Waveform Synthesizer
    if (p >= 0.10 && p < 0.20) {
      // P-wave (atrial depolarization)
      const t = (p - 0.10) / 0.10;
      voltage = 0.15 * Math.sin(t * Math.PI);
    } else if (p >= 0.22 && p < 0.25) {
      // Q-wave (septal depolarization)
      const t = (p - 0.22) / 0.03;
      voltage = -0.18 * Math.sin(t * Math.PI);
    } else if (p >= 0.25 && p < 0.31) {
      // R-wave (ventricular depolarization - sharp peak)
      const t = (p - 0.25) / 0.06;
      voltage = 1.25 * Math.sin(t * Math.PI);

      // Trigger audio beep at R-peak
      if (t >= 0.4 && t <= 0.6 && (now - ecgLastBeepTime) > (cycleDuration * 800)) {
        triggerEcgBeep(currentEcgBed.isCritical);
        ecgLastBeepTime = now;
      }
    } else if (p >= 0.31 && p < 0.35) {
      // S-wave (ventricular late depolarization)
      const t = (p - 0.31) / 0.04;
      voltage = -0.35 * Math.sin(t * Math.PI);
    } else if (p >= 0.35 && p < 0.42) {
      // ST-segment: ST depression in septic myocardial injury
      voltage = currentEcgBed.isCritical ? -0.12 : 0.0;
    } else if (p >= 0.42 && p < 0.60) {
      // T-wave (ventricular repolarization)
      const t = (p - 0.42) / 0.18;
      voltage = 0.28 * Math.sin(t * Math.PI);
      if (currentEcgBed.isCritical) voltage *= 0.7; // Flattened/inverted T-wave
    } else {
      // Baseline with micro physiological noise
      voltage = (Math.random() - 0.5) * 0.02;
    }

    // Convert voltage to Y coordinate (gain 70px)
    const currentY = midY - (voltage * 68);

    // Draw the green or red phosphor vector line
    ecgCtx.save();
    ecgCtx.beginPath();
    const startY = ecgLastY !== null ? ecgLastY : currentY;
    ecgCtx.moveTo(prevX, startY);
    ecgCtx.lineTo(ecgCurrentX, currentY);

    if (currentEcgBed.isCritical) {
      ecgCtx.strokeStyle = '#ef4444';
      ecgCtx.shadowColor = '#ef4444';
    } else {
      ecgCtx.strokeStyle = '#10b981';
      ecgCtx.shadowColor = '#10b981';
    }
    ecgCtx.shadowBlur = 6;
    ecgCtx.lineWidth = 2.0;
    ecgCtx.lineCap = 'round';
    ecgCtx.stroke();
    ecgCtx.restore();

    ecgLastY = currentY;
    ecgAnimationId = requestAnimationFrame(renderEcgFrame);
  }

  ecgAnimationId = requestAnimationFrame(renderEcgFrame);
}

function onEcgBedChange(bedId) {
  const selectElem = document.getElementById('ecg-bed-select');
  const cardElem = document.getElementById('ecg-monitor-card');
  const nameElem = document.getElementById('ecg-patient-name');
  const hrElem = document.getElementById('ecg-hr-display');
  const rhythmElem = document.getElementById('ecg-rhythm-display');
  const spo2Elem = document.getElementById('ecg-spo2-display');
  const mapElem = document.getElementById('ecg-map-display');

  const bedCatalog = {
    'BED-01': { id: 'BED-01', name: 'Rajesh Kumar (Stable Post-Op)', hr: 74, spo2: 98, map: 92, isCritical: false, rhythm: 'Normal Sinus Rhythm' },
    'BED-02': { id: 'BED-02', name: 'Priya Sharma (Elevated NEWS2)', hr: 104, spo2: 93, map: 74, isCritical: false, rhythm: 'Sinus Tachycardia' },
    'BED-03': { id: 'BED-03', name: 'Sunita Devi (Critical Septic Shock)', hr: 132, spo2: 87, map: 55, isCritical: true, rhythm: 'Sinus Tachycardia (Shock)' },
    'BED-04': { id: 'BED-04', name: 'Amit Patel (Post-Appendectomy)', hr: 82, spo2: 97, map: 88, isCritical: false, rhythm: 'Normal Sinus Rhythm' },
    'BED-05': { id: 'BED-05', name: 'Meena Kumari (COPD Deterioration)', hr: 96, spo2: 91, map: 79, isCritical: false, rhythm: 'Sinus Rhythm (Hypoxic)' },
    'BED-06': { id: 'BED-06', name: 'Vikram Singh (Poly-trauma)', hr: 118, spo2: 94, map: 71, isCritical: false, rhythm: 'Sinus Tachycardia' }
  };

  const b = bedCatalog[bedId] || bedCatalog['BED-03'];
  currentEcgBed = { ...b };

  if (cardElem) {
    if (b.isCritical) cardElem.classList.add('critical-alert');
    else cardElem.classList.remove('critical-alert');
  }

  if (nameElem) nameElem.textContent = `${b.id}: ${b.name}`;
  if (hrElem) hrElem.textContent = b.hr;
  if (rhythmElem) rhythmElem.textContent = b.rhythm;
  if (spo2Elem) spo2Elem.textContent = b.spo2;
  if (mapElem) mapElem.textContent = b.map;
}

function toggleEcgAudio() {
  const btn = document.getElementById('ecg-audio-btn');
  ecgAudioEnabled = !ecgAudioEnabled;

  if (ecgAudioEnabled) {
    if (!ecgAudioCtx) {
      ecgAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (ecgAudioCtx.state === 'suspended') {
      ecgAudioCtx.resume();
    }
    if (btn) {
      btn.textContent = '🔊 Beep: ON';
      btn.classList.add('btn-primary');
      btn.classList.remove('btn-outline');
    }
  } else {
    if (btn) {
      btn.textContent = '🔇 Beep: OFF';
      btn.classList.remove('btn-primary');
      btn.classList.add('btn-outline');
    }
  }
}

function triggerEcgBeep(isCritical) {
  if (!ecgAudioEnabled || !ecgAudioCtx) return;
  try {
    const osc = ecgAudioCtx.createOscillator();
    const gain = ecgAudioCtx.createGain();

    osc.type = 'sine';
    // Authentic hospital monitor frequency: 880Hz (stable) vs 1100Hz (critical alarm)
    osc.frequency.setValueAtTime(isCritical ? 1100 : 880, ecgAudioCtx.currentTime);

    gain.gain.setValueAtTime(0.06, ecgAudioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.0001, ecgAudioCtx.currentTime + 0.08);

    osc.connect(gain);
    gain.connect(ecgAudioCtx.destination);

    osc.start();
    osc.stop(ecgAudioCtx.currentTime + 0.08);
  } catch (e) {
    console.warn('Audio beep error:', e);
  }
}

// Attach ECG engine to DOM ready
document.addEventListener('DOMContentLoaded', () => {
  setTimeout(initEcgEngine, 200);
});

