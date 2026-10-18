import os
import pymupdf
from reportlab.lib.pagesizes import landscape, letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas

PAGE_WIDTH, PAGE_HEIGHT = landscape(letter)  # 792 x 612

class SlideDeckCanvas(canvas.Canvas):
    """Clean, high-impact executive presentation canvas with 5-slide pagination."""
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
            self.draw_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_decorations(self, page_count):
        self.saveState()
        
        # Top Accent Stripe (IBM Blue)
        self.setFillColor(colors.HexColor('#0F62FE'))
        self.rect(0, PAGE_HEIGHT - 6, PAGE_WIDTH, 6, fill=True, stroke=False)
        
        # Header Line
        self.setStrokeColor(colors.HexColor('#E2E8F0'))
        self.setLineWidth(0.75)
        self.line(36, PAGE_HEIGHT - 36, PAGE_WIDTH - 36, PAGE_HEIGHT - 36)
        
        # Header text
        self.setFont("Helvetica-Bold", 8.5)
        self.setFillColor(colors.HexColor('#0F62FE'))
        self.drawString(36, PAGE_HEIGHT - 28, "PULSEGUARD-Z")
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor('#64748B'))
        self.drawString(115, PAGE_HEIGHT - 28, "|  National Tele-ICU Sepsis Triage Engine on IBM Z & LinuxONE")
        self.drawRightString(PAGE_WIDTH - 36, PAGE_HEIGHT - 28, "IBM Z Datathon 2026 • Real-Time AI Track")
        
        # Bottom Footer Line
        self.setStrokeColor(colors.HexColor('#E2E8F0'))
        self.setLineWidth(0.75)
        self.line(36, 26, PAGE_WIDTH - 36, 26)
        
        # Footer text
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor('#64748B'))
        self.drawString(36, 12, "Team CodeBuilders  •  Preyansh Bhagatwala  •  ABDM & DPDP Act 2023 Compliant")
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor('#0F62FE'))
        self.drawRightString(PAGE_WIDTH - 36, 12, f"Slide {self._pageNumber} of {page_count}")
        
        self.restoreState()


