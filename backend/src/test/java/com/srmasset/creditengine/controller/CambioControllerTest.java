package com.srmasset.creditengine.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.TaxaCambio;
import com.srmasset.creditengine.dto.request.TaxaCambioRequest;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.TaxaCambioIndisponivelException;
import com.srmasset.creditengine.service.CambioService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CambioController.class)
class CambioControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CambioService cambioService;

  @Test
  void registrar_caminhoFeliz_retorna200ComTaxaCadastrada() throws Exception {
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    Moeda usd = Moeda.builder().codigo("USD").nome("Dólar Americano").build();
    TaxaCambio taxaCambio =
        TaxaCambio.builder()
            .moedaOrigem(usd)
            .moedaDestino(brl)
            .valor(new BigDecimal("5.1234"))
            .vigenteEm(Instant.parse("2026-07-01T00:00:00Z"))
            .build();
    when(cambioService.registrar(
            "USD", "BRL", new BigDecimal("5.1234"), Instant.parse("2026-07-01T00:00:00Z")))
        .thenReturn(taxaCambio);

    var request =
        new TaxaCambioRequest(
            "USD", "BRL", new BigDecimal("5.1234"), Instant.parse("2026-07-01T00:00:00Z"));

    mockMvc
        .perform(
            post("/api/taxas-cambio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.moedaOrigem").value("USD"))
        .andExpect(jsonPath("$.moedaDestino").value("BRL"))
        .andExpect(jsonPath("$.valor").value(5.1234));
  }

  @Test
  void registrar_payloadInvalido_retorna400ComCampoInvalido() throws Exception {
    String payloadSemMoedaOrigem =
        """
        {"moedaOrigem":"","moedaDestino":"BRL","valor":5.1234,"vigenteEm":"2026-07-01T00:00:00Z"}
        """;

    mockMvc
        .perform(
            post("/api/taxas-cambio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadSemMoedaOrigem))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"))
        .andExpect(jsonPath("$.camposInvalidos[0].campo").value("moedaOrigem"));
  }

  @Test
  void registrar_moedaInexistente_retorna404() throws Exception {
    when(cambioService.registrar(any(), any(), any(), any()))
        .thenThrow(new MoedaNaoEncontradaException("XYZ"));

    var request = new TaxaCambioRequest("XYZ", "BRL", new BigDecimal("5.1234"), Instant.now());

    mockMvc
        .perform(
            post("/api/taxas-cambio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("MOEDA_NAO_ENCONTRADA"));
  }

  @Test
  void buscarVigente_caminhoFeliz_retorna200() throws Exception {
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    Moeda usd = Moeda.builder().codigo("USD").nome("Dólar Americano").build();
    TaxaCambio taxaCambio =
        TaxaCambio.builder()
            .moedaOrigem(usd)
            .moedaDestino(brl)
            .valor(new BigDecimal("5.1234"))
            .build();
    when(cambioService.buscarTaxaVigente("USD", "BRL")).thenReturn(taxaCambio);

    mockMvc
        .perform(get("/api/taxas-cambio").param("moedaOrigem", "USD").param("moedaDestino", "BRL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valor").value(5.1234));
  }

  @Test
  void buscarVigente_semTaxaCadastrada_retorna422() throws Exception {
    when(cambioService.buscarTaxaVigente("USD", "BRL"))
        .thenThrow(new TaxaCambioIndisponivelException("USD", "BRL"));

    mockMvc
        .perform(get("/api/taxas-cambio").param("moedaOrigem", "USD").param("moedaDestino", "BRL"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("TAXA_CAMBIO_INDISPONIVEL"));
  }

  @Test
  void buscarVigente_semParametroObrigatorio_retorna400NaoQuinhentos() throws Exception {
    mockMvc
        .perform(get("/api/taxas-cambio").param("moedaOrigem", "USD"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ERRO_INESPERADO"))));
  }
}
