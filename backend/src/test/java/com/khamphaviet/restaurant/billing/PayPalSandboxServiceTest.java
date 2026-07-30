package com.khamphaviet.restaurant.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khamphaviet.restaurant.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayPalSandboxServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private final PayPalSandboxService service = new PayPalSandboxService(
            null, json, "https://api-m.sandbox.paypal.com", "", "", "USD",
            new BigDecimal("25000"), null);

    @Test
    void acceptsDepositWhenPayPalReturnsReferenceIdWithoutCustomId() throws Exception {
        JsonNode response = json.readTree("""
                {"purchase_units":[{"reference_id":"DEPOSIT-42"}]}
                """);

        JsonNode unit = service.verifiedPurchaseUnit(
                response, "DEPOSIT-42", "DEPOSIT-42", "Không đúng khoản cọc");

        assertEquals("DEPOSIT-42", unit.path("reference_id").asText());
    }

    @Test
    void acceptsCheckoutWhenCustomIdMatches() throws Exception {
        JsonNode response = json.readTree("""
                {"purchase_units":[{"custom_id":"17","reference_id":"SESSION-17"}]}
                """);

        JsonNode unit = service.verifiedPurchaseUnit(
                response, "17", "SESSION-17", "Không đúng phiên");

        assertEquals("17", unit.path("custom_id").asText());
    }

    @Test
    void rejectsOrderOwnedByAnotherDeposit() throws Exception {
        JsonNode response = json.readTree("""
                {"purchase_units":[{"custom_id":"DEPOSIT-99","reference_id":"DEPOSIT-99"}]}
                """);

        assertThrows(BusinessException.class, () -> service.verifiedPurchaseUnit(
                response, "DEPOSIT-42", "DEPOSIT-42", "Không đúng khoản cọc"));
    }
}
