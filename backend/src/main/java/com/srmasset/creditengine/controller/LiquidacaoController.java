package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.response.LiquidacaoResponse;
import com.srmasset.creditengine.service.LiquidacaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Liquidações")
@RestController
@RequestMapping("/api/liquidacoes")
@RequiredArgsConstructor
public class LiquidacaoController {

  private final LiquidacaoService liquidacaoService;

  /** Estorna uma liquidação — nunca edita a original, sempre gera uma nova linha ESTORNO. */
  @PostMapping("/{id}/estorno")
  public ResponseEntity<LiquidacaoResponse> estornar(@PathVariable UUID id) {
    return ResponseEntity.ok(LiquidacaoResponse.de(liquidacaoService.estornar(id)));
  }
}
