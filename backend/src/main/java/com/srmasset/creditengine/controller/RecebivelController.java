package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.request.LoteRecebivelRequest;
import com.srmasset.creditengine.dto.request.SimulacaoRecebivelRequest;
import com.srmasset.creditengine.dto.response.LoteLiquidacaoResponse;
import com.srmasset.creditengine.dto.response.SimulacaoRecebivelResponse;
import com.srmasset.creditengine.service.LiquidacaoBatchService;
import com.srmasset.creditengine.service.SimulacaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recebíveis")
@RestController
@RequestMapping("/api/recebiveis")
@RequiredArgsConstructor
public class RecebivelController {

  private final LiquidacaoBatchService liquidacaoBatchService;
  private final SimulacaoService simulacaoService;

  @PostMapping("/lote")
  public ResponseEntity<LoteLiquidacaoResponse> processarLote(
      @Valid @RequestBody LoteRecebivelRequest request) {
    return ResponseEntity.ok(liquidacaoBatchService.processarLote(request.itens()));
  }

  /**
   * Read-only: calcula o valor líquido sem persistir nada — sem criar recebível, sem debitar caixa.
   */
  @PostMapping("/simular")
  public ResponseEntity<SimulacaoRecebivelResponse> simular(
      @Valid @RequestBody SimulacaoRecebivelRequest request) {
    return ResponseEntity.ok(simulacaoService.simular(request));
  }
}
