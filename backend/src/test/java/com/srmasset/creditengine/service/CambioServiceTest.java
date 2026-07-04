package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.TaxaCambio;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.TaxaCambioIndisponivelException;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.TaxaCambioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CambioServiceTest {

  @Mock private TaxaCambioRepository taxaCambioRepository;
  @Mock private MoedaRepository moedaRepository;

  @InjectMocks private CambioService cambioService;

  @Test
  void buscarTaxaVigente_existente_retornaAMaisRecente() {
    TaxaCambio taxaCambio = TaxaCambio.builder().valor(new BigDecimal("5.1234")).build();
    when(taxaCambioRepository
            .findFirstByMoedaOrigem_CodigoAndMoedaDestino_CodigoOrderByVigenteEmDesc("USD", "BRL"))
        .thenReturn(Optional.of(taxaCambio));

    TaxaCambio resultado = cambioService.buscarTaxaVigente("USD", "BRL");

    assertThat(resultado).isSameAs(taxaCambio);
  }

  @Test
  void buscarTaxaVigente_semTaxaCadastrada_lancaTaxaCambioIndisponivel() {
    when(taxaCambioRepository
            .findFirstByMoedaOrigem_CodigoAndMoedaDestino_CodigoOrderByVigenteEmDesc("USD", "BRL"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> cambioService.buscarTaxaVigente("USD", "BRL"))
        .isInstanceOf(TaxaCambioIndisponivelException.class);
  }

  @Test
  void buscarSeNecessario_mesmaMoeda_retornaVazioSemConsultarRepositorio() {
    Optional<TaxaCambio> resultado = cambioService.buscarSeNecessario("BRL", "BRL");

    assertThat(resultado).isEmpty();
    verifyNoInteractions(taxaCambioRepository);
  }

  @Test
  void buscarSeNecessario_moedasDiferentes_delegaParaBuscarTaxaVigente() {
    TaxaCambio taxaCambio = TaxaCambio.builder().valor(new BigDecimal("5.1234")).build();
    when(taxaCambioRepository
            .findFirstByMoedaOrigem_CodigoAndMoedaDestino_CodigoOrderByVigenteEmDesc("BRL", "USD"))
        .thenReturn(Optional.of(taxaCambio));

    Optional<TaxaCambio> resultado = cambioService.buscarSeNecessario("BRL", "USD");

    assertThat(resultado).contains(taxaCambio);
  }

  @Test
  void registrar_caminhoFeliz_salvaComMoedasResolvidas() {
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    Moeda usd = Moeda.builder().codigo("USD").nome("Dólar Americano").build();
    Instant vigenteEm = Instant.parse("2026-07-01T00:00:00Z");
    when(moedaRepository.findById("USD")).thenReturn(Optional.of(usd));
    when(moedaRepository.findById("BRL")).thenReturn(Optional.of(brl));
    when(taxaCambioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TaxaCambio resultado =
        cambioService.registrar("USD", "BRL", new BigDecimal("5.1234"), vigenteEm);

    assertThat(resultado.getMoedaOrigem()).isEqualTo(usd);
    assertThat(resultado.getMoedaDestino()).isEqualTo(brl);
    assertThat(resultado.getValor()).isEqualByComparingTo("5.1234");
    assertThat(resultado.getVigenteEm()).isEqualTo(vigenteEm);
  }

  @Test
  void registrar_moedaOrigemInexistente_lancaMoedaNaoEncontrada() {
    when(moedaRepository.findById("XYZ")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> cambioService.registrar("XYZ", "BRL", new BigDecimal("5.1234"), Instant.now()))
        .isInstanceOf(MoedaNaoEncontradaException.class);

    verify(taxaCambioRepository, never()).save(any());
  }

  @Test
  void registrar_moedaDestinoInexistente_lancaMoedaNaoEncontrada() {
    Moeda usd = Moeda.builder().codigo("USD").nome("Dólar Americano").build();
    when(moedaRepository.findById("USD")).thenReturn(Optional.of(usd));
    when(moedaRepository.findById("XYZ")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> cambioService.registrar("USD", "XYZ", new BigDecimal("5.1234"), Instant.now()))
        .isInstanceOf(MoedaNaoEncontradaException.class);

    verify(taxaCambioRepository, never()).save(any());
  }
}
