package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.dto.request.CedenteRequest;
import com.srmasset.creditengine.exception.CedenteDuplicadoException;
import com.srmasset.creditengine.exception.CedenteNaoEncontradoException;
import com.srmasset.creditengine.repository.CedenteRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CedenteServiceTest {

  @Mock private CedenteRepository cedenteRepository;

  @InjectMocks private CedenteService cedenteService;

  @Test
  void criar_caminhoFeliz_salvaComFlushExplicito() {
    CedenteRequest request = new CedenteRequest("Acme Ltda", "123");
    when(cedenteRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Cedente.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Cedente resultado = cedenteService.criar(request);

    assertThat(resultado.getNome()).isEqualTo("Acme Ltda");
    assertThat(resultado.getDocumento()).isEqualTo("123");
  }

  @Test
  void criar_documentoDuplicado_traduzParaCedenteDuplicado() {
    CedenteRequest request = new CedenteRequest("Acme Ltda", "123");
    when(cedenteRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Cedente.class)))
        .thenThrow(new DataIntegrityViolationException("unique constraint violado"));

    assertThatThrownBy(() -> cedenteService.criar(request))
        .isInstanceOf(CedenteDuplicadoException.class);
  }

  @Test
  void buscarPorId_existente_retornaCedente() {
    UUID id = UUID.randomUUID();
    Cedente cedente = Cedente.builder().id(id).nome("Acme Ltda").documento("123").build();
    when(cedenteRepository.findById(id)).thenReturn(Optional.of(cedente));

    assertThat(cedenteService.buscarPorId(id)).isSameAs(cedente);
  }

  @Test
  void buscarPorId_inexistente_lancaCedenteNaoEncontrado() {
    UUID id = UUID.randomUUID();
    when(cedenteRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cedenteService.buscarPorId(id))
        .isInstanceOf(CedenteNaoEncontradoException.class);
  }

  @Test
  void listarTodos_delegaParaORepositorio() {
    Cedente cedente =
        Cedente.builder().id(UUID.randomUUID()).nome("Acme Ltda").documento("123").build();
    when(cedenteRepository.findAll()).thenReturn(List.of(cedente));

    assertThat(cedenteService.listarTodos()).containsExactly(cedente);
  }
}
