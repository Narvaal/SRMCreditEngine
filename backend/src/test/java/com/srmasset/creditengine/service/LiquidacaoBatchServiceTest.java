package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.domain.Liquidacao;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.domain.StatusRecebivel;
import com.srmasset.creditengine.domain.TipoLiquidacao;
import com.srmasset.creditengine.dto.request.RecebivelRequest;
import com.srmasset.creditengine.dto.response.LiquidacaoItemResultado;
import com.srmasset.creditengine.dto.response.LoteLiquidacaoResponse;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.SaldoInsuficienteException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link LiquidacaoBatchService} orquestra o lote item a item — o contrato central é resultado
 * <b>parcial</b> (um item falho não deve impedir nem reverter os demais, e nunca deve lançar
 * exceção pro chamador). Mocka {@link RecebivelService} e {@link LiquidacaoService} porque o que
 * está sob teste aqui é a orquestração, não as regras de cada um (já cobertas em seus próprios
 * testes).
 */
@ExtendWith(MockitoExtension.class)
class LiquidacaoBatchServiceTest {

  @Mock private RecebivelService recebivelService;
  @Mock private LiquidacaoService liquidacaoService;

  @InjectMocks private LiquidacaoBatchService liquidacaoBatchService;

  @Test
  void processarLote_listaVazia_retornaTotaisZerados() {
    LoteLiquidacaoResponse resposta = liquidacaoBatchService.processarLote(List.of());

    assertThat(resposta.totalItens()).isZero();
    assertThat(resposta.totalSucesso()).isZero();
    assertThat(resposta.totalFalha()).isZero();
    assertThat(resposta.itens()).isEmpty();
  }

  @Test
  void processarLote_todosOsItensComSucesso_retornaTotaisCorretos() {
    RecebivelRequest requestA = requestValido();
    RecebivelRequest requestB = requestValido();
    Recebivel recebivelA = recebivelPendente();
    Recebivel recebivelB = recebivelPendente();
    Liquidacao liquidacaoA = liquidacaoDe(recebivelA);
    Liquidacao liquidacaoB = liquidacaoDe(recebivelB);

    when(recebivelService.criar(requestA)).thenReturn(recebivelA);
    when(recebivelService.criar(requestB)).thenReturn(recebivelB);
    when(liquidacaoService.liquidar(recebivelA.getId(), requestA.moedaPagamento()))
        .thenReturn(liquidacaoA);
    when(liquidacaoService.liquidar(recebivelB.getId(), requestB.moedaPagamento()))
        .thenReturn(liquidacaoB);

    LoteLiquidacaoResponse resposta =
        liquidacaoBatchService.processarLote(List.of(requestA, requestB));

    assertThat(resposta.totalItens()).isEqualTo(2);
    assertThat(resposta.totalSucesso()).isEqualTo(2);
    assertThat(resposta.totalFalha()).isZero();
    assertThat(resposta.itens()).allMatch(LiquidacaoItemResultado::sucesso);
  }

  @Test
  void processarLote_loteMisto_retornaResultadoParcialSemInterromperOsDemais() {
    RecebivelRequest requestSucesso = requestValido();
    RecebivelRequest requestFalha = requestValido();
    Recebivel recebivelSucesso = recebivelPendente();
    Recebivel recebivelFalha = recebivelPendente();
    Liquidacao liquidacao = liquidacaoDe(recebivelSucesso);

    when(recebivelService.criar(requestSucesso)).thenReturn(recebivelSucesso);
    when(recebivelService.criar(requestFalha)).thenReturn(recebivelFalha);
    when(liquidacaoService.liquidar(recebivelSucesso.getId(), requestSucesso.moedaPagamento()))
        .thenReturn(liquidacao);
    when(liquidacaoService.liquidar(recebivelFalha.getId(), requestFalha.moedaPagamento()))
        .thenThrow(new SaldoInsuficienteException("BRL", BigDecimal.ZERO, new BigDecimal("10.00")));

    LoteLiquidacaoResponse resposta =
        liquidacaoBatchService.processarLote(List.of(requestSucesso, requestFalha));

    assertThat(resposta.totalItens()).isEqualTo(2);
    assertThat(resposta.totalSucesso()).isEqualTo(1);
    assertThat(resposta.totalFalha()).isEqualTo(1);

    LiquidacaoItemResultado itemSucesso = resposta.itens().get(0);
    assertThat(itemSucesso.sucesso()).isTrue();
    assertThat(itemSucesso.recebivelId()).isEqualTo(recebivelSucesso.getId());

    LiquidacaoItemResultado itemFalha = resposta.itens().get(1);
    assertThat(itemFalha.sucesso()).isFalse();
    assertThat(itemFalha.codigoErro()).isEqualTo("SALDO_INSUFICIENTE");
  }

