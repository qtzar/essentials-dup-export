package com.qtzar.essentialsexport.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Essential Sync API")
                        .description("Integration Application for Essential Architecture Tool")
                        .version("v0.0.1")
                        .license(new License().name("(C)2024 Dick's Sporting Goods.").url("http://dicks.com")));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
