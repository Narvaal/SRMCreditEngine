package com.srmasset.creditengine.service;

import com.srmasset.creditengine.dto.response.SincronizacaoTaxasResponse;
import com.srmasset.creditengine.exception.ProviderIndisponivelException;
import com.srmasset.creditengine.integration.CotacoesProviderResponse;
import com.srmasset.creditengine.integration.FxProviderClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Busca as cotações no provider (via client resiliente) e persiste reutilizando {@link
 * CambioService}/{@link TaxaMercadoService} — mesmas validações e histórico append-only do registro
 * manual.
 *
 * <p>Deliberadamente <b>não</b> é atômico entre as 4 taxas: cada cotação é uma observação
 * independente no histórico append-only (a UNIQUE por {@code vigente_em} nunca colide — o provider
 * data com o instante da consulta), e um sync parcial só significa "este indicador não atualizou
 * nesta rodada" — exatamente a degradação que o fallback já tolera. Envolver a chamada HTTP numa
 * transação de banco seria pior (conexão presa durante retry/backoff).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacaoTaxasService {

  private final FxProviderClient fxProviderClient;
  private final CambioService cambioService;
  private final TaxaMercadoService taxaMercadoService;

  public SincronizacaoTaxasResponse sincronizar() {
    CotacoesProviderResponse cotacoes;
    try {
      cotacoes = fxProviderClient.buscarCotacoes();
    } catch (RestClientException | CallNotPermittedException e) {
      // Retry esgotado ou circuito aberto — degrada só a atualização: liquidação/simulação seguem
      // com a última taxa vigente persistida.
      log.atWarn()
          .setMessage("Sincronização de taxas falhou — mantendo últimas taxas persistidas")
          .addKeyValue("causa", e.getClass().getSimpleName())
          .log();
      throw new ProviderIndisponivelException(e.getClass().getSimpleName());
    }

    cambioService.registrar("USD", "BRL", cotacoes.usdBrl(), cotacoes.vigenteEm());
    cambioService.registrar("BRL", "USD", cotacoes.brlUsd(), cotacoes.vigenteEm());
    taxaMercadoService.registrar("BRL", "CDI", cotacoes.cdi(), cotacoes.vigenteEm());
    taxaMercadoService.registrar("USD", "SOFR", cotacoes.sofr(), cotacoes.vigenteEm());

    log.atInfo()
        .setMessage("Taxas sincronizadas com o provider")
        .addKeyValue("usdBrl", cotacoes.usdBrl())
        .addKeyValue("cdi", cotacoes.cdi())
        .addKeyValue("sofr", cotacoes.sofr())
        .log();

    return SincronizacaoTaxasResponse.de(cotacoes);
  }
}
