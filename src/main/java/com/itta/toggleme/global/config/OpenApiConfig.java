package com.itta.toggleme.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI에 Authorize 버튼을 띄운다. 로그인 응답의 accessToken을 넣으면 인증이 필요한 API를 호출할 수 있다
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme(BEARER)
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
