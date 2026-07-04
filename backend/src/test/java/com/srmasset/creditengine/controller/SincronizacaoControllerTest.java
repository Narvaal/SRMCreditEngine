package com.srmasset.creditengine.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srmasset.creditengine.dto.response.SincronizacaoTaxasResponse;
import com.srmasset.creditengine.exception.ProviderIndisponivelException;
import com.srmasset.creditengine.service.SincronizacaoTaxasService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SincronizacaoController.class)
class SincronizacaoControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SincronizacaoTaxasService sincronizacaoTaxasService;

  @Test
  void sincronizar_caminhoFeliz_retorna200ComAsTaxasSincronizadas() throws Exception {
    when(sincronizacaoTaxasService.sincronizar())
        .thenReturn(
            new SincronizacaoTaxasResponse(
                new BigDecimal("5.40000000"),
                new BigDecimal("0.18518519"),
                new BigDecimal("0.010000"),
                new BigDecimal("0.004500"),
                Instant.parse("2026-07-04T12:00:00Z")));

    mockMvc
        .perform(post("/api/taxas/sincronizar"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usdBrl").value(5.4))
        .andExpect(jsonPath("$.cdi").value(0.01));
  }

  @Test
  void sincronizar_providerFora_retorna503ComCodigoDeDominio() throws Exception {
    when(sincronizacaoTaxasService.sincronizar())
        .thenThrow(new ProviderIndisponivelException("retry esgotado"));

    mockMvc
        .perform(post("/api/taxas/sincronizar"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.codigo").value("PROVIDER_INDISPONIVEL"));
  }
}
