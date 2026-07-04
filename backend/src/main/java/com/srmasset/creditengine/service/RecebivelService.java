package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.domain.StatusRecebivel;
import com.srmasset.creditengine.domain.TipoRecebivel;
import com.srmasset.creditengine.dto.request.RecebivelRequest;
import com.srmasset.creditengine.exception.CedenteNaoEncontradoException;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.TipoRecebivelNaoSuportadoException;
import com.srmasset.creditengine.repository.CedenteRepository;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.RecebivelRepository;
import com.srmasset.creditengine.repository.TipoRecebivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecebivelService {

  private final RecebivelRepository recebivelRepository;
  private final CedenteRepository cedenteRepository;
  private final TipoRecebivelRepository tipoRecebivelRepository;
  private final MoedaRepository moedaRepository;

  @Transactional
  public Recebivel criar(RecebivelRequest request) {
    Cedente cedente =
        cedenteRepository
            .findById(request.cedenteId())
            .orElseThrow(() -> new CedenteNaoEncontradoException(request.cedenteId()));

    TipoRecebivel tipoRecebivel =
        tipoRecebivelRepository
            .findById(request.tipoRecebivelCodigo())
            .filter(TipoRecebivel::getAtivo)
            .orElseThrow(
                () -> new TipoRecebivelNaoSuportadoException(request.tipoRecebivelCodigo()));

    Moeda moedaTitulo =
        moedaRepository
            .findById(request.moedaTitulo())
            .orElseThrow(() -> new MoedaNaoEncontradaException(request.moedaTitulo()));

    Recebivel recebivel =
        Recebivel.builder()
            .cedente(cedente)
            .tipoRecebivel(tipoRecebivel)
            .valorFace(request.valorFace())
            .moedaTitulo(moedaTitulo)
            .dataVencimento(request.dataVencimento())
            .status(StatusRecebivel.PENDENTE)
            .build();

    return recebivelRepository.save(recebivel);
  }
}
