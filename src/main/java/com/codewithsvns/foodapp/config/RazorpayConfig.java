
package com.codewithsvns.foodapp.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {

        System.out.println("Razorpay key ID starts with rzp_test_: "
                + (keyId != null && keyId.startsWith("rzp_test_")));

        System.out.println("Razorpay key ID length: "
                + (keyId == null ? -1 : keyId.length()));

        System.out.println("Razorpay secret present: "
                + (keySecret != null && !keySecret.isBlank()));

        System.out.println("Razorpay secret has surrounding whitespace: "
                + (keySecret != null && !keySecret.equals(keySecret.trim())));
        return new RazorpayClient(keyId, keySecret);
    }
}
