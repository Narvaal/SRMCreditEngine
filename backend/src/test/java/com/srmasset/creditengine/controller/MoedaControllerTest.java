package com.srmasset.creditengine.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.service.MoedaService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MoedaController.class)
class MoedaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MoedaService moedaService;

  @Test
  void listar_retornaCatalogoDeMoedas() throws Exception {
    Moeda brl =
        Moeda.builder().codigo("BRL").nome("Real Brasileiro").casasDecimais((short) 2).build();
    Moeda usd =
        Moeda.builder().codigo("USD").nome("Dólar Americano").casasDecimais((short) 2).build();
    when(moedaService.listarTodas()).thenReturn(List.of(brl, usd));

    mockMvc
        .perform(get("/api/moedas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].codigo").value("BRL"))
        .andExpect(jsonPath("$[1].codigo").value("USD"));
  }

  @Test
  void listar_semMoedasCadastradas_retornaListaVazia() throws Exception {
    when(moedaService.listarTodas()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/moedas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
