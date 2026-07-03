package com.srmasset.creditengine.dto.request;

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
    @NotNull @Positive BigDecimal valorFace,
    @NotBlank String moedaTitulo,
    @NotNull @Future LocalDate dataVencimento,
    @NotBlank String moedaPagamento) {}
