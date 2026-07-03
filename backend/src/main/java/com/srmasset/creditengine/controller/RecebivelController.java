package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.request.LoteRecebivelRequest;
import com.srmasset.creditengine.dto.response.LoteLiquidacaoResponse;
import com.srmasset.creditengine.service.LiquidacaoBatchService;
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

  /**
   * Recebe um lote de recebíveis, cria e liquida cada um numa transação própria. Sempre 200 OK — o
   * resultado é parcial (por item), nunca tudo-ou-nada pra requisição inteira.
   */
  @PostMapping("/lote")
  public ResponseEntity<LoteLiquidacaoResponse> processarLote(
      @Valid @RequestBody LoteRecebivelRequest request) {
    return ResponseEntity.ok(liquidacaoBatchService.processarLote(request.itens()));
  }
}
