package com.pressing.pressing.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String SCHEME_BEARER = "bearerAuth";

	@Bean
	OpenAPI openAPI() {
		return new OpenAPI()
				.components(new Components()
						.addSecuritySchemes(SCHEME_BEARER, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Coller le token JWT obtenu via /api/auth/login (sans le prefixe 'Bearer ').")))
				.addSecurityItem(new SecurityRequirement().addList(SCHEME_BEARER))
				.info(new Info()
						.title("N-MAC Blanchisserie - API")
						.version("1.0.0")
						.description("API REST de gestion de blanchisserie/pressing : authentification, clients, commandes, "
								+ "livraisons, tarifs, fidelite, inventaire, caisse, paiements, SMS et rapports.\n\n"
								+ "**Authentification** : appelez `POST /api/auth/login`, copiez le champ `accessToken` de la reponse, "
								+ "puis cliquez sur le bouton **Authorize** et collez-le. Toutes les requetes protegees utiliseront ce jeton.")
						.contact(new Contact().name("N-MAC Blanchisserie").email("contact@nmac-pressing.com"))
						.license(new License().name("Proprietaire")));
	}
}
