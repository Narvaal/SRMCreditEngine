package com.srmasset.creditengine.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mesmos campos de {@link RecebivelRequest}, exceto {@code cedenteId} — nada no cálculo depende do
 * cedente.
 */
public record SimulacaoRecebivelRequest(
    @NotBlank String tipoRecebivelCodigo,
    @NotNull
        @Positive
        @DecimalMax(value = "1000000000000000.00", message = "O valor máximo é 1 quadrilhão")
        @Digits(
            integer = 16,
            fraction = 2,
            message = "O valor deve ter no máximo 16 dígitos inteiros e 2 decimais")
        BigDecimal valorFace,
    @NotBlank String moedaTitulo,
    @NotNull @Future LocalDate dataVencimento,
    @NotBlank String moedaPagamento) {}
