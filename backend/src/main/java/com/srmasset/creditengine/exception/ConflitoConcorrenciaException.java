package com.srmasset.creditengine.exception;

/**
 * Tradução de {@link org.springframework.orm.ObjectOptimisticLockingFailureException} — demonstra a
 * proteção de Optimistic Locking contra duas liquidações concorrentes disputando o mesmo {@link
 * com.srmasset.creditengine.domain.Recebivel}/{@link com.srmasset.creditengine.domain.Caixa}.
 * Deliberadamente sem retry automático: vira item de falha no resultado parcial do lote, nunca é
 * escondida ou reprocessada silenciosamente.
 */
public class ConflitoConcorrenciaException extends NegocioException {

  public ConflitoConcorrenciaException(String contexto) {
    super("CONFLITO_CONCORRENCIA", "Conflito de concorrência: " + contexto);
  }
}
