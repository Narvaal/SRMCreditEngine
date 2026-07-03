package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.domain.Liquidacao;
import com.srmasset.creditengine.domain.Recebivel;
import java.util.UUID;

/**
 * Resultado de 1 item do lote — nunca lança exceção pro chamador, sucesso/falha é sempre explícito
 * aqui.
 */
public record LiquidacaoItemResultado(
    boolean sucesso,
    UUID recebivelId,
    LiquidacaoResponse liquidacao,
    String codigoErro,
    String mensagemErro) {

  public static LiquidacaoItemResultado sucesso(Recebivel recebivel, Liquidacao liquidacao) {
    return new LiquidacaoItemResultado(
        true, recebivel.getId(), LiquidacaoResponse.de(liquidacao), null, null);
  }

  public static LiquidacaoItemResultado falha(
      UUID recebivelId, String codigoErro, String mensagemErro) {
    return new LiquidacaoItemResultado(false, recebivelId, null, codigoErro, mensagemErro);
  }
}
