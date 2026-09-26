package com.clinic;

import com.clinic.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Role and module permissions are enforced by the backend, not only hidden in the UI. */
class AccessControlTest extends ApiTestSupport {

    @Test
    @WithAnonymousUser
    void signedOutUsersOnlySeePublicBranding() throws Exception {
        assertThat(get("/api/settings").status()).isEqualTo(200);
        assertThat(get("/api/patients").status()).isEqualTo(401);
    }

    @Test
    @WithMockUser(username = "desk", roles = "STAFF")
    void staffCannotChangeSettingsOrManageUsers() throws Exception {
        assertThat(get("/api/doctors").status()).isEqualTo(200);
        assertThat(post("/api/doctors", Map.of("fullName", "X")).status()).isEqualTo(403);
        assertThat(patch("/api/appointment-delete-requests/1/status", Map.of("status", "APPROVED")).status()).isEqualTo(403);
        assertThat(patch("/api/appointments/1/status", Map.of("status", "COMPLETED")).status()).isEqualTo(403);
        assertThat(put("/api/appointments/1/prescription", Map.of("strokes", "[]")).status()).isEqualTo(403);
        assertThat(post("/api/appointments/1/prescription/complete", Map.of("strokes", "[]")).status()).isEqualTo(403);
        assertThat(get("/api/settings/logo").status()).isNotEqualTo(403);
        assertThat(get("/api/users").status()).isEqualTo(403);
        assertThat(get("/api/permissions").status()).isEqualTo(403);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void administratorsCannotRemoveTheirOwnSettingsAccess() throws Exception {
        Call change = put("/api/permissions", List.of(Map.of("role", "ADMIN", "module", "SETTINGS", "access", "READ")));
        assertThat(change.status()).isEqualTo(400);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void logoUploadRejectsFilesThatAreNotImages() throws Exception {
        var file = new org.springframework.mock.web.MockMultipartFile("file", "evil.png", "image/png", "<svg onload=alert(1)>".getBytes());
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/settings/logo").file(file);
        assertThat(call(request, null).status()).isEqualTo(400);
    }
}
