package com.clinic;

import com.clinic.entity.Patient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** Small helpers so each integration test reads as a few API calls and assertions. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected final String unique = UUID.randomUUID().toString().substring(0, 6);

    private int sequence;

    protected Call call(MockHttpServletRequestBuilder request, Object body) throws Exception {
        MockHttpServletRequestBuilder withBody = body == null ? request
                : request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        MvcResult result = mvc.perform(withBody.with(csrf())).andReturn();
        String content = result.getResponse().getContentAsString();
        return new Call(result.getResponse().getStatus(), content.isEmpty() ? null : json.readTree(content));
    }

    protected Call get(String url) throws Exception {
        return call(MockMvcRequestBuilders.get(url), null);
    }

    protected Call post(String url, Object body) throws Exception {
        return call(MockMvcRequestBuilders.post(url), body);
    }

    protected Call put(String url, Object body) throws Exception {
        return call(MockMvcRequestBuilders.put(url), body);
    }

    protected Call patch(String url, Object body) throws Exception {
        return call(MockMvcRequestBuilders.patch(url), body);
    }

    // ---- fixtures -------------------------------------------------------------

    /** The clinic has a single doctor; create it once if the test database has none. */
    protected void ensureDoctor() throws Exception {
        if (get("/api/doctors/clinic").data().isNull()) {
            post("/api/doctors", Map.of("fullName", "Dr. Test"));
        }
    }

    protected long createPatient() throws Exception {
        return post("/api/patients", Map.of("fullName", "Test Patient " + unique, "phone", phone(), "gender", "FEMALE")).id();
    }

    protected long createBed() throws Exception {
        Call room = post("/api/rooms", Map.of("roomNumber", "T" + unique + (++sequence), "roomType", "GENERAL", "dailyCharge", 1000, "initialBeds", 1));
        return room.data().path("beds").get(0).path("id").asLong();
    }

    protected Map<String, Object> booking(long patientId, LocalDate date) {
        return Map.of("patientId", patientId, "appointmentDate", date.toString());
    }

    protected String phone() {
        return String.format("9%09d", Math.abs(unique.hashCode() % 1_000_000_000));
    }

    /** Status code plus parsed body. */
    public record Call(int status, JsonNode body) {

        public JsonNode data() {
            return body.path("data");
        }

        public long id() {
            return data().path("id").asLong();
        }

        public String message() {
            return body.path("message").asText();
        }
    }
}
