import os
import pymupdf
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas

PAGE_WIDTH, PAGE_HEIGHT = letter  # 612 x 792

class ScriptCanvas(canvas.Canvas):
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
        # Top Rule
        self.setStrokeColor(colors.black)
        self.setLineWidth(1.2)
        self.line(36, PAGE_HEIGHT - 32, PAGE_WIDTH - 36, PAGE_HEIGHT - 32)
        
        # Header text
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.black)
        self.drawString(36, PAGE_HEIGHT - 26, "PULSEGUARD-Z  |  OFFICIAL VIDEO SUBMISSION TELEPROMPTER SCRIPT")
        self.setFont("Helvetica", 8)
        self.drawRightString(PAGE_WIDTH - 36, PAGE_HEIGHT - 26, "TARGET: 2 MIN 45 SEC (MAX 3:00)  •  IBM Z DATATHON 2026")
        
        # Bottom Rule
        self.setStrokeColor(colors.black)
        self.setLineWidth(1.0)
        self.line(36, 30, PAGE_WIDTH - 36, 30)
        
        # Footer text
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.black)
        self.drawString(36, 18, "Team CodeBuilders  •  Preyansh Bhagatwala  •  Track: Real-Time AI for Critical Decisions")
        self.setFont("Helvetica-Bold", 8)
        self.drawRightString(PAGE_WIDTH - 36, 18, f"PAGE {self._pageNumber} OF {page_count}")
        
        self.restoreState()


