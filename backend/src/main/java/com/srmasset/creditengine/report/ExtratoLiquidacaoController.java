package com.srmasset.creditengine.report;

import com.srmasset.creditengine.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Caminho de 2 camadas (sem Service) — ver {@link ExtratoLiquidacaoRepository}. */
@Tag(name = "Relatórios")
@RestController
@RequestMapping("/api/relatorios/extrato-liquidacao")
@RequiredArgsConstructor
public class ExtratoLiquidacaoController {

  private final ExtratoLiquidacaoRepository extratoLiquidacaoRepository;

  @GetMapping
  public ResponseEntity<PaginaResponse<ExtratoLiquidacaoLinha>> buscar(
      @RequestParam(required = false) UUID cedenteId,
      @RequestParam(required = false) String moeda,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) Instant dataInicio,
      @RequestParam(required = false) Instant dataFim,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var filtro =
        new ExtratoLiquidacaoFiltro(cedenteId, moeda, tipo, dataInicio, dataFim, page, size);
    return ResponseEntity.ok(extratoLiquidacaoRepository.buscar(filtro));
  }
}
