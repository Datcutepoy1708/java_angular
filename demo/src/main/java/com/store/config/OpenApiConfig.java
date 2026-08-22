package com.store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI storeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Computer Store API")
                        .description("REST API documentation for Computer & PC Components E-commerce Platform")
                        .version("1.0.0")
                        .contact(new Contact().name("Store Dev Team")));
    }
}
