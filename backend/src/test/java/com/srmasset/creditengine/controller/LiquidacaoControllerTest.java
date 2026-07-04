package com.srmasset.creditengine.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.domain.Liquidacao;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.domain.TipoLiquidacao;
import com.srmasset.creditengine.exception.EstornoInvalidoException;
import com.srmasset.creditengine.service.LiquidacaoService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LiquidacaoController.class)
class LiquidacaoControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LiquidacaoService liquidacaoService;

  @Test
  void estornar_caminhoFeliz_retorna200() throws Exception {
    UUID liquidacaoId = UUID.randomUUID();
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    Cedente cedente =
        Cedente.builder().id(UUID.randomUUID()).nome("Acme Ltda").documento("123").build();
    Liquidacao estorno =
        Liquidacao.builder()
            .id(UUID.randomUUID())
            .recebivel(Recebivel.builder().id(UUID.randomUUID()).build())
            .cedente(cedente)
            .tipo(TipoLiquidacao.ESTORNO)
            .valorFace(new BigDecimal("1000.00"))
            .moedaTitulo(brl)
            .taxaBaseUsada(new BigDecimal("0.010000"))
            .spreadUsado(new BigDecimal("0.015000"))
            .prazoMesesUsado(new BigDecimal("1.0000"))
            .valorPresente(new BigDecimal("975.609756"))
            .moedaPagamento(brl)
            .valorLiquido(new BigDecimal("975.61"))
            .build();
    when(liquidacaoService.estornar(liquidacaoId)).thenReturn(estorno);

    mockMvc
        .perform(post("/api/liquidacoes/{id}/estorno", liquidacaoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipo").value("ESTORNO"));
  }

  @Test
  void estornar_invalido_retorna409() throws Exception {
    UUID liquidacaoId = UUID.randomUUID();
    when(liquidacaoService.estornar(liquidacaoId))
        .thenThrow(new EstornoInvalidoException(liquidacaoId, "já foi estornada anteriormente"));

    mockMvc
        .perform(post("/api/liquidacoes/{id}/estorno", liquidacaoId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("ESTORNO_INVALIDO"));
  }

  @Test
  void estornar_idMalformado_retorna400NaoQuinhentos() throws Exception {
    mockMvc
        .perform(post("/api/liquidacoes/{id}/estorno", "nao-e-um-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("REQUEST_INVALIDO"));
  }
}
