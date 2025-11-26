package com.dbapplication.bouget.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebClientConfig implements WebMvcConfigurer {

    @Value("${fastapi.base-url}")
    private String fastapiBaseUrl;
    @Value("${app.upload-dir}")
    private String uploadDir;

    @Bean
    public WebClient fastapiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(fastapiBaseUrl)
                .build();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /images/**  ->  file:/home/ec2-user/app/uploads/**
        String location = "file:" + uploadDir + "/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations(location);
    }

}
