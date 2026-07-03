package com.srmasset.creditengine.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centraliza a tradução exceção de domínio → HTTP. Nota: o endpoint de lote ({@code POST
 * /api/recebiveis/lote}) captura as exceções de negócio esperadas dentro do próprio loop de
 * processamento e nunca deixa chegar aqui — este handler só entra em ação para payload malformado
 * antes de iterar, ou erro inesperado de infraestrutura.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RecebivelNaoEncontradoException.class)
  public ResponseEntity<ErroResponse> handleNaoEncontrado(
      NegocioException ex, HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, ex, req);
  }

  @ExceptionHandler({
    RecebivelJaLiquidadoException.class,
    EstornoInvalidoException.class,
    ConflitoConcorrenciaException.class
  })
  public ResponseEntity<ErroResponse> handleConflito(NegocioException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ex, req);
  }

  @ExceptionHandler({
    SaldoInsuficienteException.class,
    TaxaMercadoIndisponivelException.class,
    TaxaCambioIndisponivelException.class
  })
  public ResponseEntity<ErroResponse> handleRegraNegocio(
      NegocioException ex, HttpServletRequest req) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex, req);
  }

  @ExceptionHandler(TipoRecebivelNaoSuportadoException.class)
  public ResponseEntity<ErroResponse> handleRequestInvalido(
      NegocioException ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, ex, req);
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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErroResponse> handleInesperado(Exception ex, HttpServletRequest req) {
    String traceId = UUID.randomUUID().toString();
    log.error("Erro inesperado [traceId={}]", traceId, ex);
    ErroResponse body =
        new ErroResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "ERRO_INESPERADO",
            "Erro inesperado. traceId=" + traceId,
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
