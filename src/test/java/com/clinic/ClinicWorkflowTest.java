package com.clinic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end business rules through the REST API, signed in as an administrator. */
@WithMockUser(username = "admin", roles = "ADMIN")
class ClinicWorkflowTest extends ApiTestSupport {

    @BeforeEach
    void setUpDoctor() throws Exception {
        ensureDoctor();
    }

    @Test
    void patientValidationDuplicateAndMissingRecords() throws Exception {
        Map<String, Object> patient = Map.of("fullName", "Dup " + unique, "phone", phone(), "gender", "MALE");
        assertThat(post("/api/patients", patient).status()).isEqualTo(201);
        assertThat(post("/api/patients", patient).status()).isEqualTo(409);
        Call invalid = post("/api/patients", Map.of("fullName", "", "phone", "12", "gender", "MALE"));
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.body().path("errors").has("fullName")).isTrue();
        assertThat(get("/api/patients/999999999").status()).isEqualTo(404);
    }

    @Test
    void bookingRegistersNewPatientAndGivesNextToken() throws Exception {
        Map<String, Object> newPatient = Map.of("fullName", "Baby " + unique, "phone", phone(), "gender", "FEMALE",
                "dateOfBirth", LocalDate.now().minusMonths(5).toString());
        Call first = post("/api/appointments", Map.of("patient", newPatient));
        assertThat(first.status()).isEqualTo(201);
        assertThat(first.data().path("type").asText()).isEqualTo("CONSULTATION");
        assertThat(first.data().path("patientAgeText").asText()).startsWith("5 months");
        Call second = post("/api/appointments", Map.of("patient", Map.of("fullName", "Sibling " + unique, "phone", phone(), "gender", "MALE")));
        assertThat(second.data().path("tokenNumber").asInt()).isEqualTo(first.data().path("tokenNumber").asInt() + 1);
    }

    @Test
    void samePatientCannotBeQueuedTwiceOnOneDay() throws Exception {
        long patientId = createPatient();
        assertThat(post("/api/appointments", booking(patientId, LocalDate.now())).status()).isEqualTo(201);
        assertThat(post("/api/appointments", booking(patientId, LocalDate.now())).status()).isEqualTo(409);
    }

    @Test
    void followUpIsFreeWithinValidityDaysOfCompletedConsultation() throws Exception {
        long patientId = createPatient();
        long consultation = post("/api/appointments", booking(patientId, LocalDate.now())).id();
        patch("/api/appointments/" + consultation + "/status", Map.of("status", "COMPLETED"));
        int validity = get("/api/settings").data().path("followUpValidityDays").asInt();
        Call lastFreeDay = get("/api/appointments/preview?patientId=" + patientId + "&date=" + LocalDate.now().plusDays(validity));
        assertThat(lastFreeDay.data().path("type").asText()).isEqualTo("FOLLOW_UP");
        assertThat(lastFreeDay.data().path("fee").asDouble()).isZero();
        Call dayAfter = get("/api/appointments/preview?patientId=" + patientId + "&date=" + LocalDate.now().plusDays(validity + 1));
        assertThat(dayAfter.data().path("type").asText()).isEqualTo("CONSULTATION");
    }

    @Test
    void completedIsFinalAndBookingsCannotBeEdited() throws Exception {
        long id = post("/api/appointments", booking(createPatient(), LocalDate.now())).id();
        String status = "/api/appointments/" + id + "/status";
        assertThat(patch(status, Map.of("status", "COMPLETED")).status()).isEqualTo(200);
        assertThat(patch(status, Map.of("status", "SCHEDULED")).status()).isEqualTo(409);
        assertThat(put("/api/appointments/" + id, booking(createPatient(), LocalDate.now())).status()).isEqualTo(405);
    }

    @Test
    void adminDeleteRemovesScheduledAppointment() throws Exception {
        long id = post("/api/appointments", booking(createPatient(), LocalDate.now())).id();
        Call request = post("/api/appointments/" + id + "/delete-request", null);
        assertThat(request.data().path("status").asText()).isEqualTo("APPROVED");
        assertThat(get("/api/appointments/" + id).status()).isEqualTo(404);
    }

    @Test
    void admissionOccupiesBedAndDischargeFreesIt() throws Exception {
        long patientId = createPatient();
        long bedId = createBed();
        Call admitted = post("/api/ipd", admission(patientId, bedId));
        assertThat(get("/api/beds/" + bedId).data().path("status").asText()).isEqualTo("OCCUPIED");
        assertThat(post("/api/ipd", admission(patientId, createBed())).status()).isEqualTo(409);
        Map<String, Object> discharge = Map.of("dischargedAt", LocalDate.now() + "T09:00:00", "dischargeCondition", "RECOVERED", "dischargeSummary", "Stable");
        assertThat(post("/api/ipd/" + admitted.id() + "/discharge", discharge).status()).isEqualTo(200);
        assertThat(get("/api/beds/" + bedId).data().path("status").asText()).isEqualTo("AVAILABLE");
    }

    @Test
    void admittedPatientCannotBeDeleted() throws Exception {
        long patientId = createPatient();
        post("/api/ipd", admission(patientId, createBed()));
        Call delete = call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/patients/" + patientId), null);
        assertThat(delete.status()).isEqualTo(409);
    }

    @Test
    void templateLogoMustFitInsideHeader() throws Exception {
        Call template = get("/api/templates/REPORT");
        Map<String, Object> body = json.convertValue(template.data(), Map.class);
        body.put("logoAreaHeight", 300);
        body.put("headerHeight", 100);
        assertThat(put("/api/templates/REPORT", body).status()).isEqualTo(400);
    }

    @Test
    void dashboardReturnsRealCounts() throws Exception {
        Call dashboard = get("/api/dashboard");
        assertThat(dashboard.status()).isEqualTo(200);
        assertThat(dashboard.data().path("counts").has("PENDING_DELETE_REQUESTS")).isTrue();
        assertThat(dashboard.data().path("beds").has("TOTAL")).isTrue();
    }

    private Map<String, Object> admission(long patientId, long bedId) {
        return Map.of("patientId", patientId, "bedId", bedId,
                "admittedAt", LocalDate.now().minusDays(1) + "T10:00:00", "reason", "Observation");
    }
}