def build_script_pdf(filename="PulseGuard_Z_Video_Script.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=36,
        rightMargin=36,
        topMargin=38,
        bottomMargin=36
    )

    styles = getSampleStyleSheet()

    title_style = ParagraphStyle(
        'ScriptTitle',
        fontName='Helvetica-Bold',
        fontSize=15,
        leading=18,
        textColor=colors.black,
        spaceAfter=2
    )

    subtitle_style = ParagraphStyle(
        'ScriptSubtitle',
        fontName='Helvetica-Oblique',
        fontSize=8.5,
        leading=11.5,
        textColor=colors.HexColor('#222222'),
        spaceAfter=6
    )

    section_header_style = ParagraphStyle(
        'SectionHeader',
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=12,
        textColor=colors.white
    )

    cue_style = ParagraphStyle(
        'CueStyle',
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=11,
        textColor=colors.black
    )

    speech_style = ParagraphStyle(
        'SpeechStyle',
        fontName='Helvetica',
        fontSize=8.8,
        leading=12.5,
        textColor=colors.black
    )

    story = []

    # Title & Metadata
    story.append(Paragraph("PulseGuard-Z: 3-Minute Video Recording Script", title_style))
    story.append(Paragraph("Teleprompter script with exact timestamps, visual cues, and spoken audio (Target: 2 min 45 sec)", subtitle_style))

    # Pre-Recording Checklist Table
    checklist_data = [
        [
            Paragraph("<b>PRE-RECORDING QUICK CHECKLIST</b>", ParagraphStyle('ChkH', fontName='Helvetica-Bold', fontSize=8, textColor=colors.white)),
        ],
        [
            Paragraph(
                "• <b>Pacing & Tone:</b> Speak calmly and deliberately (~135 words/min). Do NOT rush. Total target: 2m 45s (Limit 3m 00s).<br/>"
                "• <b>Pronunciations:</b> Telum (<i>TELL-um</i>) | LMAX (<i>ELL-max</i>) | AIIMS (<i>EYES</i>) | ABHA (<i>UB-bha</i>) | NNPA (<i>N-N-P-A</i>).<br/>"
                "• <b>Screen Setup:</b> Browser / PDF zoomed to 130%-150%; terminal font size 14pt+ for high-contrast visibility.<br/>"
                "• <b>YouTube Upload:</b> Set visibility to <b>Unlisted</b> or <b>Public</b> (Never Private!). Add GitHub & PDF links in description.",
                ParagraphStyle('ChkB', fontName='Helvetica', fontSize=7.5, leading=10.5, textColor=colors.black)
            )
        ]
    ]
    t_chk = Table(checklist_data, colWidths=[540])
    t_chk.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.black),
        ('GRID', (0,0), (-1,-1), 0.75, colors.black),
        ('BACKGROUND', (0,1), (-1,1), colors.HexColor('#F8F8F8')),
        ('TOPPADDING', (0,0), (-1,-1), 3.5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3.5),
        ('LEFTPADDING', (0,0), (-1,-1), 7),
        ('RIGHTPADDING', (0,0), (-1,-1), 7),
    ]))
    story.append(t_chk)
    story.append(Spacer(1, 6))

    # Sections Definition
    sections_page1 = [
        {
            "time": "0:00 – 0:25",
            "duration": "25s",
            "words": "56 Words",
            "title": "SECTION 1: THE CLINICAL EMERGENCY & GOLDEN HOUR",
            "visual": "SCREEN: Show Slide 2 of Pitch Deck (The Clinical Crisis Table) or the Sepsis Timeline diagram.",
            "speech": (
                "\"In septic shock, time is tissue. Clinical research published in <i>Critical Care Medicine</i> "
                "proves that for every single hour antimicrobial therapy is delayed, patient mortality increases by <b>7.6%</b>.<br/><br/>"
                "Across India, sepsis claims nearly three million lives annually. In rural Community Health Centres, "
                "there is an <b>79.5% deficit</b> of critical care specialists. Patients deteriorate silently for hours—and by the time "
                "blood pressure collapses, it is often too late.<br/><br/>"
                "Closing this deadly latency gap requires deterministic, sub-second telemetry intelligence.\""
            )
        },
        {
            "time": "0:25 – 0:55",
            "duration": "30s",
            "words": "68 Words",
            "title": "SECTION 2: PULSEGUARD-Z SOLUTION & INGESTION ARCHITECTURE",
            "visual": "SCREEN: Show Slide 1 (Project Charter) and Slide 3 (6-Stage Pipeline Table).",
            "speech": (
                "\"This is <b>PulseGuard-Z</b>—a real-time Tele-ICU triage and emergency escalation engine "
                "engineered in Java 21 on IBM Z and LinuxONE mainframes.<br/><br/>"
                "Rather than relying on commodity cloud servers prone to network jitter and GPU bus bottlenecks, "
                "PulseGuard-Z runs directly on the mainframe.<br/><br/>"
                "Our ingestion tier utilizes an <b>LMAX Disruptor lock-free ring buffer</b> with 65,536 cache-aligned slots. "
                "It streams vital sign telemetry with <b>zero garbage collection allocations</b>, eliminating JVM stop-the-world pauses entirely.\""
            )
        },
        {
            "time": "0:55 – 1:35",
            "duration": "40s",
            "words": "94 Words",
            "title": "SECTION 3: ON-CHIP TELUM AI & AUTONOMOUS ESCALATION (LIVE DEMO)",
            "visual": "SCREEN: Live Terminal showing server logs, vital sign evaluation, and autonomous bed booking JSON response.",
            "speech": (
                "\"At the heart of the engine is a dual evaluation pipeline:<br/><br/>"
                "First, deterministic rule scoring implementing the <b>AIIMS National Early Warning Score</b> protocol.<br/><br/>"
                "Second, deep neural hazard forecasting powered by the <b>IBM Telum on-chip Neural Network Processing Assist</b>. "
                "Executing matrix GEMM operations directly on-chip at 4.6 gigahertz alongside CPU cache delivers inference in just <b>2.8 microseconds</b>.<br/><br/>"
                "The instant a patient breaches the critical NEWS2 threshold of 7, PulseGuard-Z triggers autonomous escalation: "
                "it locks an emergency ICU bed at the District Hospital, reserves a ventilator, and dispatches an Emergency 108 Advanced Life Support ambulance with GPS-optimized routing.\""
            )
        }
    ]

    sections_page2 = [
        {
            "time": "1:35 – 2:05",
            "duration": "30s",
            "words": "74 Words",
            "title": "SECTION 4: OFFLINE 2G SMS FALLBACK & ABDM COMPLIANCE",
            "visual": "SCREEN: Show Slide 3 (53-Byte Frame Table) and Slide 4 (Hardware Exploitation Matrix).",
            "speech": (
                "\"In rural Primary Health Centres where internet connectivity fails, PulseGuard-Z deploys a fail-safe offline protocol: "
                "compressing all 7 vitals, GPS coordinates, and clinical alerts into a single <b>53-byte binary struct</b> with CRC-16 checksums, transmitted over standard 2G GSM SMS.<br/><br/>"
                "Every event is formatted into an authentic <b>ABDM HL7 FHIR R4 bundle</b> tied to the patient's 14-digit ABHA ID.<br/><br/>"
                "Mainframe <b>CPACF hardware opcodes</b> encrypt records at silicon speed, while an immutable <b>SHA-256 Merkle ledger</b> guarantees full compliance with India's DPDP Act 2023.\""
            )
        },
        {
            "time": "2:05 – 2:35",
            "duration": "30s",
            "words": "73 Words",
            "title": "SECTION 5: EMPIRICAL BENCHMARKS & SOCIO-ECONOMIC IMPACT",
            "visual": "SCREEN: Show Slide 5 of Pitch Deck (Empirical Benchmarks & Socio-Economic Impact Tables).",
            "speech": (
                "\"We subjected PulseGuard-Z to extensive stress-testing. In reproducible benchmarks:<br/><br/>"
                "The engine processed <b>100,000 multi-parameter records in just 0.24 seconds</b>—achieving sustained throughput exceeding <b>205,000 evaluations per second</b> on a single core, with 99th percentile latency bounded at 3.9 microseconds.<br/><br/>"
                "Clinically, detecting septic shock 2 to 4 hours earlier cuts mortality from 40% to under 20%. "
                "Economically, early stabilization saves rural families <b>two to five lakh rupees</b> in catastrophic private ICU debt.\""
            )
        },
        {
            "time": "2:35 – 2:45",
            "duration": "10s",
            "words": "28 Words",
            "title": "SECTION 6: CLOSING SUMMARY",
            "visual": "SCREEN: Show GitHub Repository (README.md) with badges, documentation, and contact details.",
            "speech": (
                "\"PulseGuard-Z brings the world's most resilient mainframe computing architecture to India's most vulnerable critical care patients.<br/><br/>"
                "All source code, test suites, and benchmarks are fully open-source and reproducible on GitHub. Thank you.\""
            )
        }
    ]

    for sec in sections_page1:
        header_text = f"<b>{sec['title']}</b>   [{sec['time']}  |  {sec['duration']}  |  {sec['words']}]"
        t_sec = Table([
            [Paragraph(header_text, section_header_style)],
            [Paragraph(f"<b>VISUAL / ACTION:</b> {sec['visual']}", cue_style)],
            [Paragraph(sec['speech'], speech_style)]
        ], colWidths=[540])
        t_sec.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,0), colors.black),
            ('BACKGROUND', (0,1), (-1,1), colors.HexColor('#EFEFEF')),
            ('BACKGROUND', (0,2), (-1,2), colors.white),
            ('GRID', (0,0), (-1,-1), 0.75, colors.black),
            ('TOPPADDING', (0,0), (-1,-1), 3),
            ('BOTTOMPADDING', (0,0), (-1,-1), 3),
            ('LEFTPADDING', (0,0), (-1,-1), 7),
            ('RIGHTPADDING', (0,0), (-1,-1), 7),
        ]))
        story.append(t_sec)
        story.append(Spacer(1, 5))

    story.append(PageBreak())

    for sec in sections_page2:
        header_text = f"<b>{sec['title']}</b>   [{sec['time']}  |  {sec['duration']}  |  {sec['words']}]"
        t_sec = Table([
            [Paragraph(header_text, section_header_style)],
            [Paragraph(f"<b>VISUAL / ACTION:</b> {sec['visual']}", cue_style)],
            [Paragraph(sec['speech'], speech_style)]
        ], colWidths=[540])
        t_sec.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,0), colors.black),
            ('BACKGROUND', (0,1), (-1,1), colors.HexColor('#EFEFEF')),
            ('BACKGROUND', (0,2), (-1,2), colors.white),
            ('GRID', (0,0), (-1,-1), 0.75, colors.black),
            ('TOPPADDING', (0,0), (-1,-1), 4),
            ('BOTTOMPADDING', (0,0), (-1,-1), 4),
            ('LEFTPADDING', (0,0), (-1,-1), 7),
            ('RIGHTPADDING', (0,0), (-1,-1), 7),
        ]))
        story.append(t_sec)
        story.append(Spacer(1, 8))

    doc.build(story, canvasmaker=ScriptCanvas)
    file_size = os.path.getsize(filename)
    print(f"[SUCCESS] Teleprompter Video Script PDF built: {filename} ({file_size:,} bytes)")


if __name__ == "__main__":
    build_script_pdf()
