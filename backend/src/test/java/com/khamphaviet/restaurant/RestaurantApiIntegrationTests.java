package com.khamphaviet.restaurant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class RestaurantApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void publicMenuIsAvailable() throws Exception {
        mvc.perform(get("/api/v1/menu/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void invalidReservationIsRejected() throws Exception {
        var request = Map.of(
                "customerName", "Khach thu",
                "phone", "0901234567",
                "reservationDate", LocalDate.now().minusDays(1).toString(),
                "timeSlot", "LUNCH",
                "partySize", 2
        );

        mvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reservationDate").exists());
    }

    @Test
    void staffEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/staff/reservations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reservationCapacityCannotBeExceeded() throws Exception {
        String date = LocalDate.now().plusYears(5).toString();
        var fullCapacity = Map.of(
                "customerName", "Doan lon",
                "phone", "0901234567",
                "reservationDate", date,
                "timeSlot", "DINNER",
                "partySize", 300
        );
        var oneMoreGuest = Map.of(
                "customerName", "Khach den sau",
                "phone", "0907654321",
                "reservationDate", date,
                "timeSlot", "DINNER",
                "partySize", 1
        );

        mvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullCapacity)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oneMoreGuest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffCanLoginAndOpenDashboardApi() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff@khamphaviet.vn\",\"password\":\"Staff@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andReturn();

        JsonNode login = objectMapper.readTree(result.getResponse().getContentAsString());
        mvc.perform(get("/api/v1/staff/reservations")
                        .header("Authorization", "Bearer " + login.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotOpenAdminApi() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@khamphaviet.vn\",\"password\":\"Customer@123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode login = objectMapper.readTree(result.getResponse().getContentAsString());
        mvc.perform(get("/api/v1/admin/users/stats")
                        .header("Authorization", "Bearer " + login.get("accessToken").asText()))
                .andExpect(status().isForbidden());
    }
}
