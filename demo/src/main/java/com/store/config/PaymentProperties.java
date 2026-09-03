package com.store.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class PaymentProperties {

    private Bank bank = new Bank();
    private Sepay sepay = new Sepay();

    @Getter
    @Setter
    public static class Bank {
        private String id = "";
        private String accountNo = "";
        private String accountName = "";
    }

    @Getter
    @Setter
    public static class Sepay {
        private boolean enabled = false;
        private String webhookApikey = "";
    }

    @PostConstruct
    public void validate() {
        if (sepay.isEnabled()) {
            if (!StringUtils.hasText(sepay.getWebhookApikey())) {
                throw new IllegalStateException("app.sepay.webhook-apikey is required when app.sepay.enabled=true");
            }
            if (!StringUtils.hasText(bank.getAccountNo())) {
                throw new IllegalStateException("app.bank.account-no is required when app.sepay.enabled=true");
            }
            if (!StringUtils.hasText(bank.getId())) {
                throw new IllegalStateException("app.bank.id is required when app.sepay.enabled=true");
            }
            if (!StringUtils.hasText(bank.getAccountName())) {
                throw new IllegalStateException("app.bank.account-name is required when app.sepay.enabled=true");
            }
        }
    }
}
