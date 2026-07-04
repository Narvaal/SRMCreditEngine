package com.srmasset.creditengine.exception;

/**
 * Base de todas as exceções de domínio. Deliberadamente sem qualquer anotação Spring/HTTP (sem
 * {@code @ResponseStatus}) — a tradução exceção→HTTP fica centralizada só no {@link
 * GlobalExceptionHandler}, mantendo o domínio testável isoladamente, sem contexto Spring.
 */
public abstract class NegocioException extends RuntimeException {

  private final String codigo;

  protected NegocioException(String codigo, String mensagem) {
    super(mensagem);
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }
}
