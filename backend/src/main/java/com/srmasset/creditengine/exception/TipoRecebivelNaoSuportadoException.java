package com.srmasset.creditengine.exception;

/** Código de tipo de recebível informado no request não existe no catálogo — erro do cliente. */
public class TipoRecebivelNaoSuportadoException extends NegocioException {

  public TipoRecebivelNaoSuportadoException(String tipoRecebivelCodigo) {
    super(
        "TIPO_RECEBIVEL_NAO_SUPORTADO", "Tipo de recebível não cadastrado: " + tipoRecebivelCodigo);
  }
}
