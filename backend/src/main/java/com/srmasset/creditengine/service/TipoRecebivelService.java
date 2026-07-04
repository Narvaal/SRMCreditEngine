package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.TipoRecebivel;
import com.srmasset.creditengine.repository.TipoRecebivelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TipoRecebivelService {

  private final TipoRecebivelRepository tipoRecebivelRepository;

  public List<TipoRecebivel> listarAtivos() {
    return tipoRecebivelRepository.findAll().stream().filter(TipoRecebivel::getAtivo).toList();
  }
}
