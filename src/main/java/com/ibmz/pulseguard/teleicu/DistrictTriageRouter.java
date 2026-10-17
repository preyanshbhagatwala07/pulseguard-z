package com.ibmz.pulseguard.teleicu;

import java.util.ArrayList;
import java.util.List;

/**
 * DistrictTriageRouter: Hub-and-Spoke Tele-ICU Escalation Engine for Government Health Systems.
 * Connects Rural PHCs (Primary Health Centres) and CHCs with District Hospitals and Apex AIIMS Centers.
 * Integrates automated Emergency 108 Ambulance triage dispatch.
 */
public class DistrictTriageRouter {

    public static class HealthFacility {
        public final String facilityCode;
        public final String name;
        public final String tier; // PHC, CHC, DISTRICT_HOSPITAL, APEX_TERTIARY
        public final String district;
        public final String state;
        public final int totalBeds;
        public int availableIcuBeds;

        public HealthFacility(String facilityCode, String name, String tier, String district, String state, int totalBeds, int availableIcuBeds) {
            this.facilityCode = facilityCode;
            this.name = name;
            this.tier = tier;
            this.district = district;
            this.state = state;
            this.totalBeds = totalBeds;
            this.availableIcuBeds = availableIcuBeds;
        }
    }

    public static class DispatchOrder {
        public final String dispatchId;
        public final String patientId;
        public final String abhaId;
        public final String originatingFacility;
        public final String destinationHospital;
        public final String triageSeverity;
        public final String ambulanceUnitId;
        public final int etaMinutes;
        public final String immediateStabilizationOrders;
        public final long timestamp;

        public DispatchOrder(String dispatchId, String patientId, String abhaId,
                             String originatingFacility, String destinationHospital,
                             String triageSeverity, String ambulanceUnitId,
                             int etaMinutes, String immediateStabilizationOrders, long timestamp) {
            this.dispatchId = dispatchId;
            this.patientId = patientId;
            this.abhaId = abhaId;
            this.originatingFacility = originatingFacility;
            this.destinationHospital = destinationHospital;
            this.triageSeverity = triageSeverity;
            this.ambulanceUnitId = ambulanceUnitId;
            this.etaMinutes = etaMinutes;
            this.immediateStabilizationOrders = immediateStabilizationOrders;
            this.timestamp = timestamp;
        }
    }

    private final List<HealthFacility> network = new ArrayList<>();
    private final List<DispatchOrder> activeDispatches = new ArrayList<>();

    public DistrictTriageRouter() {
        // Sample realistic District Tele-ICU Grid (e.g. Pune/Chennai/Delhi healthcare corridor)
        network.add(new HealthFacility("PHC-042", "Dharampur Rural PHC", "PHC", "Vapi / Valsad", "Gujarat", 12, 0));
        network.add(new HealthFacility("CHC-108", "Kaprada Community Health Centre", "CHC", "Valsad", "Gujarat", 30, 2));
        network.add(new HealthFacility("DH-201", "Civil Hospital Valsad (District HQ)", "DISTRICT_HOSPITAL", "Valsad", "Gujarat", 350, 14));
        network.add(new HealthFacility("AIIMS-01", "AIIMS Rajkot / Civil Hospital Surat", "APEX_TERTIARY", "Surat", "Gujarat", 950, 42));
    }

    public synchronized DispatchOrder triggerEmergencyTransfer(String patientId, String abhaId, String originatingCode,
                                                               int news2Score, double sepsisRisk, String vitalSummary) {
        HealthFacility dest = findNearestIcuFacility();
        if (dest != null && dest.availableIcuBeds > 0) {
            dest.availableIcuBeds--; // Reserve ICU bed automatically
        }

        String destName = (dest != null) ? dest.name : "District Headquarters Emergency Ward";
        String dispatchId = "EMERG-108-" + System.currentTimeMillis() % 100000;
        String ambulanceId = "ALS-AMB-" + (101 + (int)(Math.random() * 50));

        String orders = "STABILIZE EN ROUTE: Continuous high-flow O2, 2 large-bore IV lines (16G), administer 500 mL normal saline bolus, alert receiving intensivist team.";

        DispatchOrder order = new DispatchOrder(
            dispatchId, patientId, abhaId, originatingCode, destName,
            "CRITICAL_RED_SEPSIS", ambulanceId, 18, orders, System.currentTimeMillis()
        );

        activeDispatches.add(0, order);
        if (activeDispatches.size() > 50) {
            activeDispatches.remove(activeDispatches.size() - 1);
        }

        return order;
    }

    private HealthFacility findNearestIcuFacility() {
        for (HealthFacility hf : network) {
            if ((hf.tier.equals("DISTRICT_HOSPITAL") || hf.tier.equals("APEX_TERTIARY")) && hf.availableIcuBeds > 0) {
                return hf;
            }
        }
        return null;
    }

    public synchronized List<HealthFacility> getNetwork() {
        return new ArrayList<>(network);
    }

    public synchronized List<DispatchOrder> getActiveDispatches() {
        return new ArrayList<>(activeDispatches);
    }
}
