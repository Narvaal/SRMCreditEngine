package com.srmasset.creditengine.dto.request;

import com.srmasset.creditengine.validation.DocumentoValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CedenteRequest(
    @NotBlank
        @Size(min = 3, max = 20, message = "Informe uma razão social de 3 a 20 caracteres.")
        @Pattern(
            regexp = "[\\p{L}\\p{N} ]+",
            message = "Use apenas letras, números e espaços na razão social.")
        String nome,
    @NotBlank @DocumentoValido String documento) {}
