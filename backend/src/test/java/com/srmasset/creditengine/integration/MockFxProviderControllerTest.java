package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.integration.MockFxProviderController.MockFxProviderStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O bean do controller (e portanto o failureRate/contador) é compartilhado entre os testes da
 * classe — cada teste seta explicitamente o failureRate de que precisa antes de agir, em vez de
 * depender do default.
 */
@WebMvcTest(MockFxProviderController.class)
class MockFxProviderControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void cotacoes_semFalhaConfigurada_retornaCotacoesCompletas() throws Exception {
    mockMvc.perform(put("/mock/fx-provider/config").param("failureRate", "0.0"));

    mockMvc
        .perform(get("/mock/fx-provider/cotacoes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usdBrl").isNumber())
        .andExpect(jsonPath("$.brlUsd").isNumber())
        .andExpect(jsonPath("$.cdi").isNumber())
        .andExpect(jsonPath("$.sofr").isNumber())
        .andExpect(jsonPath("$.vigenteEm").isNotEmpty());
  }

  @Test
  void cotacoes_comFailureRateTotal_retorna503() throws Exception {
    mockMvc.perform(put("/mock/fx-provider/config").param("failureRate", "1.0"));

    mockMvc.perform(get("/mock/fx-provider/cotacoes")).andExpect(status().isServiceUnavailable());
  }

  @Test
  void stats_contaTodaChamadaDeCotacoes_inclusiveAsQueFalham() throws Exception {
    mockMvc.perform(put("/mock/fx-provider/config").param("failureRate", "1.0"));
    long antes = totalChamadas();

    mockMvc.perform(get("/mock/fx-provider/cotacoes"));
    mockMvc.perform(get("/mock/fx-provider/cotacoes"));

    assertThat(totalChamadas() - antes).isEqualTo(2);
  }

  @Test
  void configurar_valorForaDoIntervalo_clampaEmVezDeRejeitar() throws Exception {
    mockMvc
        .perform(put("/mock/fx-provider/config").param("failureRate", "7.5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.failureRate").value(1.0));

    mockMvc
        .perform(put("/mock/fx-provider/config").param("failureRate", "-3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.failureRate").value(0.0));
  }

  private long totalChamadas() throws Exception {
    String json =
        mockMvc
            .perform(get("/mock/fx-provider/stats"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readValue(json, MockFxProviderStats.class).totalChamadas();
  }
}
