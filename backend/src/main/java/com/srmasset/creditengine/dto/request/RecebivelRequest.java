package com.srmasset.creditengine.dto.request;

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
    @NotNull @Positive BigDecimal valorFace,
    @NotBlank String moedaTitulo,
    @NotNull @Future LocalDate dataVencimento,
    @NotBlank String moedaPagamento) {}
