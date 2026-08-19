package com.khamphaviet.restaurant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khamphaviet.restaurant.deposit.DepositMethod;
import com.khamphaviet.restaurant.deposit.ReservationDepositRepository;
import com.khamphaviet.restaurant.table.RestaurantTable;
import com.khamphaviet.restaurant.table.RestaurantTableRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class RestaurantApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ReservationDepositRepository deposits;
    @Autowired RestaurantTableRepository tables;

    @Test
    void publicMenuIsAvailable() throws Exception {
        mvc.perform(get("/api/v1/menu/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    @Transactional
    void tableQrCanCallWaiterWithoutLoginOrActiveSession() throws Exception {
        String publicToken = "public-qr-integration-test";
        RestaurantTable table = tables.save(new RestaurantTable(
                "TQR", "Bàn kiểm thử QR", "Tầng trệt", "Kiểm thử", 4,
                10, 10, "ROUND", publicToken));

        mvc.perform(get("/api/v1/table-guest/{token}", publicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TQR"))
                .andExpect(jsonPath("$.activeSession").value(false));

        mvc.perform(post("/api/v1/table-guest/{token}/requests", publicToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CALL_WAITER\",\"note\":\"Cần hỗ trợ đặt món\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableId").value(table.getId()))
                .andExpect(jsonPath("$.status").value("NEW"));
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
                "partySize", 40
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
                .andExpect(jsonPath("$.depositAmount").value(8000000))
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
        mvc.perform(get("/api/v1/staff/tables/overview")
                        .header("Authorization", "Bearer " + login.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[?(@.code == 'B07')].area").value("Trung tâm"))
                .andExpect(jsonPath("$[?(@.code == 'B08')].area").value("Trung tâm"));
        mvc.perform(get("/api/v1/staff/timeouts/policy")
                        .header("Authorization", "Bearer " + login.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationHoldMinutes").value(10))
                .andExpect(jsonPath("$.reservationConfirmationMinutes").value(5))
                .andExpect(jsonPath("$.lateWarningMinutes").value(15))
                .andExpect(jsonPath("$.lateCriticalMinutes").value(20))
                .andExpect(jsonPath("$.tableRequestAckMinutes").value(3));
    }

    @Test
    void reservationConfirmationRequiresPaidDepositAndCreatesAuditHistory() throws Exception {
        String token = staffToken();
        var created = mvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerName", "Khach xac nhan coc",
                                "phone", "0934567890",
                                "reservationDate", LocalDate.now().plusYears(7).toString(),
                                "timeSlot", "LUNCH",
                                "partySize", 2
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositStatus").value("PENDING"))
                .andReturn();
        long reservationId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/v1/staff/reservations/" + reservationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isBadRequest());

        var deposit = deposits.findByReservationId(reservationId).orElseThrow();
        deposit.pay(DepositMethod.PAYPAL, "TEST-ORDER", "TEST-CAPTURE");
        deposits.saveAndFlush(deposit);

        mvc.perform(patch("/api/v1/staff/reservations/" + reservationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\",\"reason\":\"Da doi chieu giao dich Sandbox\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").exists());

        mvc.perform(get("/api/v1/staff/reservations/" + reservationId + "/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].toStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].reason").value("Da doi chieu giao dich Sandbox"));

        mvc.perform(patch("/api/v1/staff/reservations/" + reservationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/v1/staff/reservations/" + reservationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\",\"reason\":\"Khach yeu cau huy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
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
    void adminCanOpenDashboardMetricDetails() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@khamphaviet.vn\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();

        mvc.perform(get("/api/v1/admin/reports/operations/details")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationsToday").isArray())
                .andExpect(jsonPath("$.activeSessions").isArray())
                .andExpect(jsonPath("$.paymentsToday").isArray())
                .andExpect(jsonPath("$.paymentsThisMonth").isArray());
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

    @Test
    void staffCanOrchestrateWalkInFromQueueToSeatedTable() throws Exception {
        var loginResult=mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff@khamphaviet.vn\",\"password\":\"Staff@123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token=objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        var created=mvc.perform(post("/api/v1/staff/walk-ins")
                        .header("Authorization","Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Khach tai quan","phone":"0987111222","partySize":2,
                                 "priority":"NORMAL","quotedWaitMinutes":0,"note":"Kiem thu walk-in"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.slaLevel").exists())
                .andExpect(jsonPath("$.suggestedTables[0].safe").value(true))
                .andReturn();
        JsonNode visit=objectMapper.readTree(created.getResponse().getContentAsString());
        long visitId=visit.get("id").asLong();
        long tableId=visit.get("suggestedTables").get(0).get("id").asLong();

        mvc.perform(post("/api/v1/staff/walk-ins/"+visitId+"/offer")
                        .header("Authorization","Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tableId",tableId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TABLE_OFFERED"))
                .andExpect(jsonPath("$.reservationId").isNumber())
                .andExpect(jsonPath("$.offerExpiresAt").exists());

        mvc.perform(post("/api/v1/staff/walk-ins/"+visitId+"/seat")
                        .header("Authorization","Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SEATED"))
                .andExpect(jsonPath("$.serviceSessionId").isNumber())
                .andExpect(jsonPath("$.events.length()").value(3));

        mvc.perform(get("/api/v1/staff/walk-ins/metrics")
                        .header("Authorization","Bearer "+token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").isNumber())
                .andExpect(jsonPath("$.quoteAccuracyPercent").isNumber());
    }

    @Test
    @Transactional
    void demoScenarioCreatesOnlyTheSituationConfiguredByTheUser() throws Exception {
        String token = staffToken();
        mvc.perform(post("/api/v1/staff/walk-ins/demo-scenario")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Tình huống do người dùng tạo","phone":"0901000008",
                                 "partySize":3,"priority":"NORMAL","quotedWaitMinutes":10,
                                 "slaLevel":"CRITICAL","note":"Kiểm thử cấu hình thủ công"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("[DEMO] Tình huống do người dùng tạo"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.slaLevel").value("CRITICAL"));
        mvc.perform(post("/api/v1/staff/walk-ins/demo-scenario")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Tình huống cảnh báo thứ hai","phone":"0901000009",
                                 "partySize":2,"priority":"ELDERLY","priorityReason":"Khách cao tuổi",
                                 "quotedWaitMinutes":20,"slaLevel":"WARNING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("[DEMO] Tình huống cảnh báo thứ hai"))
                .andExpect(jsonPath("$.slaLevel").value("WARNING"));
    }

    @Test
    @Transactional
    void fullDemoStudioCanCreateAnAutomaticallyOverdueDish() throws Exception {
        String token=staffToken();
        mvc.perform(post("/api/v1/staff/demo-scenarios")
                        .header("Authorization","Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"KITCHEN_OVERDUE_WARNING","customerName":"Khách kiểm thử món chậm",
                                 "phone":"0901000010","partySize":2,"minutes":4,
                                 "reason":"Quá ETA tự động","note":"Kiểm thử xưởng tình huống"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group").value("Bếp & món ăn"))
                .andExpect(jsonPath("$.title").value("Món tự động quá ETA"))
                .andExpect(jsonPath("$.targetPath").value("/bep"))
                .andExpect(jsonPath("$.entityId").isNumber())
                .andExpect(jsonPath("$.tableId").isNumber());

        mvc.perform(get("/api/v1/staff/timeouts")
                        .header("Authorization","Bearer "+token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'KITCHEN_SLA')]").isNotEmpty());
    }

    @Test
    void sameWalkInTableCannotBeOfferedTwice() throws Exception {
        String token = staffToken();
        long first = createWalkIn(token, "Tranh chấp bàn A");
        var secondResult = mvc.perform(post("/api/v1/staff/walk-ins")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Tranh chấp bàn B","phone":"0987000002","partySize":2,
                                 "priority":"NORMAL","quotedWaitMinutes":0}
                                """))
                .andExpect(status().isOk()).andReturn();
        JsonNode second = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        long secondId = second.get("id").asLong();
        long tableId = second.get("suggestedTables").get(0).get("id").asLong();
        mvc.perform(post("/api/v1/staff/walk-ins/" + first + "/offer")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tableId", tableId))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/staff/walk-ins/" + secondId + "/offer")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("tableId", tableId))))
                .andExpect(status().isConflict());
    }

    private String staffToken() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"staff@khamphaviet.vn\",\"password\":\"Staff@123\"}"))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long createWalkIn(String token, String name) throws Exception {
        var result = mvc.perform(post("/api/v1/staff/walk-ins")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerName", name, "phone", "0987000001", "partySize", 2,
                                "priority", "NORMAL", "quotedWaitMinutes", 0))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
