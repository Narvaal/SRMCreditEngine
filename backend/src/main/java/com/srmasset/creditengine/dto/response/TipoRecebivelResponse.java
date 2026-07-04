package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.domain.TipoRecebivel;

public record TipoRecebivelResponse(String codigo, String nome) {

  public static TipoRecebivelResponse de(TipoRecebivel tipoRecebivel) {
    return new TipoRecebivelResponse(tipoRecebivel.getCodigo(), tipoRecebivel.getNome());
  }
}
