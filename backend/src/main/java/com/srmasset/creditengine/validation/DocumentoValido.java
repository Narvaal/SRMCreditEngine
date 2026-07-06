package com.srmasset.creditengine.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que o campo é um CPF ou CNPJ com dígitos verificadores corretos (módulo 11). Aceita o
 * valor com ou sem máscara. Valores nulos/em branco são responsabilidade do @NotBlank.
 */
@Documented
@Constraint(validatedBy = DocumentoValidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentoValido {

  String message() default "Informe um CPF ou CNPJ válido.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
