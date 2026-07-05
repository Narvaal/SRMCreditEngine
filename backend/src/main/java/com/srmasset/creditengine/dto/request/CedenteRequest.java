package com.srmasset.creditengine.dto.request;

import com.srmasset.creditengine.validation.DocumentoValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CedenteRequest(
    @NotBlank
        @Size(min = 3, max = 20, message = "A razão social deve ter entre 3 e 20 caracteres")
        @Pattern(
            regexp = "[\\p{L}\\p{N} ]+",
            message = "A razão social não pode conter caracteres especiais")
        String nome,
    @NotBlank @DocumentoValido String documento) {}
