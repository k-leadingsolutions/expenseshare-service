package com.kleadingsolutions.expenseshare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure OpenAPI to include an OAuth2 security scheme for Google.
 * Swagger UI will show the "Authorize" button; set springdoc.oauth.clientId property or GOOGLE_CLIENT_ID env.
 */
@Configuration
public class SpringDocConfig {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Bean
    public OpenAPI api() {
        SecurityScheme oauthScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("Google OAuth2")
                .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl("https://accounts.google.com/o/oauth2/v2/auth")
                                .tokenUrl("https://oauth2.googleapis.com/token")
                                .scopes(null)
                        )
                );

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("googleOAuth2", oauthScheme))
                .addSecurityItem(new SecurityRequirement().addList("googleOAuth2"))
                .info(new Info().title("ExpenseShare API").version("1.0"));
    }
}