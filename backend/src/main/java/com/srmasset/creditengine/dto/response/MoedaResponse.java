package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.domain.Moeda;

public record MoedaResponse(String codigo, String nome, Short casasDecimais) {

  public static MoedaResponse de(Moeda moeda) {
    return new MoedaResponse(moeda.getCodigo(), moeda.getNome(), moeda.getCasasDecimais());
  }
}