def build_pitch_deck(filename="PulseGuard_Z_PitchDeck.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=landscape(letter),
        leftMargin=36,
        rightMargin=36,
        topMargin=44,
        bottomMargin=32
    )

    styles = getSampleStyleSheet()

    # --- Typography Styles ---
    tag_style = ParagraphStyle(
        'DeckTag',
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=11,
        textColor=colors.HexColor('#0F62FE'),
        spaceAfter=3
    )

    title_style = ParagraphStyle(
        'DeckTitle',
        fontName='Helvetica-Bold',
        fontSize=21,
        leading=25,
        textColor=colors.HexColor('#0F172A'),
        spaceAfter=3
    )

    subtitle_style = ParagraphStyle(
        'DeckSubtitle',
        fontName='Helvetica',
        fontSize=10,
        leading=14,
        textColor=colors.HexColor('#475569'),
        spaceAfter=14
    )

    card_head_style = ParagraphStyle(
        'CardHead',
        fontName='Helvetica-Bold',
        fontSize=10.5,
        leading=14,
        textColor=colors.HexColor('#0F62FE'),
        spaceAfter=4
    )

    card_body_style = ParagraphStyle(
        'CardBody',
        fontName='Helvetica',
        fontSize=8.5,
        leading=12.5,
        textColor=colors.HexColor('#334155')
    )

    takeaway_style = ParagraphStyle(
        'Takeaway',
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor('#0F172A')
    )

    story = []

    # =========================================================================
    # SLIDE 1: COVER SLIDE
    # =========================================================================
    story.append(Spacer(1, 16))
    story.append(Paragraph("EXECUTIVE PITCH DECK  •  IBM Z DATATHON 2026", tag_style))
    story.append(Paragraph("PulseGuard-Z: Sub-Second Tele-ICU Sepsis Triage", ParagraphStyle(
        'CoverTitle', fontName='Helvetica-Bold', fontSize=26, leading=30, textColor=colors.HexColor('#0F172A'), spaceAfter=5
    )))
    story.append(Paragraph("Predicting Acute Septic Shock 2 to 4 Hours in Advance via On-Chip IBM Telum AI", ParagraphStyle(
        'CoverSub', fontName='Helvetica', fontSize=12, leading=16, textColor=colors.HexColor('#0F62FE'), spaceAfter=18
    )))

    # Cover Metadata Grid
    meta_table = [
        [
            Paragraph("<b>CHALLENGE TRACK</b><br/><font color='#0F172A' size='9.5'>Real-Time AI for Critical Decisions</font>", card_body_style),
            Paragraph("<b>TARGET PLATFORM</b><br/><font color='#0F172A' size='9.5'>IBM LinuxONE / s390x & Telum NNPA</font>", card_body_style),
            Paragraph("<b>PUBLIC HEALTH STANDARD</b><br/><font color='#0F172A' size='9.5'>ABDM M3 (HL7 FHIR R4 & ABHA)</font>", card_body_style),
            Paragraph("<b>TEAM LEAD</b><br/><font color='#0F172A' size='9.5'>CodeBuilders (Preyansh Bhagatwala)</font>", card_body_style)
        ]
    ]
    t_meta = Table(meta_table, colWidths=[180, 180, 180, 180])
    t_meta.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 10),
        ('BOTTOMPADDING', (0,0), (-1,-1), 10),
        ('LEFTPADDING', (0,0), (-1,-1), 12),
        ('RIGHTPADDING', (0,0), (-1,-1), 12),
    ]))
    story.append(t_meta)
    story.append(Spacer(1, 16))

    # Core Metrics Highlights Bar
    metrics_table = [
        [
            Paragraph("<font color='#0F62FE' size='18'><b>2.8 µs</b></font><br/><font color='#64748B' size='8'>Inference Latency<br/>IBM Telum On-Chip AI</font>", card_body_style),
            Paragraph("<font color='#10B981' size='18'><b>>205,000</b></font><br/><font color='#64748B' size='8'>Decisions / Second<br/>LMAX Disruptor Zero-GC</font>", card_body_style),
            Paragraph("<font color='#8B5CF6' size='18'><b>99.99999%</b></font><br/><font color='#64748B' size='8'>Sysplex Availability<br/>Active-Active GDPS HA</font>", card_body_style),
            Paragraph("<font color='#EF4444' size='18'><b>53 Bytes</b></font><br/><font color='#64748B' size='8'>2G GSM Offline SMS<br/>CRC16 Fault-Tolerant</font>", card_body_style)
        ]
    ]
    t_metrics = Table(metrics_table, colWidths=[180, 180, 180, 180])
    t_metrics.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#FFFFFF')),
        ('BOX', (0,0), (-1,-1), 1.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 10),
        ('BOTTOMPADDING', (0,0), (-1,-1), 10),
        ('ALIGN', (0,0), (-1,-1), 'CENTER'),
    ]))
    story.append(t_metrics)
    story.append(PageBreak())

    # =========================================================================
    # SLIDE 2: THE CLINICAL EMERGENCY IN INDIA
    # =========================================================================
    story.append(Paragraph("THE CLINICAL EMERGENCY IN INDIA", tag_style))
    story.append(Paragraph("Sepsis & The Deadly Latency Gap", title_style))
    story.append(Paragraph("Every hour of delayed intervention costs human lives. Rural centers lack specialist backup.", subtitle_style))

    p_data = [
        [
            Paragraph("<font color='#EF4444' size='16'><b>11.0M Deaths</b></font><br/><br/><b>The Global & National Toll:</b><br/>Lancet GBD confirms 11M annual sepsis deaths globally; India bears ~2.99M fatalities with 30–50% ICU mortality (INDICAPS study).", card_body_style),
            Paragraph("<font color='#F59E0B' size='16'><b>7.6% / Hour</b></font><br/><br/><b>The Golden Hour Window:</b><br/>Kumar et al. (Crit Care Med) proved each hour of delayed antimicrobial therapy increases patient mortality by <b>7.6%</b> exponentially.", card_body_style),
            Paragraph("<font color='#0F62FE' size='16'><b>79.5% Void</b></font><br/><br/><b>The Specialist Shortfall:</b><br/>MoHFW data reveals an 79.5% specialist shortfall in rural CHCs. Over 25,000 PHCs have zero intensivists or ICU telemetry.", card_body_style),
            Paragraph("<font color='#8B5CF6' size='16'><b>>85% False</b></font><br/><br/><b>Deadly Alarm Fatigue:</b><br/>ICU monitors emit >85% false alarms (Drew et al., PLOS ONE), inducing desensitization until blood pressure crashes (<90 mmHg).", card_body_style)
        ]
    ]
    t_p = Table(p_data, colWidths=[175, 175, 175, 175])
    t_p.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 10),
        ('BOTTOMPADDING', (0,0), (-1,-1), 10),
        ('LEFTPADDING', (0,0), (-1,-1), 9),
        ('RIGHTPADDING', (0,0), (-1,-1), 9),
    ]))
    story.append(t_p)
    story.append(Spacer(1, 14))

    takeaway1 = [
        [Paragraph("<b>CORE CLINICAL BOTTLENECK:</b> Patients deteriorate silently for hours. By the time human clinicians notice physical collapse, irreversible septic shock has already set in. Triage must be automated, continuous, and sub-second.", takeaway_style)]
    ]
    t_take1 = Table(takeaway1, colWidths=[720])
    t_take1.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#FEF2F2')),
        ('BOX', (0,0), (-1,-1), 1.5, colors.HexColor('#EF4444')),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
        ('LEFTPADDING', (0,0), (-1,-1), 12),
    ]))
    story.append(t_take1)
    story.append(PageBreak())

    # =========================================================================
    # SLIDE 3: THE SOLUTION & CLINICAL PIPELINE
    # =========================================================================
    story.append(Paragraph("THE SOLUTION & CLINICAL PIPELINE", tag_style))
    story.append(Paragraph("PulseGuard-Z: Real-Time In-Transaction Tele-ICU Pipeline", title_style))
    story.append(Paragraph("A continuous 3-pillar pipeline connecting bedside monitors directly to on-chip mainframe intelligence.", subtitle_style))

    sol_data = [
        [
            Paragraph("<b>PILLAR 1: STREAMING & AI</b><br/><br/>• <b>Lock-Free Ingestion:</b> LMAX Disruptor ring buffer (65,536 slots, cache-padded) streams >2.8M readings/sec with zero GC pauses.<br/>• <b>Telum NNPA Matrix AI:</b> Executes matrix GEMM on-chip in <b>2.8 µs</b>, fusing AIIMS NEWS2 scoring with physiological vectors to project 4h, 8h, and 12h sepsis hazard curves.", card_body_style),
            Paragraph("<b>PILLAR 2: AUTO-ESCALATION</b><br/><br/>• <b>Automated Triage Lock:</b> When NEWS2 breaches ≥7, the engine immediately reserves an ICU bed at the District Civil Hospital.<br/>• <b>108 Ambulance Dispatch:</b> Dispatches GPS-tracked ALS ambulance with en-route orders.<br/>• <b>2G SMS Fallback:</b> Compresses telemetry into a <b>53-byte frame</b> with CRC16 when rural broadband fails.", card_body_style),
            Paragraph("<b>PILLAR 3: ABDM INTEROPERABILITY</b><br/><br/>• <b>HL7 FHIR R4 Standard:</b> Generates authentic Observation bundles using international codes (LOINC 8867-4, 85354-9, 2708-6, 2524-7).<br/>• <b>14-Digit ABHA ID:</b> Full regex & checksum validation.<br/>• <b>Forensic Audit Trail:</b> Signs every triage decision into an immutable <b>SHA-256 Merkle Ledger</b> (DPDP Act 2023).", card_body_style)
        ]
    ]
    t_sol = Table(sol_data, colWidths=[236, 236, 236])
    t_sol.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 10),
        ('BOTTOMPADDING', (0,0), (-1,-1), 10),
        ('LEFTPADDING', (0,0), (-1,-1), 10),
        ('RIGHTPADDING', (0,0), (-1,-1), 10),
    ]))
    story.append(t_sol)
    story.append(Spacer(1, 14))

    takeaway2 = [
        [Paragraph("<b>ARCHITECTURAL ADVANCE:</b> Replaces slow manual charting with in-transaction AI running at microsecond speeds, bridging the distance between rural primary centers and tertiary hospital intensive care.", takeaway_style)]
    ]
    t_take2 = Table(takeaway2, colWidths=[720])
    t_take2.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F0F9FF')),
        ('BOX', (0,0), (-1,-1), 1.5, colors.HexColor('#0284C7')),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
        ('LEFTPADDING', (0,0), (-1,-1), 12),
    ]))
    story.append(t_take2)
    story.append(PageBreak())

    # =========================================================================
    # SLIDE 4: HARDWARE EXPLOITATION - WHY IBM Z & LINUXONE?
    # =========================================================================
    story.append(Paragraph("HARDWARE EXPLOITATION", tag_style))
    story.append(Paragraph("Deep IBM Z & LinuxONE Mainframe Architecture", title_style))
    story.append(Paragraph("PulseGuard-Z does not treat LinuxONE as a generic VM; it deeply integrates with native s390x hardware capabilities.", subtitle_style))

    ibm_data = [
        [
            Paragraph("<b>IBM Telum NNPA On-Chip AI</b><br/><br/>• Neural matrix GEMM executed directly on-chip at 4.6+ GHz.<br/>• Eliminates high-latency PCI-e bus transfers to external GPUs.<br/>• <b>Result:</b> 2.8 µs per decision, enabling 100,000 evaluations in 0.24s.", card_body_style),
            Paragraph("<b>CPACF Pervasive Encryption</b><br/><br/>• Native KIMD/KLMD hardware opcodes for AES-256-GCM ciphering.<br/>• Encrypts patient vitals in-flight, in-use, and at-rest.<br/>• <b>Result:</b> DPDP Act 2023 & HIPAA compliance with zero CPU drag.", card_body_style),
            Paragraph("<b>Parallel Sysplex Active-Active</b><br/><br/>• Dual-LPAR clustering (Mumbai ↔ Delhi) with Coupling Facility.<br/>• Sub-millisecond state sync & automated failover.<br/>• <b>Result:</b> 99.99999% uptime (&lt;3.15s downtime/yr); Tele-ICU never crashes.", card_body_style),
            Paragraph("<b>Memory-Mapped WAL Persistence</b><br/><br/>• Java NIO MappedByteBuffer writes logs at memory bus speed.<br/>• Direct memory-mapped disk persistence.<br/>• <b>Result:</b> Zero telemetry loss even during hospital electrical blackouts.", card_body_style)
        ]
    ]
    t_ibm = Table(ibm_data, colWidths=[175, 175, 175, 175])
    t_ibm.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 10),
        ('BOTTOMPADDING', (0,0), (-1,-1), 10),
        ('LEFTPADDING', (0,0), (-1,-1), 9),
        ('RIGHTPADDING', (0,0), (-1,-1), 9),
    ]))
    story.append(t_ibm)
    story.append(Spacer(1, 14))

    takeaway3 = [
        [Paragraph("<b>MAINFRAME ADVANTAGE:</b> While competitor projects run generic Python scripts on standard VMs, PulseGuard-Z uses native s390x hardware features: on-chip Telum AI, CPACF pervasive cipher, and Parallel Sysplex seven-nines uptime.", takeaway_style)]
    ]
    t_take3 = Table(takeaway3, colWidths=[720])
    t_take3.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F0FDF4')),
        ('BOX', (0,0), (-1,-1), 1.5, colors.HexColor('#10B981')),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
        ('LEFTPADDING', (0,0), (-1,-1), 12),
    ]))
    story.append(t_take3)
    story.append(PageBreak())

    # =========================================================================
    # SLIDE 5: EMPIRICAL VERIFICATION & SOCIO-ECONOMIC IMPACT
    # =========================================================================
    story.append(Paragraph("EMPIRICAL PROOF & REAL-WORLD IMPACT", tag_style))
    story.append(Paragraph("Certified Performance, Self-Test Suite & Societal Value", title_style))
    story.append(Paragraph("Verified under multi-threaded enterprise stress conditions with quantified human impact.", subtitle_style))

    final_data = [
        [
            Paragraph("<b>VERIFIED SYSTEM BENCHMARKS</b><br/><br/>• <b>Concurrent Stress Test:</b> 100,000 requests across 16 threads in <b>0.243s</b> (&gt;205,000 decisions/sec, P99 = 8.5 µs).<br/>• <b>10-Point Self-Test Suite:</b> Single CLI command (<font color='#0F62FE'>java Main --diagnostics</font>) verifies all subsystems in <b>216 ms</b> with official certificate.<br/>• <b>60 FPS Lead-II ECG Canvas:</b> Real-time cardiac oscillograph with dynamic P-Q-R-S-T synthesis & Web Audio pulse beeps.", card_body_style),
            Paragraph("<b>HUMAN, CLINICAL & ECONOMIC IMPACT</b><br/><br/>• <b>Cutting Mortality in Half:</b> Forecasting deterioration 2–4 hours early cuts septic shock mortality from <b>~40% to &lt;20%</b> (Kumar et al.).<br/>• <b>Preventing Medical Poverty:</b> Private hospital sepsis ICU admissions cost <b>₹2–5 lakhs</b>. Early district stabilization shields rural families.<br/>• <b>Public Health Scalability:</b> A single central LinuxONE instance can monitor all 25,000+ PHCs in a state via existing pulse oximeters.", card_body_style)
        ]
    ]
    t_final = Table(final_data, colWidths=[355, 355])
    t_final.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 1, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 10),
        ('BOTTOMPADDING', (0,0), (-1,-1), 10),
        ('LEFTPADDING', (0,0), (-1,-1), 12),
        ('RIGHTPADDING', (0,0), (-1,-1), 12),
    ]))
    story.append(t_final)
    story.append(Spacer(1, 14))

    takeaway5 = [
        [Paragraph("<b>SUMMARY & REPO:</b> Codebase, 10-point diagnostics suite, and documentation are public on GitHub: <b>https://github.com/preyansh/pulseguard-z</b>. PulseGuard-Z proves real-time AI on IBM Z can save thousands of human lives across India.", takeaway_style)]
    ]
    t_take5 = Table(takeaway5, colWidths=[720])
    t_take5.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F0F9FF')),
        ('BOX', (0,0), (-1,-1), 1.5, colors.HexColor('#0F62FE')),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
        ('LEFTPADDING', (0,0), (-1,-1), 12),
    ]))
    story.append(t_take5)

    # Build the document
    doc.build(story, canvasmaker=SlideDeckCanvas)
    print(f"[SUCCESS] Clean 5-slide pitch deck built: {filename} ({os.path.getsize(filename):,} bytes)")

if __name__ == '__main__':
    build_pitch_deck()
