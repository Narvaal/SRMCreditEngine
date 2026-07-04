package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.repository.MoedaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoedaService {

  private final MoedaRepository moedaRepository;

  public List<Moeda> listarTodas() {
    return moedaRepository.findAll();
  }
}
