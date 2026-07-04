package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.TaxaCambio;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.TaxaCambioIndisponivelException;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.TaxaCambioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "Currency Engine": fonte de verdade de taxas de câmbio, histórico append-only. */
@Service
@RequiredArgsConstructor
public class CambioService {

  private final TaxaCambioRepository taxaCambioRepository;
  private final MoedaRepository moedaRepository;

  /** Busca a taxa vigente mais recente para o par — lança se não houver nenhuma cadastrada. */
  public TaxaCambio buscarTaxaVigente(String moedaOrigemCodigo, String moedaDestinoCodigo) {
    return taxaCambioRepository
        .findFirstByMoedaOrigem_CodigoAndMoedaDestino_CodigoOrderByVigenteEmDesc(
            moedaOrigemCodigo, moedaDestinoCodigo)
        .orElseThrow(
            () -> new TaxaCambioIndisponivelException(moedaOrigemCodigo, moedaDestinoCodigo));
  }

  /**
   * Mesma moeda de origem e destino => sem conversão (vazio). Moedas diferentes => busca a taxa
   * vigente (lança se não houver). Centraliza a regra "converte só se as moedas diferirem",
   * reaproveitada por LiquidacaoService e SimulacaoService.
   */
  public Optional<TaxaCambio> buscarSeNecessario(
      String moedaOrigemCodigo, String moedaDestinoCodigo) {
    if (moedaOrigemCodigo.equals(moedaDestinoCodigo)) {
      return Optional.empty();
    }
    return Optional.of(buscarTaxaVigente(moedaOrigemCodigo, moedaDestinoCodigo));
  }

  @Transactional
  public TaxaCambio registrar(
      String moedaOrigemCodigo, String moedaDestinoCodigo, BigDecimal valor, Instant vigenteEm) {
    Moeda moedaOrigem = buscarMoeda(moedaOrigemCodigo);
    Moeda moedaDestino = buscarMoeda(moedaDestinoCodigo);

    TaxaCambio taxaCambio =
        TaxaCambio.builder()
            .moedaOrigem(moedaOrigem)
            .moedaDestino(moedaDestino)
            .valor(valor)
            .vigenteEm(vigenteEm)
            .build();

    return taxaCambioRepository.save(taxaCambio);
  }

  private Moeda buscarMoeda(String codigo) {
    return moedaRepository
        .findById(codigo)
        .orElseThrow(() -> new MoedaNaoEncontradaException(codigo));
  }
}