  @Test
  void processarItem_falhaAoCriarRecebivel_retornaFalhaSemRecebivelIdENuncaChamaLiquidar() {
    RecebivelRequest request = requestValido();
    when(recebivelService.criar(request))
        .thenThrow(new MoedaNaoEncontradaException(request.moedaTitulo()));

    LoteLiquidacaoResponse resposta = liquidacaoBatchService.processarLote(List.of(request));

    LiquidacaoItemResultado item = resposta.itens().get(0);
    assertThat(item.sucesso()).isFalse();
    assertThat(item.recebivelId()).isNull();
    assertThat(item.codigoErro()).isEqualTo("MOEDA_NAO_ENCONTRADA");
    verify(liquidacaoService, never()).liquidar(any(), any());
  }

  @Test
  void processarItem_falhaAoLiquidar_retornaFalhaComRecebivelIdPreenchido() {
    RecebivelRequest request = requestValido();
    Recebivel recebivel = recebivelPendente();
    when(recebivelService.criar(request)).thenReturn(recebivel);
    when(liquidacaoService.liquidar(eq(recebivel.getId()), eq(request.moedaPagamento())))
        .thenThrow(new SaldoInsuficienteException("BRL", BigDecimal.ZERO, new BigDecimal("10.00")));

    LoteLiquidacaoResponse resposta = liquidacaoBatchService.processarLote(List.of(request));

    LiquidacaoItemResultado item = resposta.itens().get(0);
    assertThat(item.sucesso()).isFalse();
    assertThat(item.recebivelId()).isEqualTo(recebivel.getId());
    assertThat(item.codigoErro()).isEqualTo("SALDO_INSUFICIENTE");
  }

  private RecebivelRequest requestValido() {
    return new RecebivelRequest(
        UUID.randomUUID(),
        "DUPLICATA_MERCANTIL",
        new BigDecimal("1000.00"),
        "BRL",
        LocalDate.of(2026, 8, 1),
        "BRL");
  }

  private Recebivel recebivelPendente() {
    return Recebivel.builder().id(UUID.randomUUID()).status(StatusRecebivel.PENDENTE).build();
  }

  private Liquidacao liquidacaoDe(Recebivel recebivel) {
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    Cedente cedente =
        Cedente.builder().id(UUID.randomUUID()).nome("Acme Ltda").documento("123").build();
    return Liquidacao.builder()
        .id(UUID.randomUUID())
        .recebivel(recebivel)
        .cedente(cedente)
        .tipo(TipoLiquidacao.LIQUIDACAO)
        .valorFace(new BigDecimal("1000.00"))
        .moedaTitulo(brl)
        .taxaBaseUsada(new BigDecimal("0.010000"))
        .spreadUsado(new BigDecimal("0.015000"))
        .prazoMesesUsado(new BigDecimal("1.0000"))
        .valorPresente(new BigDecimal("975.609756"))
        .moedaPagamento(brl)
        .valorLiquido(new BigDecimal("975.61"))
        .build();
  }
}
