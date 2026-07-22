package com.khamphaviet.restaurant.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.khamphaviet.restaurant.common.BusinessException;
import com.khamphaviet.restaurant.deposit.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
public class PayPalSandboxService {
    private final CheckoutService checkout;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final String currency;
    private final BigDecimal vndPerUsd;
    private final ReservationDepositService deposits;

    public PayPalSandboxService(CheckoutService checkout, ObjectMapper json,
            @Value("${app.paypal.base-url}") String baseUrl,
            @Value("${app.paypal.client-id:}") String clientId,
            @Value("${app.paypal.client-secret:}") String clientSecret,
            @Value("${app.paypal.currency:USD}") String currency,
            @Value("${app.paypal.vnd-per-usd:25000}") BigDecimal vndPerUsd, ReservationDepositService deposits) {
        this.checkout = checkout;
        this.json = json;
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.currency = currency;
        this.vndPerUsd = vndPerUsd;
        this.deposits = deposits;
    }

    public CheckoutDtos.PayPalConfig config() {
        return new CheckoutDtos.PayPalConfig(enabled(), enabled() ? clientId : "", currency, vndPerUsd);
    }

    public CheckoutDtos.PayPalOrder createOrder(Long sessionId, BigDecimal discount) {
        ensureEnabled();
        var draft = checkout.prepareExternalPayment(sessionId, discount);
        BigDecimal amount = toPayPalAmount(draft.total());
        if (amount.signum() <= 0) throw new BusinessException("PayPal không hỗ trợ hóa đơn có tổng tiền bằng 0");

        ObjectNode amountNode = json.createObjectNode().put("currency_code", currency).put("value", amount.toPlainString());
        ObjectNode unit = json.createObjectNode()
                .put("reference_id", "SESSION-" + sessionId)
                .put("custom_id", sessionId.toString())
                .put("description", "Hoa don nha hang Kham Pha Viet")
                .set("amount", amountNode);
        ObjectNode body = json.createObjectNode().put("intent", "CAPTURE");
        body.putArray("purchase_units").add(unit);

        JsonNode response = sendJson("/v2/checkout/orders", "POST", body.toString(), true);
        String orderId = response.path("id").asText();
        if (orderId.isBlank()) throw new BusinessException("PayPal không trả về mã đơn hàng");
        return new CheckoutDtos.PayPalOrder(orderId, response.path("status").asText(), currency, amount, approvalUrl(response));
    }

    public CheckoutDtos.PayPalOrder createDepositOrder(String code,String phone) {
        ensureEnabled(); ReservationDeposit deposit=deposits.verified(code,phone);
        if(deposit.getStatus()==DepositStatus.PAID) throw new BusinessException("Khoản đặt cọc đã được thanh toán");
        BigDecimal amount=toPayPalAmount(deposit.getAmount());
        ObjectNode amountNode=json.createObjectNode().put("currency_code",currency).put("value",amount.toPlainString());
        ObjectNode unit=json.createObjectNode().put("reference_id","DEPOSIT-"+deposit.getReservationId()).put("custom_id","DEPOSIT-"+deposit.getReservationId()).put("description","Dat coc Kham Pha Viet").set("amount",amountNode);
        ObjectNode body=json.createObjectNode().put("intent","CAPTURE");body.putArray("purchase_units").add(unit);
        JsonNode response=sendJson("/v2/checkout/orders","POST",body.toString(),true);String orderId=response.path("id").asText();
        if(orderId.isBlank())throw new BusinessException("PayPal không trả về mã đơn hàng");
        return new CheckoutDtos.PayPalOrder(orderId,response.path("status").asText(),currency,amount,approvalUrl(response));
    }

