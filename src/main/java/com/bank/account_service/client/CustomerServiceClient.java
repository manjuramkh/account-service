package com.bank.account_service.client;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;
import java.util.logging.Logger;

@Slf4j
@Component
public class CustomerServiceClient {

    private final WebClient webClient;

    Logger logger = Logger.getLogger(CustomerServiceClient.class.getName());

    public CustomerServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:7072").build();
    }

    @Retry(name = "customerServiceRetry", fallbackMethod = "isVerifiedFallback")
    public boolean isVerified(UUID customerId) {
        try {
            Boolean verified = webClient.get()
                    .uri("/api/v1/customers/{id}/verified", customerId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            return Boolean.TRUE.equals(verified);
        }
        catch (Exception e) {
            // Handle the exception (e.g., log it)
            return false;
        }
    }

    private boolean isVerifiedFallback(UUID customerId, Throwable throwable) {
        // Log the exception or handle it as needed
        logger.warning("Fallback method called for isVerified due to: {}"+ throwable.getMessage());
        return false; // Return a default value or handle the fallback logic
    }
}
