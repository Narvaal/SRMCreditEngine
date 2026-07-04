package com.srmasset.creditengine.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CedenteRequest(@NotBlank String nome, @NotBlank String documento) {}
