package com.srmasset.creditengine.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.dto.request.TaxaMercadoRequest;
import com.srmasset.creditengine.exception.TaxaMercadoIndisponivelException;
import com.srmasset.creditengine.service.TaxaMercadoService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaxaMercadoController.class)
class TaxaMercadoControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TaxaMercadoService taxaMercadoService;

  @Test
  void registrar_caminhoFeliz_retorna200() throws Exception {
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    TaxaMercado cdi =
        TaxaMercado.builder()
            .moeda(brl)
            .indicador("CDI")
            .valor(new BigDecimal("0.010000"))
            .vigenteEm(Instant.parse("2026-07-01T00:00:00Z"))
            .build();
    when(taxaMercadoService.registrar(
            "BRL", "CDI", new BigDecimal("0.010000"), Instant.parse("2026-07-01T00:00:00Z")))
        .thenReturn(cdi);

    var request =
        new TaxaMercadoRequest(
            "BRL", "CDI", new BigDecimal("0.010000"), Instant.parse("2026-07-01T00:00:00Z"));

    mockMvc
        .perform(
            post("/api/taxas-mercado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicador").value("CDI"));
  }

  @Test
  void registrar_indicadorEmBranco_retorna400() throws Exception {
    String payload =
        """
        {"moedaCodigo":"BRL","indicador":"","valor":0.01,"vigenteEm":"2026-07-01T00:00:00Z"}
        """;

    mockMvc
        .perform(
            post("/api/taxas-mercado").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.camposInvalidos[0].campo").value("indicador"));
  }

  @Test
  void buscarVigente_caminhoFeliz_retorna200() throws Exception {
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    TaxaMercado cdi =
        TaxaMercado.builder().moeda(brl).indicador("CDI").valor(new BigDecimal("0.010000")).build();
    when(taxaMercadoService.buscarTaxaVigente("BRL")).thenReturn(cdi);

    mockMvc
        .perform(get("/api/taxas-mercado").param("moedaCodigo", "BRL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.indicador").value("CDI"));
  }

  @Test
  void buscarVigente_semTaxaCadastrada_retorna422() throws Exception {
    when(taxaMercadoService.buscarTaxaVigente("USD"))
        .thenThrow(new TaxaMercadoIndisponivelException("USD", "SOFR"));

    mockMvc
        .perform(get("/api/taxas-mercado").param("moedaCodigo", "USD"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("TAXA_MERCADO_INDISPONIVEL"));
  }

  @Test
  void buscarVigente_semParametroObrigatorio_retorna400NaoQuinhentos() throws Exception {
    mockMvc
        .perform(get("/api/taxas-mercado"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"));
  }
}
