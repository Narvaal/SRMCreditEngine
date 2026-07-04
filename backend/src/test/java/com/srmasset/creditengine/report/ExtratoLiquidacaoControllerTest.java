package com.srmasset.creditengine.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srmasset.creditengine.dto.PaginaResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExtratoLiquidacaoController.class)
class ExtratoLiquidacaoControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ExtratoLiquidacaoRepository extratoLiquidacaoRepository;

  private ExtratoLiquidacaoLinha linha() {
    return new ExtratoLiquidacaoLinha(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Acme Ltda",
        "LIQUIDACAO",
        "BRL",
        "BRL",
        new BigDecimal("1000.00"),
        new BigDecimal("975.61"),
        Instant.parse("2026-07-01T00:00:00Z"));
  }

  @Test
  void buscar_semFiltros_usaDefaultsDePaginacao() throws Exception {
    when(extratoLiquidacaoRepository.buscar(any()))
        .thenReturn(PaginaResponse.de(List.of(linha()), 0, 20, 1));

    mockMvc
        .perform(get("/api/relatorios/extrato-liquidacao"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].cedenteNome").value("Acme Ltda"));
  }

  @Test
  void buscar_comFiltrosCompletos_repassaFiltroMontado() throws Exception {
    UUID cedenteId = UUID.randomUUID();
    when(extratoLiquidacaoRepository.buscar(
            new ExtratoLiquidacaoFiltro(
                cedenteId,
                "USD",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                2,
                10)))
        .thenReturn(PaginaResponse.de(List.of(), 2, 10, 0));

    mockMvc
        .perform(
            get("/api/relatorios/extrato-liquidacao")
                .param("cedenteId", cedenteId.toString())
                .param("moeda", "USD")
                .param("dataInicio", "2026-01-01T00:00:00Z")
                .param("dataFim", "2026-02-01T00:00:00Z")
                .param("page", "2")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(10));
  }

  @Test
  void buscar_cedenteIdMalformado_retorna400NaoQuinhentos() throws Exception {
    mockMvc
        .perform(get("/api/relatorios/extrato-liquidacao").param("cedenteId", "nao-e-um-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"));
  }
}
