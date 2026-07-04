package com.srmasset.creditengine.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record LoteRecebivelRequest(@NotEmpty @Valid List<RecebivelRequest> itens) {}
