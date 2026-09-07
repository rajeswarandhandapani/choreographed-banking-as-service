package com.rajeswaran.apigateway.config;

import com.rajeswaran.apigateway.client.AdminDashboardClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

    @org.springframework.beans.factory.annotation.Value("${banking.gateway-base-url:http://localhost:${server.port:8080}}")
    private String baseUrl;

    @Bean
    AdminDashboardClient adminDashboardClient() {

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        WebClientAdapter webClientAdapter = WebClientAdapter.create(webClient);

        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builderFor(webClientAdapter)
                .build();

        return httpServiceProxyFactory.createClient(AdminDashboardClient.class);
    }
}
