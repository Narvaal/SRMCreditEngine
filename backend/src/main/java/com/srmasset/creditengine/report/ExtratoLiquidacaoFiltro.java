package com.srmasset.creditengine.report;

import java.time.Instant;
import java.util.UUID;

public record ExtratoLiquidacaoFiltro(
    UUID cedenteId,
    String moeda,
    String tipo,
    Instant dataInicio,
    Instant dataFim,
    int page,
    int size) {}
