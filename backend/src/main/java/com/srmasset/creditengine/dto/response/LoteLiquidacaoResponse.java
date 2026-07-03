package com.srmasset.creditengine.dto.response;

import java.util.List;

public record LoteLiquidacaoResponse(
    int totalItens, int totalSucesso, int totalFalha, List<LiquidacaoItemResultado> itens) {

  public static LoteLiquidacaoResponse de(List<LiquidacaoItemResultado> itens) {
    long sucesso = itens.stream().filter(LiquidacaoItemResultado::sucesso).count();
    return new LoteLiquidacaoResponse(
        itens.size(), (int) sucesso, itens.size() - (int) sucesso, itens);
  }
}
