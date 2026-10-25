
package com.codewithsvns.foodapp.controller;

import com.codewithsvns.foodapp.service.RazorpayWebhookService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/razorpay")
public class RazorpayWebhookController {

    private final RazorpayWebhookService razorpayWebhookService;

    public RazorpayWebhookController(
            RazorpayWebhookService razorpayWebhookService) {
        this.razorpayWebhookService = razorpayWebhookService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(
                    name = "X-Razorpay-Signature",
                    required = false
            ) String signature) {

        razorpayWebhookService.processWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
