package com.projetCloud.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(security = { @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME) })
@SecurityScheme(
        name = OpenApiConfig.SECURITY_SCHEME_NAME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "Opaque",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public io.swagger.v3.oas.models.OpenAPI customOpenAPI() {
        return new io.swagger.v3.oas.models.OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Projet Cloud API")
                        .description("API REST pour le projet cloud de gestion des signalements")
                        .version("1.0.0")
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Équipe Projet Cloud")
                                .email("contact@projetcloud.com"))
                        .license(new io.swagger.v3.oas.models.info.License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name("Authorization")
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("Opaque")))
                .servers(List.of(
                        new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8080").description("Serveur de développement"),
                        new io.swagger.v3.oas.models.servers.Server().url("https://api.projetcloud.com").description("Serveur de production")
                ));
    }
}