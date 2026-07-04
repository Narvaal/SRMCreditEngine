package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * Testa só o contrato HTTP do client (parse e propagação de erro) — sem contexto Spring, as
 * anotações {@code @Retry}/{@code @CircuitBreaker} são inertes aqui. O comportamento de resiliência
 * de verdade é coberto por {@code FxProviderResilienceIT}.
 */
class FxProviderClientTest {

  @Test
  void buscarCotacoes_parseiaARespostaDoProvider() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    FxProviderClient client = new FxProviderClient(builder, "http://provider-teste");

    server
        .expect(requestTo("http://provider-teste/cotacoes"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {"usdBrl":5.40000000,"brlUsd":0.18518519,"cdi":0.010000,"sofr":0.004500,
                 "vigenteEm":"2026-07-04T12:00:00Z"}
                """,
                MediaType.APPLICATION_JSON));

    CotacoesProviderResponse cotacoes = client.buscarCotacoes();

    assertThat(cotacoes.usdBrl()).isEqualByComparingTo("5.40");
    assertThat(cotacoes.brlUsd()).isEqualByComparingTo("0.18518519");
    assertThat(cotacoes.cdi()).isEqualByComparingTo("0.010000");
    assertThat(cotacoes.sofr()).isEqualByComparingTo("0.004500");
    assertThat(cotacoes.vigenteEm()).isEqualTo(Instant.parse("2026-07-04T12:00:00Z"));
    server.verify();
  }

  @Test
  void buscarCotacoes_provider503_propagaExcecaoHttp() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    FxProviderClient client = new FxProviderClient(builder, "http://provider-teste");

    server
        .expect(requestTo("http://provider-teste/cotacoes"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(client::buscarCotacoes).isInstanceOf(HttpServerErrorException.class);
    server.verify();
  }
}
