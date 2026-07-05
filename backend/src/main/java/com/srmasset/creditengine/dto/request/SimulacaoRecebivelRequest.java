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
        @DecimalMax(
            value = "1000000000000000.00",
            message = "Informe um valor de até 1 quadrilhão.")
        @Digits(integer = 16, fraction = 2, message = "Informe um valor com até 2 casas decimais.")
        BigDecimal valorFace,
    @NotBlank String moedaTitulo,
    @NotNull @Future LocalDate dataVencimento,
    @NotBlank String moedaPagamento) {}
