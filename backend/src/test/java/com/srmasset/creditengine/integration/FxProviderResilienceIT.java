package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.srmasset.creditengine.dto.request.CedenteRequest;
import com.srmasset.creditengine.dto.request.LoteRecebivelRequest;
import com.srmasset.creditengine.dto.request.RecebivelRequest;
import com.srmasset.creditengine.dto.response.CedenteResponse;
import com.srmasset.creditengine.dto.response.LoteLiquidacaoResponse;
import com.srmasset.creditengine.dto.response.SincronizacaoTaxasResponse;
import com.srmasset.creditengine.integration.MockFxProviderController.MockFxProviderStats;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Resiliência de ponta a ponta, com HTTP real (o client chama o próprio servidor do teste, onde o
 * provider mockado vive): retry provado pelo contador do mock, circuit breaker provado pelo
 * short-circuit (falha sem nenhuma chamada chegar ao provider), e a semântica de degradação provada
 * liquidando um recebível com a última taxa persistida enquanto o provider está fora.
 *
 * <p>{@code DEFINED_PORT} (18081): o client monta a base URL a partir de {@code server.port} no
 * boot do contexto — com {@code RANDOM_PORT} a porta só existiria depois. {@code
 * wait-duration-in-open-state} vai pra 60s aqui pra o circuito não meio-abrir sozinho no meio das
 * fases (a config real de 10s é pra demo manual, não pra teste).
 *
 * <p>Fases num único {@code @Test}: o estado do circuit breaker é do contexto, não do método —
 * fases ordenadas num teste só evitam dependência implícita de ordem entre métodos.
 *
 * <p>Mesma limitação de ambiente dos outros ITs: requer Docker Engine compatível com o probe do
 * Testcontainers 1.21.x; roda no CI do GitHub, não neste ambiente de desenvolvimento local.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
      "server.port=18081",
      "resilience4j.circuitbreaker.instances.fxProvider.wait-duration-in-open-state=60s"
    })
@Testcontainers
class FxProviderResilienceIT {

  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private TestRestTemplate rest;

  @Test
  void resiliencia_fimAFim_retryCircuitoAbertoEDegradacaoGraciosa() {
    // --- Fase 1: happy path — sincroniza e persiste as taxas ---
    configurarFailureRate(0.0);
    ResponseEntity<SincronizacaoTaxasResponse> sincronizacao =
        rest.postForEntity("/api/taxas/sincronizar", null, SincronizacaoTaxasResponse.class);
    assertThat(sincronizacao.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(sincronizacao.getBody().usdBrl()).isPositive();

    ResponseEntity<String> taxaPersistida =
        rest.getForEntity("/api/taxas-cambio?moedaOrigem=USD&moedaDestino=BRL", String.class);
    assertThat(taxaPersistida.getStatusCode()).isEqualTo(HttpStatus.OK);

    // --- Fase 2: provider 100% fora — o retry acontece de verdade (3 tentativas no contador) ---
    configurarFailureRate(1.0);
    long chamadasAntesDoRetry = totalChamadasNoProvider();
    ResponseEntity<String> falha = rest.postForEntity("/api/taxas/sincronizar", null, String.class);
    assertThat(falha.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(falha.getBody()).contains("PROVIDER_INDISPONIVEL");
    assertThat(totalChamadasNoProvider() - chamadasAntesDoRetry)
        .as("max-attempts=3 do retry deve gerar exatamente 3 chamadas HTTP ao provider")
        .isEqualTo(3);

    // Janela do circuito neste ponto: 1 sucesso (fase 1) + 3 falhas = 4 chamadas
    // (minimum-number-of-calls), 75% de falha >= threshold de 50% => circuito ABERTO.

    // --- Fase 3: circuito aberto — falha rápido, sem nenhuma chamada chegar ao provider ---
    long chamadasAntesDoShortCircuit = totalChamadasNoProvider();
    ResponseEntity<String> falhaRapida =
        rest.postForEntity("/api/taxas/sincronizar", null, String.class);
    assertThat(falhaRapida.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(falhaRapida.getBody()).contains("PROVIDER_INDISPONIVEL");
    assertThat(totalChamadasNoProvider())
        .as("com o circuito aberto, o short-circuit não deve deixar nenhuma chamada sair")
        .isEqualTo(chamadasAntesDoShortCircuit);

    // --- Fase 4: degradação graciosa — liquidação segue com a última taxa persistida ---
    ResponseEntity<CedenteResponse> cedente =
        rest.postForEntity(
            "/api/cedentes",
            // CNPJ com dígito verificador válido — o POST /cedentes agora valida documento de
            // verdade
            new CedenteRequest("Empresa Resiliente", "11444777000161"),
            CedenteResponse.class);
    assertThat(cedente.getStatusCode()).isEqualTo(HttpStatus.OK);

    LoteRecebivelRequest lote =
        new LoteRecebivelRequest(
            List.of(
                new RecebivelRequest(
                    cedente.getBody().id(),
                    "DUPLICATA_MERCANTIL",
                    new BigDecimal("1000.00"),
                    "BRL",
                    LocalDate.now().plusDays(30),
                    "BRL")));
    ResponseEntity<LoteLiquidacaoResponse> liquidacao =
        rest.postForEntity("/api/recebiveis/lote", lote, LoteLiquidacaoResponse.class);
    assertThat(liquidacao.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(liquidacao.getBody().totalSucesso())
        .as("provider fora não pode impedir liquidação — a última taxa persistida continua válida")
        .isEqualTo(1);
  }

  private void configurarFailureRate(double failureRate) {
    ResponseEntity<MockFxProviderStats> resposta =
        rest.exchange(
            "/mock/fx-provider/config?failureRate=" + failureRate,
            HttpMethod.PUT,
            HttpEntity.EMPTY,
            MockFxProviderStats.class);
    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private long totalChamadasNoProvider() {
    return rest.getForEntity("/mock/fx-provider/stats", MockFxProviderStats.class)
        .getBody()
        .totalChamadas();
  }
}
