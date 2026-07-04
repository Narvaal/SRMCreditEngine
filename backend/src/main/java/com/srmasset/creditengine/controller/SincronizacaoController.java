package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.response.SincronizacaoTaxasResponse;
import com.srmasset.creditengine.service.SincronizacaoTaxasService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sincroniza câmbio + taxas de mercado com o provider externo (mockado) sob demanda. Retorna 503
 * {@code PROVIDER_INDISPONIVEL} quando o provider está fora (retry esgotado/circuito aberto) — e
 * nesse caso o sistema segue liquidando com a última taxa vigente persistida.
 */
@Tag(name = "Sincronização de Taxas")
@RestController
@RequestMapping("/api/taxas")
@RequiredArgsConstructor
public class SincronizacaoController {

  private final SincronizacaoTaxasService sincronizacaoTaxasService;

  @PostMapping("/sincronizar")
  public ResponseEntity<SincronizacaoTaxasResponse> sincronizar() {
    return ResponseEntity.ok(sincronizacaoTaxasService.sincronizar());
  }
}
