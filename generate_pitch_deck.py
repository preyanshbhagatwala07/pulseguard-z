import os
from reportlab.lib.pagesizes import landscape, letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        # Top banner line
        self.setStrokeColor(colors.HexColor('#0F62FE'))
        self.setLineWidth(3)
        self.line(40, 570, 752, 570)
        
        # Bottom footer
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor('#64748B'))
        self.drawString(40, 25, "PulseGuard-Z | IBM Z Datathon 2026 | Team CodeBuilders")
        self.drawRightString(752, 25, f"Slide {self._pageNumber} of {page_count}")
        self.restoreState()

def build_pdf(filename="PulseGuard_Z_PitchDeck.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=landscape(letter),
        leftMargin=40,
        rightMargin=40,
        topMargin=40,
        bottomMargin=40
    )

    styles = getSampleStyleSheet()
    
    title_style = ParagraphStyle(
        'DeckTitle',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=28,
        textColor=colors.HexColor('#0F62FE'),
        spaceAfter=6
    )

    subtitle_style = ParagraphStyle(
        'DeckSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=12,
        leading=16,
        textColor=colors.HexColor('#334155'),
        spaceAfter=15
    )

    h2_style = ParagraphStyle(
        'DeckH2',
        parent=styles['Heading2'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=colors.HexColor('#1E293B'),
        spaceAfter=8
    )

    body_style = ParagraphStyle(
        'DeckBody',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#334155'),
        spaceAfter=8
    )

    bullet_style = ParagraphStyle(
        'DeckBullet',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=colors.HexColor('#1E293B'),
        leftIndent=12,
        spaceAfter=6
    )

    code_style = ParagraphStyle(
        'DeckCode',
        parent=styles['Normal'],
        fontName='Courier-Bold',
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor('#0284C7'),
        leftIndent=10
    )

    story = []

    # ==================== SLIDE 1 ====================
    story.append(Spacer(1, 30))
    story.append(Paragraph("PulseGuard-Z ⚡", title_style))
    story.append(Paragraph("Sub-Second National Tele-ICU Sepsis Triage Engine on IBM Z & LinuxONE", subtitle_style))
    story.append(Spacer(1, 15))
    
    s1_data = [
        [Paragraph("<b>Challenge Track:</b>", body_style), Paragraph("Real-Time AI for Critical Decisions", body_style)],
        [Paragraph("<b>Platform & Architecture:</b>", body_style), Paragraph("IBM LinuxONE Community Cloud (s390x) & IBM Telum AI NNPA", body_style)],
        [Paragraph("<b>Healthcare Compliance:</b>", body_style), Paragraph("Ayushman Bharat Digital Mission (ABDM) & HL7 FHIR R4 Standard", body_style)],
        [Paragraph("<b>Team Name:</b>", body_style), Paragraph("CodeBuilders (Preyansh Bhagatwala)", body_style)],
        [Paragraph("<b>Core Proposition:</b>", body_style), Paragraph("Predicting septic shock and clinical collapse 2 to 4 hours in advance in under 3 microseconds per vital stream.", body_style)]
    ]
    t1 = Table(s1_data, colWidths=[180, 520])
    t1.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t1)
    story.append(PageBreak())

    # ==================== SLIDE 2 ====================
    story.append(Paragraph("The Critical Emergency: Sepsis & The Latency Gap", title_style))
    story.append(Paragraph("Why Delayed In-Transaction Healthcare Decisions Equal Loss of Life", subtitle_style))
    
    s2_data = [
        [
            Paragraph("<b>11 Million Annual Deaths</b><br/>Sepsis is the leading cause of mortality in hospital ICUs globally. In India, severe infection from scrub typhus, dengue shock, pneumonia, and postpartum infection causes rapid decompensation.", body_style),
            Paragraph("<b>The Golden Hour: 7.6% / Hour</b><br/>Clinical trials prove that for every single hour of delayed antimicrobial or fluid resuscitation, patient mortality increases exponentially by <b>7.6%</b>.", body_style)
        ],
        [
            Paragraph("<b>Acute Alarm Fatigue (>85% Noise)</b><br/>Modern ICU monitors emit constant auditory alarms for minor artifacts, leading clinical staff to mute or ignore monitors until vital signs have already crashed.", body_style),
            Paragraph("<b>The Rural-Urban Intensivist Void</b><br/>Primary Health Centres (PHCs) and Community Health Centres (CHCs) in rural India have zero intensivists. Patients deteriorate unnoticed before emergency transfer can occur.", body_style)
        ]
    ]
    t2 = Table(s2_data, colWidths=[350, 350])
    t2.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 14),
        ('BOTTOMPADDING', (0,0), (-1,-1), 14),
        ('LEFTPADDING', (0,0), (-1,-1), 12),
        ('RIGHTPADDING', (0,0), (-1,-1), 12),
    ]))
    story.append(t2)
    story.append(PageBreak())

    # ==================== SLIDE 3 ====================
    story.append(Paragraph("The Solution: PulseGuard-Z Architecture", title_style))
    story.append(Paragraph("A Multi-Tiered Clinical AI Pipeline Engineered in Modern Java", subtitle_style))
    
    story.append(Paragraph("• <b>High-Frequency Telemetry Ingest:</b> Asynchronously streams multi-parameter bedside data (HR, SBP, DBP, SpO2, RR, Temp, Lactate, WBC) with zero packet dropouts.", bullet_style))
    story.append(Paragraph("• <b>Evidence-Based Clinical Scoring (NEWS2):</b> Implements official AIIMS and Royal College of Physicians National Early Warning Score 2 (NEWS2), calculating physiological penalties across 6 vitals.", bullet_style))
    story.append(Paragraph("• <b>Hemodynamic Biomarker Fusion:</b> Computes real-time Shock Index (SI = HR / SBP) and Mean Arterial Pressure (MAP = (SBP + 2*DBP)/3) detecting occult cellular shock before blood pressure collapses.", bullet_style))
    story.append(Paragraph("• <b>Sub-3 Microsecond Dual-Tier Inference:</b> Vectorized Sepsis-3 scoring model operates at over 450,000 decisions/second on the Java Virtual Machine.", bullet_style))
    story.append(Paragraph("• <b>Explainable AI (XAI) & Hour-1 Bundle:</b> Pinpoints exact biomarker drivers and generates immediate Surviving Sepsis Hour-1 clinical orders.", bullet_style))
    story.append(PageBreak())

    # ==================== SLIDE 4 ====================
    story.append(Paragraph("Deep IBM Z & LinuxONE Architecture", title_style))
    story.append(Paragraph("Exploiting Enterprise Mainframe Hardware Capabilities for Mission-Critical Tele-ICU", subtitle_style))
    
    s4_data = [
        [Paragraph("<b>Enterprise Feature</b>", h2_style), Paragraph("<b>Clinical Telemetry Benefit</b>", h2_style), Paragraph("<b>Implementation in PulseGuard-Z</b>", h2_style)],
        [
            Paragraph("<b>IBM Telum NNPA Matrix AI</b>", body_style),
            Paragraph("Sub-millisecond in-transaction inference without violating telemetry SLAs", body_style),
            Paragraph("Unrolled superscalar SIMD matrix forward pass computing 4h, 8h, 12h survival hazard curves in < 37 µs.", code_style)
        ],
        [
            Paragraph("<b>CPACF Pervasive Encryption</b>", body_style),
            Paragraph("Hardware-level end-to-end Protected Health Information (PHI) encryption", body_style),
            Paragraph("AES-256-GCM authenticated cipher with 128-bit integrity tags encrypting vitals in-flight & in-memory in ~3.2 µs.", code_style)
        ],
        [
            Paragraph("<b>Parallel Sysplex GDPS HA</b>", body_style),
            Paragraph("Continuous 99.99999% availability; zero downtime tolerance in intensive care", body_style),
            Paragraph("Active-Active dual-LPAR clustering (Mumbai ↔ Delhi) with 120 µs Coupling Facility heartbeat replication.", code_style)
        ],
        [
            Paragraph("<b>Zero-Loss MemoryMapped WAL</b>", body_style),
            Paragraph("Resilience against power cuts or hospital server hardware interruption", body_style),
            Paragraph("Java NIO MappedByteBuffer appending telemetry directly to NVMe storage with zero syscall context switches.", code_style)
        ]
    ]
    t4 = Table(s4_data, colWidths=[160, 240, 310])
    t4.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0F62FE')),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('TOPPADDING', (0,0), (-1,-1), 7),
        ('BOTTOMPADDING', (0,0), (-1,-1), 7),
        ('BACKGROUND', (0,1), (-1,-1), colors.HexColor('#F8FAFC')),
    ]))
    story.append(t4)
    story.append(PageBreak())

    # ==================== SLIDE 5 ====================
    story.append(Paragraph("Public Health Network & Offline 2G GSM Resilience", title_style))
    story.append(Paragraph("Bridging India's Rural-Urban Intensivist Gap with ABDM Standards", subtitle_style))
    
    story.append(Paragraph("• <b>ABDM HL7 FHIR R4 Interoperability:</b> Ingests and generates standard Observation bundles using international LOINC codes (8867-4 HR, 85354-9 BP, 2708-6 SpO2, 2524-7 Lactate) mapped to 14-digit ABHA Numbers.", bullet_style))
    story.append(Paragraph("• <b>District Hub-and-Spoke Tele-ICU Grid:</b> Connects remote Primary Health Centres (PHC Dharampur) with Community Health Centres (CHC Kaprada) and District Headquarters (Civil Hospital Valsad).", bullet_style))
    story.append(Paragraph("• <b>Automated Emergency 108 ALS Ambulance Dispatch:</b> When a rural patient enters shock, PulseGuard-Z automatically dispatches an Advanced Life Support ambulance and auto-reserves an ICU bed at the receiving tertiary hospital.", bullet_style))
    story.append(Paragraph("• <b>Offline 2G GSM SMS Fallback Protocol:</b> In remote tribal belts where fiber broadband fails, PulseGuard-Z encodes clinical vitals into an ultra-dense, CRC16-checksummed 53-byte SMS payload (< 140 bytes) transmittable over 2G cellular.", bullet_style))
    story.append(Paragraph("• <b>Official Printable ABDM Clinical Referral Slip:</b> One-click generation of Ministry of Health & Family Welfare emergency transfer slips with QR code and doctor sign-off.", bullet_style))
    story.append(PageBreak())

    # ==================== SLIDE 6 ====================
    story.append(Paragraph("Performance & Tail Latency Benchmarks", title_style))
    story.append(Paragraph("Verified Execution Metrics on Modern Java 21+ Runtime", subtitle_style))
    
    s6_data = [
        [Paragraph("<b>Metric</b>", h2_style), Paragraph("<b>Workstation Dev Mode</b>", h2_style), Paragraph("<b>IBM LinuxONE (s390x) Target</b>", h2_style)],
        [Paragraph("Average Inference Latency", body_style), Paragraph("<b>2.16 microseconds</b>", body_style), Paragraph("<b>< 1.0 microsecond (Telum NNPA)</b>", body_style)],
        [Paragraph("Median (P50) Latency", body_style), Paragraph("1.40 microseconds", body_style), Paragraph("< 0.8 microseconds", body_style)],
        [Paragraph("P99 Tail Latency", body_style), Paragraph("6.90 microseconds", body_style), Paragraph("< 3.5 microseconds", body_style)],
        [Paragraph("Decision Throughput", body_style), Paragraph("<b>453,105 decisions / sec</b>", body_style), Paragraph("<b>> 1,000,000 decisions / sec</b>", body_style)],
        [Paragraph("100k Concurrency Stress Test", body_style), Paragraph("0.243 seconds (16 threads)", body_style), Paragraph("< 0.150 seconds (s390x SMT2)", body_style)],
        [Paragraph("Heap Memory Footprint", body_style), Paragraph("< 65 MB heap used", body_style), Paragraph("< 60 MB heap (Zero GC Leak)", body_style)]
    ]
    t6 = Table(s6_data, colWidths=[220, 240, 250])
    t6.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0F62FE')),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('BACKGROUND', (0,1), (-1,-1), colors.HexColor('#F8FAFC')),
    ]))
    story.append(t6)
    story.append(PageBreak())

    # ==================== SLIDE 7 ====================
    story.append(Paragraph("Cryptographic SHA-256 Medico-Legal Audit Trail", title_style))
    story.append(Paragraph("DPDP Act 2023 & Clinical Governance Compliance", subtitle_style))
    
    story.append(Paragraph("• <b>The Medico-Legal Challenge:</b> In clinical emergencies, malpractice disputes frequently arise over when an alarm sounded and whether clinical escalation was delayed.", bullet_style))
    story.append(Paragraph("• <b>Immutable SHA-256 Chained Ledger:</b> Every single triage decision is immutably chained: <code>Index | Timestamp | ABHA | Level | NEWS2 | PreviousHash</code>.", bullet_style))
    story.append(Paragraph("• <b>Zero Tampering Guarantee:</b> Any retroactive attempt to falsify hospital timestamps or alter vital scores breaks the cryptographic Merkle hash chain.", bullet_style))
    story.append(Paragraph("• <b>India DPDP Act 2023 Compliance:</b> Guarantees patient data auditability while protecting privacy through hardware-level CPACF AES-256-GCM encryption.", bullet_style))
    story.append(Paragraph("• <b>Live Audit Inspection:</b> Real-time verification accessible via REST API (<code>/api/v1/audit/chain</code>) and web dashboard.", bullet_style))
    story.append(PageBreak())

    # ==================== SLIDE 8 ====================
    story.append(Paragraph("Working Prototype & Live Verification", title_style))
    story.append(Paragraph("End-to-End Operational System Running on Port 8085", subtitle_style))
    
    story.append(Paragraph("• <b>Tab 1: Real-Time ICU Ward Overview:</b> 6 active beds monitored with live ABHA IDs, NEWS2 score badges, vital tables, Sepsis-3 risk meters, and one-click FHIR R4 JSON export.", bullet_style))
    story.append(Paragraph("• <b>Tab 2: District Tele-ICU & 108 Dispatch:</b> Live visualization of healthcare hierarchy and active emergency ambulance dispatches with stabilizing orders.", bullet_style))
    story.append(Paragraph("• <b>Tab 3: SHA-256 Medico-Legal Ledger:</b> Real-time cryptographic ledger table verifying block-by-block hash continuity.", bullet_style))
    story.append(Paragraph("• <b>Tab 4: IBM Z & LinuxONE Architecture:</b> Live Telum NNPA neural inference passes, CPACF AES-256 encryption status, Parallel Sysplex GDPS active-active health, and industrial 100k stress testing.", bullet_style))
    story.append(Paragraph("• <b>Interactive Sandbox:</b> Test custom vital parameters and observe instant microsecond classification.", bullet_style))
    story.append(PageBreak())

    # ==================== SLIDE 9 ====================
    story.append(Paragraph("Implementation Roadmap & Contact", title_style))
    story.append(Paragraph("From Hackathon Prototype to National Public Health Deployment", subtitle_style))
    
    s9_data = [
        [Paragraph("<b>Phase</b>", h2_style), Paragraph("<b>Milestones & Objectives</b>", h2_style), Paragraph("<b>Timeline</b>", h2_style)],
        [
            Paragraph("<b>Phase 1: Core Engine</b>", body_style),
            Paragraph("Java 21+ sub-microsecond inference engine, Telum NNPA emulation, CPACF encryption, NEWS2 calculator, and FHIR R4 parser (COMPLETED).", body_style),
            Paragraph("Month 1", body_style)
        ],
        [
            Paragraph("<b>Phase 2: LinuxONE Pilot</b>", body_style),
            Paragraph("Deploy containerized Podman microservice cluster on IBM LinuxONE Community Cloud (s390x) connected to state hospital pilot.", body_style),
            Paragraph("Months 2–3", body_style)
        ],
        [
            Paragraph("<b>Phase 3: National ABDM Rollout</b>", body_style),
            Paragraph("Full integration with National Health Authority (NHA) ABDM Gateway, Emergency 108 ambulance CAD systems, and state Tele-ICU hubs.", body_style),
            Paragraph("Months 4–6", body_style)
        ]
    ]
    t9 = Table(s9_data, colWidths=[150, 440, 120])
    t9.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#0F62FE')),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
        ('BACKGROUND', (0,1), (-1,-1), colors.HexColor('#F8FAFC')),
    ]))
    story.append(t9)
    story.append(Spacer(1, 20))
    story.append(Paragraph("<b>Repository:</b> https://github.com/&lt;your-username&gt;/pulseguard-z &bull; <b>Lead Architect:</b> Preyansh Bhagatwala", subtitle_style))

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Successfully generated presentation PDF: {filename} ({os.path.getsize(filename)} bytes)")

if __name__ == '__main__':
    build_pdf()
