package com.srmasset.creditengine.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Trava o mapeamento exceção de domínio -> status HTTP, o contrato central do handler. Fácil de
 * regredir silenciosamente (nova exceção sem handler cai no 500 genérico sem avisar ninguém).
 */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
  private final HttpServletRequest request = mock(HttpServletRequest.class);

  {
    when(request.getRequestURI()).thenReturn("/api/recebiveis/123");
  }

  @Test
  void recebivelNaoEncontrado_retorna404ComCodigoDeDominio() {
    ResponseEntity<ErroResponse> resposta =
        handler.handleNaoEncontrado(
            new RecebivelNaoEncontradoException(UUID.randomUUID()), request);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(resposta.getBody().codigo()).isEqualTo("RECEBIVEL_NAO_ENCONTRADO");
    assertThat(resposta.getBody().path()).isEqualTo("/api/recebiveis/123");
  }

  @Test
  void conflitoConcorrencia_retorna409ComCodigoDeDominio() {
    ResponseEntity<ErroResponse> resposta =
        handler.handleConflito(new ConflitoConcorrenciaException("caixa BRL"), request);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(resposta.getBody().codigo()).isEqualTo("CONFLITO_CONCORRENCIA");
  }

  @Test
  void cedenteDuplicado_retorna409ComCodigoDeDominio() {
    ResponseEntity<ErroResponse> resposta =
        handler.handleConflito(new CedenteDuplicadoException("123"), request);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(resposta.getBody().codigo()).isEqualTo("CEDENTE_DUPLICADO");
  }

  @Test
  void saldoInsuficiente_retorna422ComCodigoDeDominio() {
    ResponseEntity<ErroResponse> resposta =
        handler.handleRegraNegocio(
            new SaldoInsuficienteException(
                "BRL", java.math.BigDecimal.ZERO, java.math.BigDecimal.TEN),
            request);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(resposta.getBody().codigo()).isEqualTo("SALDO_INSUFICIENTE");
  }

  @Test
  void tipoRecebivelNaoSuportado_retorna400ComCodigoDeDominio() {
    ResponseEntity<ErroResponse> resposta =
        handler.handleRequestInvalido(
            new TipoRecebivelNaoSuportadoException("INEXISTENTE"), request);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(resposta.getBody().codigo()).isEqualTo("TIPO_RECEBIVEL_NAO_SUPORTADO");
  }

  @Test
  void pricingStrategyNaoEncontrada_retorna500ComoBugDeConfiguracao() {
    ResponseEntity<ErroResponse> resposta =
        handler.handleConfiguracaoInconsistente(
            new PricingStrategyNaoEncontradaException("NOVO_TIPO"), request);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(resposta.getBody().codigo()).isEqualTo("PRICING_STRATEGY_NAO_ENCONTRADA");
  }

  @Test
  void validacaoInvalida_retorna400ComListaDeCamposInvalidos() {
    BindingResult bindingResult = mock(BindingResult.class);
    when(bindingResult.getFieldErrors())
        .thenReturn(List.of(new FieldError("recebivelRequest", "valorFace", "deve ser positivo")));
    MethodArgumentNotValidException excecao =
        new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

    ResponseEntity<ErroResponse> resposta = handler.handleValidacao(excecao, request);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(resposta.getBody().codigo()).isEqualTo("REQUEST_INVALIDO");
    assertThat(resposta.getBody().camposInvalidos()).hasSize(1);
    assertThat(resposta.getBody().camposInvalidos().get(0).campo()).isEqualTo("valorFace");
    assertThat(resposta.getBody().camposInvalidos().get(0).mensagem())
        .isEqualTo("deve ser positivo");
  }

  @Test
  void erroInesperado_retorna500ComTraceIdNaMensagem() {
    ResponseEntity<ErroResponse> resposta =
        handler.handleInesperado(new RuntimeException("boom"), request);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(resposta.getBody().codigo()).isEqualTo("ERRO_INESPERADO");
    assertThat(resposta.getBody().mensagem()).contains("traceId=");
    assertThat(resposta.getBody().camposInvalidos()).isEmpty();
  }
}
