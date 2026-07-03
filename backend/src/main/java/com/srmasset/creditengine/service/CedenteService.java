package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.dto.request.CedenteRequest;
import com.srmasset.creditengine.exception.CedenteNaoEncontradoException;
import com.srmasset.creditengine.repository.CedenteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CedenteService {

  private final CedenteRepository cedenteRepository;

  @Transactional
  public Cedente criar(CedenteRequest request) {
    Cedente cedente = Cedente.builder().nome(request.nome()).documento(request.documento()).build();
    return cedenteRepository.save(cedente);
  }

  public Cedente buscarPorId(UUID id) {
    return cedenteRepository.findById(id).orElseThrow(() -> new CedenteNaoEncontradoException(id));
  }

  public List<Cedente> listarTodos() {
    return cedenteRepository.findAll();
  }
}
