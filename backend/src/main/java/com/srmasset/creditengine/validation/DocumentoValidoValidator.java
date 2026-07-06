package com.srmasset.creditengine.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Dígito verificador de CPF (11 dígitos) e CNPJ (14 dígitos) — algoritmo módulo 11. */
public class DocumentoValidoValidator implements ConstraintValidator<DocumentoValido, String> {

  private static final int[] PESOS_CPF_DV1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
  private static final int[] PESOS_CPF_DV2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
  private static final int[] PESOS_CNPJ_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
  private static final int[] PESOS_CNPJ_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

  @Override
  public boolean isValid(String valor, ConstraintValidatorContext context) {
    if (valor == null || valor.isBlank()) {
      return true; // nulo/em branco é papel do @NotBlank
    }
    String digitos = valor.replaceAll("[.\\-/\\s]", "");
    if (!digitos.chars().allMatch(Character::isDigit)) {
      return false;
    }
    return switch (digitos.length()) {
      case 11 -> documentoValido(digitos, PESOS_CPF_DV1, PESOS_CPF_DV2);
      case 14 -> documentoValido(digitos, PESOS_CNPJ_DV1, PESOS_CNPJ_DV2);
      default -> false;
    };
  }

  private boolean documentoValido(String digitos, int[] pesosDv1, int[] pesosDv2) {
    if (digitos.chars().distinct().count() == 1) {
      return false; // sequências repetidas (ex.: 111.111.111-11) têm DV "correto" mas são inválidas
    }
    int dv1 = digitoVerificador(digitos, pesosDv1);
    int dv2 = digitoVerificador(digitos, pesosDv2);
    return dv1 == Character.getNumericValue(digitos.charAt(pesosDv1.length))
        && dv2 == Character.getNumericValue(digitos.charAt(pesosDv2.length));
  }

  private int digitoVerificador(String digitos, int[] pesos) {
    int soma = 0;
    for (int i = 0; i < pesos.length; i++) {
      soma += Character.getNumericValue(digitos.charAt(i)) * pesos[i];
    }
    int resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  }
}
