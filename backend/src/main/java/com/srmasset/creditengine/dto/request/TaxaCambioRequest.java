package com.srmasset.creditengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record TaxaCambioRequest(
    @NotBlank String moedaOrigem,
    @NotBlank String moedaDestino,
    @NotNull @Positive BigDecimal valor,
    @NotNull Instant vigenteEm) {}
