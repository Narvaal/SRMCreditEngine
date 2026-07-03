package com.srmasset.creditengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record TaxaMercadoRequest(
    @NotBlank String moedaCodigo,
    @NotBlank String indicador,
    @NotNull BigDecimal valor,
    @NotNull Instant vigenteEm) {}