    public ReservationDepositService.DepositResponse captureDepositOrder(String code,String phone,String orderId){
        ensureEnabled();ReservationDeposit deposit=deposits.verified(code,phone);BigDecimal expected=toPayPalAmount(deposit.getAmount());
        JsonNode response=sendJson("/v2/checkout/orders/"+url(orderId)+"/capture","POST","{}",true);
        if(!"COMPLETED".equals(response.path("status").asText()))throw new BusinessException("PayPal chưa hoàn tất giao dịch");
        JsonNode unit=response.path("purchase_units").path(0);if(!("DEPOSIT-"+deposit.getReservationId()).equals(unit.path("custom_id").asText()))throw new BusinessException("Đơn PayPal không thuộc khoản cọc này");
        JsonNode capture=unit.path("payments").path("captures").path(0);String captureId=capture.path("id").asText();
        if(!currency.equals(capture.path("amount").path("currency_code").asText())||expected.compareTo(decimal(capture.path("amount").path("value").asText()))!=0)throw new BusinessException("Số tiền PayPal không khớp khoản cọc");
        return deposits.completePayPal(deposit.getReservationId(),orderId,captureId);
    }

    public CheckoutDtos.Checkout captureOrder(Long sessionId, String orderId, BigDecimal discount) {
        ensureEnabled();
        var draft = checkout.prepareExternalPayment(sessionId, discount);
        BigDecimal expected = toPayPalAmount(draft.total());
        JsonNode response = sendJson("/v2/checkout/orders/" + url(orderId) + "/capture", "POST", "{}", true);
        if (!"COMPLETED".equals(response.path("status").asText())) throw new BusinessException("PayPal chưa hoàn tất giao dịch");

        JsonNode unit = response.path("purchase_units").path(0);
        if (!sessionId.toString().equals(unit.path("custom_id").asText())) throw new BusinessException("Đơn PayPal không thuộc phiên phục vụ này");
        JsonNode capture = unit.path("payments").path("captures").path(0);
        String captureId = capture.path("id").asText();
        String capturedCurrency = capture.path("amount").path("currency_code").asText();
        BigDecimal capturedAmount = decimal(capture.path("amount").path("value").asText());
        if (captureId.isBlank() || !currency.equals(capturedCurrency) || expected.compareTo(capturedAmount) != 0)
            throw new BusinessException("Số tiền PayPal không khớp với hóa đơn");
        return checkout.completePayPalPayment(sessionId, discount, orderId, captureId);
    }

    private JsonNode sendJson(String path, String method, String body, boolean authenticated) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30)).header("Accept", "application/json");
            if (authenticated) builder.header("Authorization", "Bearer " + accessToken());
            if ("POST".equals(method)) builder.header("Content-Type", "application/json")
                    .header("PayPal-Request-Id", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new BusinessException("PayPal Sandbox từ chối giao dịch: " + paypalMessage(response.body()));
            return json.readTree(response.body());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Không thể kết nối PayPal Sandbox");
        }
    }

    private String accessToken() throws Exception {
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String form = "grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/oauth2/token"))
                .timeout(Duration.ofSeconds(20)).header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new BusinessException("Thông tin PayPal Sandbox không hợp lệ");
        String token = json.readTree(response.body()).path("access_token").asText();
        if (token.isBlank()) throw new BusinessException("PayPal không trả về access token");
        return token;
    }

    private String paypalMessage(String body) {
        try {
            JsonNode value = json.readTree(body);
            String message = value.path("message").asText();
            return message.isBlank() ? "Lỗi không xác định" : message;
        } catch (Exception ignored) {
            return "Lỗi không xác định";
        }
    }

    private String approvalUrl(JsonNode response){for(JsonNode link:response.path("links"))if("approve".equals(link.path("rel").asText()))return link.path("href").asText();return "";}

    private BigDecimal toPayPalAmount(BigDecimal vnd) {
        if (vndPerUsd.signum() <= 0) throw new BusinessException("Tỷ giá PayPal không hợp lệ");
        return vnd.divide(vndPerUsd, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(String value) {
        try { return new BigDecimal(value); } catch (Exception e) { throw new BusinessException("PayPal trả về số tiền không hợp lệ"); }
    }

    private boolean enabled() { return !clientId.isBlank() && !clientSecret.isBlank(); }
    private void ensureEnabled() { if (!enabled()) throw new BusinessException("PayPal Sandbox chưa được cấu hình trên backend"); }
    private String url(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
