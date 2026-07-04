package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecebivelServiceTest {

  @Mock private RecebivelRepository recebivelRepository;
  @Mock private CedenteRepository cedenteRepository;
  @Mock private TipoRecebivelRepository tipoRecebivelRepository;
  @Mock private MoedaRepository moedaRepository;

  @InjectMocks private RecebivelService recebivelService;

  private final UUID cedenteId = UUID.randomUUID();
  private final Cedente cedente =
      Cedente.builder().id(cedenteId).nome("Acme Ltda").documento("123").build();
  private final TipoRecebivel duplicata =
      TipoRecebivel.builder()
          .codigo("DUPLICATA_MERCANTIL")
          .nome("Duplicata Mercantil")
          .ativo(true)
          .build();
  private final Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();

  private RecebivelRequest request() {
    return new RecebivelRequest(
        cedenteId,
        "DUPLICATA_MERCANTIL",
        new BigDecimal("1000.00"),
        "BRL",
        LocalDate.of(2026, 8, 1),
        "BRL");
  }

  @Test
  void criar_caminhoFeliz_statusInicialPendente() {
    RecebivelRequest request = request();
    when(cedenteRepository.findById(cedenteId)).thenReturn(Optional.of(cedente));
    when(tipoRecebivelRepository.findById("DUPLICATA_MERCANTIL"))
        .thenReturn(Optional.of(duplicata));
    when(moedaRepository.findById("BRL")).thenReturn(Optional.of(brl));
    when(recebivelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Recebivel resultado = recebivelService.criar(request);

    assertThat(resultado.getStatus()).isEqualTo(StatusRecebivel.PENDENTE);
    assertThat(resultado.getCedente()).isEqualTo(cedente);
    assertThat(resultado.getTipoRecebivel()).isEqualTo(duplicata);
    assertThat(resultado.getMoedaTitulo()).isEqualTo(brl);
    assertThat(resultado.getValorFace()).isEqualByComparingTo("1000.00");
  }

  @Test
  void criar_cedenteInexistente_lancaCedenteNaoEncontrado() {
    when(cedenteRepository.findById(cedenteId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recebivelService.criar(request()))
        .isInstanceOf(CedenteNaoEncontradoException.class);

    verify(recebivelRepository, never()).save(any());
  }

  @Test
  void criar_tipoRecebivelInexistente_lancaTipoRecebivelNaoSuportado() {
    when(cedenteRepository.findById(cedenteId)).thenReturn(Optional.of(cedente));
    when(tipoRecebivelRepository.findById("DUPLICATA_MERCANTIL")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recebivelService.criar(request()))
        .isInstanceOf(TipoRecebivelNaoSuportadoException.class);

    verify(recebivelRepository, never()).save(any());
  }

  @Test
  void criar_tipoRecebivelInativo_lancaTipoRecebivelNaoSuportado() {
    TipoRecebivel inativo =
        TipoRecebivel.builder()
            .codigo("DUPLICATA_MERCANTIL")
            .nome("Duplicata Mercantil")
            .ativo(false)
            .build();
    when(cedenteRepository.findById(cedenteId)).thenReturn(Optional.of(cedente));
    when(tipoRecebivelRepository.findById("DUPLICATA_MERCANTIL")).thenReturn(Optional.of(inativo));

    assertThatThrownBy(() -> recebivelService.criar(request()))
        .isInstanceOf(TipoRecebivelNaoSuportadoException.class);

    verify(recebivelRepository, never()).save(any());
  }

  @Test
  void criar_moedaTituloInexistente_lancaMoedaNaoEncontrada() {
    when(cedenteRepository.findById(cedenteId)).thenReturn(Optional.of(cedente));
    when(tipoRecebivelRepository.findById("DUPLICATA_MERCANTIL"))
        .thenReturn(Optional.of(duplicata));
    when(moedaRepository.findById("BRL")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> recebivelService.criar(request()))
        .isInstanceOf(MoedaNaoEncontradaException.class);

    verify(recebivelRepository, never()).save(any());
  }
}
