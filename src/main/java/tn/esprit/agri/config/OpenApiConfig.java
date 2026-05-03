package tn.esprit.agri.config;  // Mets dans ton package config

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";  // Nom arbitraire, mais cohérent
        final String description = "JWT Auth (Bearer Token)";

        return new OpenAPI()
                .info(new Info()
                        .title("AgriProtect API")
                        .version("1.0")
                        .description("API pour la gestion agricole avec assurance"))

                // Déclare le schéma Bearer JWT
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")  // Indique que c'est du JWT
                                        .description(description)))

                // Applique globalement (tous les endpoints protégés utilisent ce scheme)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}