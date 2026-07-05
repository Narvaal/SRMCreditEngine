package com.srmasset.creditengine.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.dto.request.LoteRecebivelRequest;
import com.srmasset.creditengine.dto.request.RecebivelRequest;
import com.srmasset.creditengine.dto.request.SimulacaoRecebivelRequest;
import com.srmasset.creditengine.dto.response.LiquidacaoItemResultado;
import com.srmasset.creditengine.dto.response.LoteLiquidacaoResponse;
import com.srmasset.creditengine.dto.response.SimulacaoRecebivelResponse;
import com.srmasset.creditengine.exception.TipoRecebivelNaoSuportadoException;
import com.srmasset.creditengine.service.LiquidacaoBatchService;
import com.srmasset.creditengine.service.SimulacaoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecebivelController.class)
class RecebivelControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private LiquidacaoBatchService liquidacaoBatchService;
  @MockitoBean private SimulacaoService simulacaoService;

  private RecebivelRequest itemValido() {
    return new RecebivelRequest(
        UUID.randomUUID(),
        "DUPLICATA_MERCANTIL",
        new BigDecimal("1000.00"),
        "BRL",
        LocalDate.of(2099, 8, 1),
        "BRL");
  }

  @Test
  void processarLote_caminhoFeliz_retorna200ComResultadoPorItem() throws Exception {
    LiquidacaoItemResultado item =
        LiquidacaoItemResultado.falha(null, "SALDO_INSUFICIENTE", "sem saldo");
    when(liquidacaoBatchService.processarLote(any()))
        .thenReturn(new LoteLiquidacaoResponse(1, 0, 1, List.of(item)));

    mockMvc
        .perform(
            post("/api/recebiveis/lote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoteRecebivelRequest(List.of(itemValido())))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItens").value(1))
        .andExpect(jsonPath("$.totalFalha").value(1))
        .andExpect(jsonPath("$.itens[0].codigoErro").value("SALDO_INSUFICIENTE"));
  }

  @Test
  void processarLote_listaVazia_retorna400() throws Exception {
    mockMvc
        .perform(
            post("/api/recebiveis/lote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoteRecebivelRequest(List.of()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"));
  }

  @Test
  void processarLote_itemComValorFaceNegativo_retorna400() throws Exception {
    RecebivelRequest itemInvalido =
        new RecebivelRequest(
            UUID.randomUUID(),
            "DUPLICATA_MERCANTIL",
            new BigDecimal("-10.00"),
            "BRL",
            LocalDate.of(2099, 8, 1),
            "BRL");

    mockMvc
        .perform(
            post("/api/recebiveis/lote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoteRecebivelRequest(List.of(itemInvalido)))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"));
  }

  @Test
  void simular_caminhoFeliz_retorna200SemPersistirNada() throws Exception {
    SimulacaoRecebivelResponse resposta =
        new SimulacaoRecebivelResponse(
            new BigDecimal("1000.00"),
            "BRL",
            new BigDecimal("0.010000"),
            new BigDecimal("0.015000"),
            new BigDecimal("1.0000"),
            new BigDecimal("975.609756"),
            "BRL",
            null,
            new BigDecimal("975.61"));
    when(simulacaoService.simular(any())).thenReturn(resposta);

    var request =
        new SimulacaoRecebivelRequest(
            "DUPLICATA_MERCANTIL",
            new BigDecimal("1000.00"),
            "BRL",
            LocalDate.of(2099, 8, 1),
            "BRL");

    mockMvc
        .perform(
            post("/api/recebiveis/simular")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valorLiquido").value(975.61));
  }

  @Test
  void simular_valorFaceAcimaDeUmQuadrilhao_retorna400ComCampoDetalhado() throws Exception {
    var request =
        new SimulacaoRecebivelRequest(
            "DUPLICATA_MERCANTIL",
            new BigDecimal("5550000000000000000000000000000000000000"),
            "BRL",
            LocalDate.of(2099, 8, 1),
            "BRL");

    mockMvc
        .perform(
            post("/api/recebiveis/simular")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.camposInvalidos[0].campo").value("valorFace"));
  }

  @Test
  void simular_tipoRecebivelNaoSuportado_retorna400() throws Exception {
    when(simulacaoService.simular(any()))
        .thenThrow(new TipoRecebivelNaoSuportadoException("INEXISTENTE"));

    var request =
        new SimulacaoRecebivelRequest(
            "INEXISTENTE", new BigDecimal("1000.00"), "BRL", LocalDate.of(2099, 8, 1), "BRL");

    mockMvc
        .perform(
            post("/api/recebiveis/simular")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("TIPO_RECEBIVEL_NAO_SUPORTADO"));
  }
}
