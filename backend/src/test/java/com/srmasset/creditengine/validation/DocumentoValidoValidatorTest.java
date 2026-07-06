package com.srmasset.creditengine.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoValidoValidatorTest {

  private final DocumentoValidoValidator validator = new DocumentoValidoValidator();

  @ParameterizedTest
  @ValueSource(strings = {"52998224725", "529.982.247-25", "11444777000161", "11.444.777/0001-61"})
  void documentosComDigitoVerificadorCorreto_saoValidos(String documento) {
    assertThat(validator.isValid(documento, null)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"52998224726", "529.982.247-26", "11444777000162", "11.444.777/0001-62"})
  void digitoVerificadorErrado_eInvalido(String documento) {
    assertThat(validator.isValid(documento, null)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"11111111111", "00000000000", "11111111111111"})
  void sequenciasRepetidas_saoInvalidas(String documento) {
    assertThat(validator.isValid(documento, null)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "123", "529982247", "5299822472512345", "5299822472a"})
  void tamanhoErradoOuNaoNumerico_eInvalido(String documento) {
    assertThat(validator.isValid(documento, null)).isFalse();
  }

  @Test
  void nuloOuEmBranco_naoEResponsabilidadeDesteValidador() {
    assertThat(validator.isValid(null, null)).isTrue();
    assertThat(validator.isValid("  ", null)).isTrue();
  }
}
