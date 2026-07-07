package com.srmasset.creditengine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados do contrato OpenAPI (título/descrição/versão) exibidos no Swagger UI e exportados em
 * {@code /v3/api-docs} — snapshot versionado em {@code docs/openapi.json} pra importar em clientes
 * REST (Bruno, Postman, Insomnia).
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI creditEngineOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("SRM Credit Engine API")
                .description(
                    "Precificação e liquidação de recebíveis multimoedas (BRL/USD): câmbio e taxas"
                        + " de mercado, simulação, liquidação em lote com estorno e extrato"
                        + " analítico. Endpoints em /mock/fx-provider simulam o provider externo"
                        + " de cotações (knob de falha pra demonstrar retry/circuit breaker).")
                .version("v1"));
  }
}
