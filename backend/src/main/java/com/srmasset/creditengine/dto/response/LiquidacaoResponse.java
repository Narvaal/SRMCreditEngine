package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.domain.Liquidacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LiquidacaoResponse(
    UUID id,
    UUID recebivelId,
    UUID cedenteId,
    String tipo,
    UUID liquidacaoEstornadaId,
    BigDecimal valorFace,
    String moedaTitulo,
    BigDecimal taxaBaseUsada,
    BigDecimal spreadUsado,
    BigDecimal prazoMesesUsado,
    BigDecimal valorPresente,
    String moedaPagamento,
    BigDecimal taxaCambioUsada,
    BigDecimal valorLiquido,
    Instant criadoEm) {

  public static LiquidacaoResponse de(Liquidacao liquidacao) {
    return new LiquidacaoResponse(
        liquidacao.getId(),
        liquidacao.getRecebivel().getId(),
        liquidacao.getCedente().getId(),
        liquidacao.getTipo().name(),
        liquidacao.getLiquidacaoEstornada() != null
            ? liquidacao.getLiquidacaoEstornada().getId()
            : null,
        liquidacao.getValorFace(),
        liquidacao.getMoedaTitulo().getCodigo(),
        liquidacao.getTaxaBaseUsada(),
        liquidacao.getSpreadUsado(),
        liquidacao.getPrazoMesesUsado(),
        liquidacao.getValorPresente(),
        liquidacao.getMoedaPagamento().getCodigo(),
        liquidacao.getTaxaCambioUsada(),
        liquidacao.getValorLiquido(),
        liquidacao.getCriadoEm());
  }
}
