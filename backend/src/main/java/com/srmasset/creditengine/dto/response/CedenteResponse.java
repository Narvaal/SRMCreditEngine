package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.domain.Cedente;
import java.util.UUID;

public record CedenteResponse(UUID id, String nome, String documento) {

  public static CedenteResponse de(Cedente cedente) {
    return new CedenteResponse(cedente.getId(), cedente.getNome(), cedente.getDocumento());
  }
}
