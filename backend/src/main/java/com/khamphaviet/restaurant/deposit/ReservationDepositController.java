package com.khamphaviet.restaurant.deposit;

import com.khamphaviet.restaurant.billing.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/reservations/{code}/deposit")
public class ReservationDepositController {
 private final ReservationDepositService deposits;private final PayPalSandboxService paypal;
 public ReservationDepositController(ReservationDepositService deposits,PayPalSandboxService paypal){this.deposits=deposits;this.paypal=paypal;}
 public record VerifyRequest(@NotBlank String phone){}
 public record CaptureRequest(@NotBlank String phone,@NotBlank String orderId){}
 @PostMapping("/qr")public ReservationDepositService.QrResponse qr(@PathVariable String code,@Valid @RequestBody VerifyRequest request){return deposits.qr(code,request.phone());}
 @PostMapping("/qr/confirm")public ReservationDepositService.DepositResponse confirmQr(@PathVariable String code,@Valid @RequestBody VerifyRequest request){return deposits.confirmQr(code,request.phone());}
 @GetMapping("/paypal/config")public CheckoutDtos.PayPalConfig paypalConfig(){return paypal.config();}
 @PostMapping("/paypal/orders")public CheckoutDtos.PayPalOrder createPayPal(@PathVariable String code,@Valid @RequestBody VerifyRequest request){return paypal.createDepositOrder(code,request.phone());}
 @PostMapping("/paypal/orders/capture")public ReservationDepositService.DepositResponse capturePayPal(@PathVariable String code,@Valid @RequestBody CaptureRequest request){return paypal.captureDepositOrder(code,request.phone(),request.orderId());}
}
