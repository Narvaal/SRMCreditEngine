package com.srmasset.creditengine.exception;

import java.time.Instant;
import java.util.List;

public record ErroResponse(
    Instant timestamp,
    int status,
    String codigo,
    String mensagem,
    String path,
    List<CampoErro> camposInvalidos) {

  public record CampoErro(String campo, String mensagem) {}
}
