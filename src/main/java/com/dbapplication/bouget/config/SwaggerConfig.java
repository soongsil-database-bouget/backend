package com.dbapplication.bouget.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Bouget API",
                description = "Bouquet 추천 & 이미지 적용 서비스 API 명세",
                version = "v1"
        ),
        servers = {
                @Server(url = "http://52.78.57.66:8080", description = "AWS 웹 서버")
        }
)
public class SwaggerConfig {

    /**
     * 기본 그룹: 전체 컨트롤러 문서화
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .packagesToScan("com.dbapplication.bouget.controller")
                .build();
    }
}
