// PulseGuard-Z Dashboard Client Logic

const PRESETS = {
  normal: { hr: 74, sbp: 118, dbp: 76, rr: 15, temp: 36.8, spo2: 98, lactate: 1.1, wbc: 7.8 },
  elevated: { hr: 104, sbp: 98, dbp: 62, rr: 23, temp: 38.6, spo2: 93, lactate: 2.8, wbc: 14.5 },
  shock: { hr: 132, sbp: 78, dbp: 44, rr: 29, temp: 39.4, spo2: 87, lactate: 5.2, wbc: 21.0 }
};

document.addEventListener('DOMContentLoaded', () => {
  fetchSystemInfo();
  pollBeds();
  setInterval(pollBeds, 1500);
});

async function fetchSystemInfo() {
  try {
    const res = await fetch('/api/v1/system');
    const data = await res.json();
    const archElem = document.getElementById('platform-arch');
    if (archElem) {
      if (data.isLinuxONE) {
        archElem.textContent = 'IBM LinuxONE s390x (Native Mainframe)';
      } else {
        archElem.textContent = `Dev Runtime: ${data.architecture} (s390x Portable)`;
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
    renderBeds(data.beds);
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
    const levelClass = a.level === 'CRITICAL_SEPSIS' ? 'critical' : (a.level === 'ELEVATED_RISK' ? 'elevated' : 'stable');
    const riskPercent = Math.round(a.riskScore * 100);

    const isHrAlarm = v.heartRate > 105;
    const isBpAlarm = v.systolicBp < 90 || v.map < 65;
    const isLactAlarm = v.lactate >= 2.0;

    return `
      <div class="bed-card status-${levelClass}">
        <div class="bed-header">
          <div>
            <div class="bed-id">${bed.bedId}</div>
            <div class="patient-info">${bed.patientName}, ${bed.age}y &bull; ${bed.diagnosis}</div>
          </div>
          <span class="status-tag ${levelClass}">${a.level.replace('_', ' ')}</span>
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
            <span>Sepsis Risk Score</span>
            <strong>${riskPercent}% &bull; 4h Mortality: ${Math.round(a.fourHourMortalityRisk * 100)}%</strong>
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
      </div>
    `;
  }).join('');
}

function loadPreset(type) {
  const p = PRESETS[type];
  if (!p) return;
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
    patientId: 'P-SIM-DEMO',
    bedId: 'BED-SIMULATOR',
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
        <strong>Decision: <span class="status-tag ${result.triageLevel.toLowerCase().replace('_', '-')}">${result.triageLevel}</span></strong>
        <div style="font-size:0.75rem; color:#94a3b8; margin-top:0.2rem;">
          Sepsis Risk: <strong>${Math.round(result.sepsisRiskScore * 100)}%</strong> &bull; MAP: ${result.meanArterialPressure} mmHg &bull; Shock Index: ${result.shockIndex}
        </div>
      </div>
      <div style="text-align:right;">
        <span class="text-accent" style="font-family:var(--font-mono); font-size:1.1rem; font-weight:700;">${result.inferenceLatencyMicros} µs</span>
        <div style="font-size:0.7rem; color:#94a3b8;">Latency</div>
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
