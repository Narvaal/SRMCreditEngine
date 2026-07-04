package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.TaxaMercadoIndisponivelException;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.TaxaMercadoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxaMercadoServiceTest {

  @Mock private TaxaMercadoRepository taxaMercadoRepository;
  @Mock private MoedaRepository moedaRepository;

  @InjectMocks private TaxaMercadoService taxaMercadoService;

  @Test
  void buscarTaxaVigente_brl_resolveIndicadorCdi() {
    TaxaMercado cdi =
        TaxaMercado.builder().indicador("CDI").valor(new BigDecimal("0.010000")).build();
    when(taxaMercadoRepository.findFirstByMoeda_CodigoAndIndicadorOrderByVigenteEmDesc(
            "BRL", "CDI"))
        .thenReturn(Optional.of(cdi));

    TaxaMercado resultado = taxaMercadoService.buscarTaxaVigente("BRL");

    assertThat(resultado).isSameAs(cdi);
  }

  @Test
  void buscarTaxaVigente_usd_resolveIndicadorSofr() {
    TaxaMercado sofr =
        TaxaMercado.builder().indicador("SOFR").valor(new BigDecimal("0.050000")).build();
    when(taxaMercadoRepository.findFirstByMoeda_CodigoAndIndicadorOrderByVigenteEmDesc(
            "USD", "SOFR"))
        .thenReturn(Optional.of(sofr));

    TaxaMercado resultado = taxaMercadoService.buscarTaxaVigente("USD");

    assertThat(resultado).isSameAs(sofr);
  }

  @Test
  void buscarTaxaVigente_moedaSemIndicadorMapeado_lancaComIndicadorNd() {
    assertThatThrownBy(() -> taxaMercadoService.buscarTaxaVigente("EUR"))
        .isInstanceOf(TaxaMercadoIndisponivelException.class)
        .hasMessageContaining("N/D");
  }

  @Test
  void buscarTaxaVigente_indicadorMapeadoMasSemTaxaCadastrada_lancaComIndicadorCorreto() {
    when(taxaMercadoRepository.findFirstByMoeda_CodigoAndIndicadorOrderByVigenteEmDesc(
            "BRL", "CDI"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> taxaMercadoService.buscarTaxaVigente("BRL"))
        .isInstanceOf(TaxaMercadoIndisponivelException.class)
        .hasMessageContaining("CDI");
  }

  @Test
  void registrar_caminhoFeliz_salvaComMoedaResolvida() {
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    Instant vigenteEm = Instant.parse("2026-07-01T00:00:00Z");
    when(moedaRepository.findById("BRL")).thenReturn(Optional.of(brl));
    when(taxaMercadoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TaxaMercado resultado =
        taxaMercadoService.registrar("BRL", "CDI", new BigDecimal("0.010000"), vigenteEm);

    assertThat(resultado.getMoeda()).isEqualTo(brl);
    assertThat(resultado.getIndicador()).isEqualTo("CDI");
    assertThat(resultado.getValor()).isEqualByComparingTo("0.010000");
    assertThat(resultado.getVigenteEm()).isEqualTo(vigenteEm);
  }

  @Test
  void registrar_moedaInexistente_lancaMoedaNaoEncontrada() {
    when(moedaRepository.findById("XYZ")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                taxaMercadoService.registrar(
                    "XYZ", "CDI", new BigDecimal("0.010000"), Instant.now()))
        .isInstanceOf(MoedaNaoEncontradaException.class);

    verify(taxaMercadoRepository, never()).save(any());
  }
}
