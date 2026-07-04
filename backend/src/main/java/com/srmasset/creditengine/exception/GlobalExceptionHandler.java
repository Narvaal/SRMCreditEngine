package com.srmasset.creditengine.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Centraliza a tradução exceção de domínio → HTTP. Nota: o endpoint de lote ({@code POST
 * /api/recebiveis/lote}) captura as exceções de negócio esperadas dentro do próprio loop de
 * processamento e nunca deixa chegar aqui — este handler só entra em ação para payload malformado
 * antes de iterar, ou erro inesperado de infraestrutura.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({
    RecebivelNaoEncontradoException.class,
    CedenteNaoEncontradoException.class,
    MoedaNaoEncontradaException.class
  })
  public ResponseEntity<ErroResponse> handleNaoEncontrado(
      NegocioException ex, HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, ex, req);
  }

  @ExceptionHandler({
    RecebivelJaLiquidadoException.class,
    EstornoInvalidoException.class,
    ConflitoConcorrenciaException.class,
    CedenteDuplicadoException.class
  })
  public ResponseEntity<ErroResponse> handleConflito(NegocioException ex, HttpServletRequest req) {
    log.warn("[{}] {}", ex.getCodigo(), ex.getMessage());
    return build(HttpStatus.CONFLICT, ex, req);
  }

  @ExceptionHandler({
    SaldoInsuficienteException.class,
    TaxaMercadoIndisponivelException.class,
    TaxaCambioIndisponivelException.class
  })
  public ResponseEntity<ErroResponse> handleRegraNegocio(
      NegocioException ex, HttpServletRequest req) {
    log.warn("[{}] {}", ex.getCodigo(), ex.getMessage());
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex, req);
  }

  @ExceptionHandler(TipoRecebivelNaoSuportadoException.class)
  public ResponseEntity<ErroResponse> handleRequestInvalido(
      NegocioException ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, ex, req);
  }

  @ExceptionHandler(ProviderIndisponivelException.class)
  public ResponseEntity<ErroResponse> handleProviderIndisponivel(
      NegocioException ex, HttpServletRequest req) {
    log.warn("[{}] {}", ex.getCodigo(), ex.getMessage());
    return build(HttpStatus.SERVICE_UNAVAILABLE, ex, req);
  }

  @ExceptionHandler(PricingStrategyNaoEncontradaException.class)
  public ResponseEntity<ErroResponse> handleConfiguracaoInconsistente(
      PricingStrategyNaoEncontradaException ex, HttpServletRequest req) {
    log.error("Bug de configuração: {}", ex.getMessage(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, req);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErroResponse> handleValidacao(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<ErroResponse.CampoErro> campos =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ErroResponse.CampoErro(fe.getField(), fe.getDefaultMessage()))
            .toList();
    ErroResponse body =
        new ErroResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "REQUEST_INVALIDO",
            "Payload inválido",
            req.getRequestURI(),
            campos);
    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Query param obrigatório ausente (ex.: {@code GET /api/taxas-cambio} sem {@code moedaOrigem}) ou
   * com tipo incompatível (ex.: {@code UUID}/{@code Instant} malformado). Sem este handler
   * dedicado, {@link #handleInesperado} (catch-all de {@code Exception}) intercepta essas exceções
   * do próprio Spring MVC antes da resolução padrão do framework e devolve 500 em vez de 400.
   */
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErroResponse> handleParametroInvalido(
      Exception ex, HttpServletRequest req) {
    ErroResponse body =
        new ErroResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "REQUEST_INVALIDO",
            ex.getMessage(),
            req.getRequestURI(),
            List.of());
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErroResponse> handleInesperado(Exception ex, HttpServletRequest req) {
    // requestId vem do CorrelationIdFilter (MDC) — a mesma correlação de todas as outras linhas
    // de log desta requisição, em vez de um id solto gerado só aqui.
    String requestId = MDC.get("requestId");
    log.error("Erro inesperado [requestId={}]", requestId, ex);
    ErroResponse body =
        new ErroResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "ERRO_INESPERADO",
            "Erro inesperado. requestId=" + requestId,
            req.getRequestURI(),
            List.of());
    return ResponseEntity.internalServerError().body(body);
  }

  private ResponseEntity<ErroResponse> build(
      HttpStatus status, NegocioException ex, HttpServletRequest req) {
    ErroResponse body =
        new ErroResponse(
            Instant.now(),
            status.value(),
            ex.getCodigo(),
            ex.getMessage(),
            req.getRequestURI(),
            List.of());
    return ResponseEntity.status(status).body(body);
  }
}
