package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.dto.request.CedenteRequest;
import com.srmasset.creditengine.exception.CedenteDuplicadoException;
import com.srmasset.creditengine.exception.CedenteNaoEncontradoException;
import com.srmasset.creditengine.repository.CedenteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CedenteService {

  private final CedenteRepository cedenteRepository;

  @Transactional
  public Cedente criar(CedenteRequest request) {
    Cedente cedente = Cedente.builder().nome(request.nome()).documento(request.documento()).build();
    try {
      // saveAndFlush (não save): força a checagem da UNIQUE constraint de "documento" a acontecer
      // aqui dentro, pra poder traduzir pra exceção de domínio — mesmo padrão do flush explícito
      // usado em LiquidacaoService para Optimistic Locking.
      return cedenteRepository.saveAndFlush(cedente);
    } catch (DataIntegrityViolationException e) {
      throw new CedenteDuplicadoException(request.documento());
    }
  }

  public Cedente buscarPorId(UUID id) {
    return cedenteRepository.findById(id).orElseThrow(() -> new CedenteNaoEncontradoException(id));
  }

  public List<Cedente> listarTodos() {
    return cedenteRepository.findAll();
  }
}
