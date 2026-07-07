package com.srmasset.creditengine.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record LoteRecebivelRequest(
    // Teto por requisição: o lote é processado de forma síncrona (uma transação por item), então o
    // tamanho precisa ser limitado na borda pra requisição ter latência previsível. Volumes maiores
    // pedem processamento assíncrono (ver docs/criterios-aceite.md, E6).
    @NotEmpty @Size(max = 100, message = "Envie no máximo 100 itens por lote.") @Valid
        List<RecebivelRequest> itens) {}
