package com.srmasset.creditengine.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecebivelRequest(
    @NotNull UUID cedenteId,
    @NotBlank String tipoRecebivelCodigo,
    // Teto de negócio (1 quadrilhão) + limite físico do NUMERIC(18,2) — evita overflow no banco
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
