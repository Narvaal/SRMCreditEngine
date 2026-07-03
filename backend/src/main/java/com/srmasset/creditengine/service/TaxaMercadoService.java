package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.TaxaMercadoIndisponivelException;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.TaxaMercadoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Taxa de mercado (CDI para BRL, SOFR para USD) — taxa externa, o fundo só consome, nunca decide o
 * valor. Histórico append-only, igual câmbio.
 */
@Service
@RequiredArgsConstructor
public class TaxaMercadoService {

  private static final Map<String, String> INDICADOR_POR_MOEDA =
      Map.of("BRL", "CDI", "USD", "SOFR");

  private final TaxaMercadoRepository taxaMercadoRepository;
  private final MoedaRepository moedaRepository;

  /** Resolve o indicador de mercado da moeda (CDI/SOFR) e busca a taxa vigente mais recente. */
  public TaxaMercado buscarTaxaVigente(String moedaCodigo) {
    String indicador = INDICADOR_POR_MOEDA.get(moedaCodigo);
    if (indicador == null) {
      throw new TaxaMercadoIndisponivelException(moedaCodigo, "N/D");
    }
    return taxaMercadoRepository
        .findFirstByMoeda_CodigoAndIndicadorOrderByVigenteEmDesc(moedaCodigo, indicador)
        .orElseThrow(() -> new TaxaMercadoIndisponivelException(moedaCodigo, indicador));
  }

  @Transactional
  public TaxaMercado registrar(
      String moedaCodigo, String indicador, BigDecimal valor, Instant vigenteEm) {
    Moeda moeda =
        moedaRepository
            .findById(moedaCodigo)
            .orElseThrow(() -> new MoedaNaoEncontradaException(moedaCodigo));

    TaxaMercado taxaMercado =
        TaxaMercado.builder()
            .moeda(moeda)
            .indicador(indicador)
            .valor(valor)
            .vigenteEm(vigenteEm)
            .build();

    return taxaMercadoRepository.save(taxaMercado);
  }
}
