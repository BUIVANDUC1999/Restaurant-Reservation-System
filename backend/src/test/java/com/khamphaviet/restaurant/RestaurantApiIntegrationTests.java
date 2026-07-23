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
import java.util.HashMap;
import java.util.List;

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositAmount").value(60000000))
                .andExpect(jsonPath("$.depositStatus").value("PENDING"));
        mvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oneMoreGuest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preorderDepositIsTenPercentOfSelectedFood() throws Exception {
        var request = Map.of(
                "customerName", "Khach dat mon",
                "phone", "0912345678",
                "reservationDate", LocalDate.now().plusYears(4).toString(),
                "timeSlot", "LUNCH",
                "partySize", 2,
                "preOrderItems", java.util.List.of(Map.of("menuItemId", 1, "quantity", 2))
        );
        mvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositAmount").value(44000));
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
        mvc.perform(get("/api/v1/staff/timeouts/policy")
                        .header("Authorization", "Bearer " + login.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationHoldMinutes").value(10))
                .andExpect(jsonPath("$.lateWarningMinutes").value(15))
                .andExpect(jsonPath("$.lateCriticalMinutes").value(20))
                .andExpect(jsonPath("$.tableRequestAckMinutes").value(3));
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

    @Test
    void exactTimeAndCleaningBufferPreventOverlappingTableBookings() throws Exception {
        String date=LocalDate.now().plusYears(2).plusDays(17).toString();
        var available=mvc.perform(get("/api/v1/reservations/available-tables")
                        .param("date",date).param("time","18:00").param("durationMinutes","120").param("partySize","2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].layoutX").isNumber()).andReturn();
        long tableId=objectMapper.readTree(available.getResponse().getContentAsString()).get(0).get("id").asLong();

        Map<String,Object> first=new HashMap<>();
        first.put("customerName","Khách khung giờ 1");first.put("phone","0908888001");first.put("reservationDate",date);
        first.put("timeSlot","DINNER");first.put("reservationTime","18:00");first.put("durationMinutes",120);
        first.put("partySize",2);first.put("selectedTableIds",List.of(tableId));first.put("preOrderItems",List.of());
        mvc.perform(post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reservationTime").value("18:00:00"));

        Map<String,Object> overlap=new HashMap<>(first);overlap.put("customerName","Khách bị trùng");overlap.put("phone","0908888002");
        overlap.put("reservationTime","19:45");
        mvc.perform(post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(overlap)))
                .andExpect(status().isBadRequest());

        Map<String,Object> later=new HashMap<>(first);later.put("customerName","Khách sau dọn bàn");later.put("phone","0908888003");
        later.put("reservationTime","20:30");
        mvc.perform(post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(later)))
                .andExpect(status().isOk());
    }
}
