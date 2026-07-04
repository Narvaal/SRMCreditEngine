package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.dto.response.SincronizacaoTaxasResponse;
import com.srmasset.creditengine.exception.ProviderIndisponivelException;
import com.srmasset.creditengine.integration.CotacoesProviderResponse;
import com.srmasset.creditengine.integration.FxProviderClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class SincronizacaoTaxasServiceTest {

  @Mock private FxProviderClient fxProviderClient;
  @Mock private CambioService cambioService;
  @Mock private TaxaMercadoService taxaMercadoService;

  @InjectMocks private SincronizacaoTaxasService sincronizacaoTaxasService;

  private static final Instant VIGENTE_EM = Instant.parse("2026-07-04T12:00:00Z");

  private CotacoesProviderResponse cotacoes() {
    return new CotacoesProviderResponse(
        new BigDecimal("5.40000000"),
        new BigDecimal("0.18518519"),
        new BigDecimal("0.010000"),
        new BigDecimal("0.004500"),
        VIGENTE_EM);
  }

  @Test
  void sincronizar_caminhoFeliz_persisteAsQuatroTaxasComOMesmoVigenteEm() {
    when(fxProviderClient.buscarCotacoes()).thenReturn(cotacoes());

    SincronizacaoTaxasResponse resposta = sincronizacaoTaxasService.sincronizar();

    verify(cambioService).registrar("USD", "BRL", new BigDecimal("5.40000000"), VIGENTE_EM);
    verify(cambioService).registrar("BRL", "USD", new BigDecimal("0.18518519"), VIGENTE_EM);
    verify(taxaMercadoService).registrar("BRL", "CDI", new BigDecimal("0.010000"), VIGENTE_EM);
    verify(taxaMercadoService).registrar("USD", "SOFR", new BigDecimal("0.004500"), VIGENTE_EM);
    assertThat(resposta.usdBrl()).isEqualByComparingTo("5.40");
    assertThat(resposta.vigenteEm()).isEqualTo(VIGENTE_EM);
  }

  @Test
  void sincronizar_falhaHttpDoProvider_traduzParaProviderIndisponivelSemPersistirNada() {
    when(fxProviderClient.buscarCotacoes()).thenThrow(new RestClientException("retry esgotado"));

    assertThatThrownBy(() -> sincronizacaoTaxasService.sincronizar())
        .isInstanceOf(ProviderIndisponivelException.class);

    verifyNoInteractions(cambioService, taxaMercadoService);
  }

  @Test
  void sincronizar_circuitoAberto_traduzParaProviderIndisponivelSemPersistirNada() {
    when(fxProviderClient.buscarCotacoes())
        .thenThrow(
            CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.ofDefaults("fxProvider")));

    assertThatThrownBy(() -> sincronizacaoTaxasService.sincronizar())
        .isInstanceOf(ProviderIndisponivelException.class);

    verifyNoInteractions(cambioService, taxaMercadoService);
  }
}
