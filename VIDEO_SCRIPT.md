# PulseGuard-Z: 3-Minute Video Demo Script
**Track:** Real-Time AI for Critical Decisions | **Event:** IBM Z Datathon 2026

---

### Timing Breakdown (Total: 3 minutes / 180 seconds)

#### ⏱ 0:00 - 0:35 | The Hook & Critical Problem
*(Show presentation slide or screen showing sepsis statistics)*
> "Hello judges. Every single year, sepsis claims over 11 million lives worldwide. In Intensive Care Units, for every single hour that medical treatment is delayed, patient mortality climbs by 7.6 percent. 
> 
> Modern bedside monitors generate millions of data points every second, but current systems sound alarms only AFTER blood pressure has already crashed and the patient is in severe septic shock. Doctors and nurses are overwhelmed with alarm fatigue, and there is no real-time AI operating directly at the point of care to detect deterioration before it's too late. 
> 
> That is why our team built **PulseGuard-Z**."

---

#### ⏱ 0:35 - 1:15 | The Solution & IBM Z / LinuxONE Advantage
*(Show Architecture Diagram and Terminal connected to LinuxONE)*
> "PulseGuard-Z is a sub-second, enterprise clinical triage engine engineered in Java specifically for **IBM Z and LinuxONE** enterprise infrastructure.
> 
> It continuously ingests high-frequency physiological streams—including Heart Rate, Blood Pressure, Respiratory Rate, SpO2, and Lactate. It extracts 14 critical biomarkers, calculating real-time Shock Index and Mean Arterial Pressure, and executes a vectorized Sepsis-3 scoring model.
> 
> Why IBM Z and LinuxONE?
> First, **Sub-Millisecond In-Transaction AI**: PulseGuard-Z completes inference in **under 4 microseconds**—over 270,000 decisions per second—matching the speed of IBM Telum on-chip accelerators.
> Second, **Zero Downtime**: Intensive care units cannot afford system reboots; LinuxONE's 99.99999% availability guarantees continuous surveillance.
> And third, **IBM Z Pervasive Encryption**: Sensitive patient vitals remain encrypted end-to-end, guaranteeing strict HIPAA compliance."

---

#### ⏱ 1:15 - 2:25 | Live Prototype Demonstration
*(Switch to browser showing http://localhost:8085)*
> "Now let's see PulseGuard-Z in action.
> 
> Here on our live clinical triage dashboard, we are monitoring six ICU beds in real time. 
> Notice Bed 3—Robert Chen. The system has automatically flagged him with a red **CRITICAL SEPSIS** alert. His Shock Index has risen to 1.53, Mean Arterial Pressure has fallen below 60, and his lactate is 4.9. 
> 
> Notice the explainability chips: the AI clearly tells the physician exactly WHY the score jumped, and immediately prompts the Surviving Sepsis Campaign **Hour-1 Bundle**—drawing blood cultures, starting IV broad-spectrum antibiotics, and administering fluids.
> 
> Look at the inference latency in the top bar: **0.4 microseconds** per evaluation!
> 
> If we open the Interactive Sandbox below, we can test custom telemetry. Let's load an acute septic shock preset with a Heart Rate of 132, Blood Pressure of 78 over 44, and Lactate of 5.2. When I click 'Evaluate', the engine scores the packet in microseconds, categorizing it as Critical Sepsis with a 99% risk score.
> 
> Furthermore, clicking 'Run Benchmark' runs 50,000 live decisions right before your eyes, demonstrating over 400,000 decisions per second on the JVM!"

---

#### ⏱ 2:25 - 3:00 | Business Impact & Closing
*(Switch back to closing slide)*
> "Sepsis costs the global healthcare system over 38 billion dollars every year. By catching deterioration 2 to 4 hours earlier, PulseGuard-Z shortens ICU stays, saves hospitals hundreds of thousands of dollars per bed, and most importantly, saves human lives.
> 
> By uniting modern Java with the unmatched reliability and security of IBM LinuxONE and IBM Z, PulseGuard-Z turns passive hospital data into proactive, life-saving critical decisions.
> 
> Thank you."
