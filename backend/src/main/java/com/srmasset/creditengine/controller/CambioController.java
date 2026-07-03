package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.request.TaxaCambioRequest;
import com.srmasset.creditengine.dto.response.TaxaCambioResponse;
import com.srmasset.creditengine.service.CambioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** "Currency Engine" — armazena e provê taxas de câmbio (atualização manual/mockada). */
@Tag(name = "Câmbio")
@RestController
@RequestMapping("/api/taxas-cambio")
@RequiredArgsConstructor
public class CambioController {

  private final CambioService cambioService;

  @PostMapping
  public ResponseEntity<TaxaCambioResponse> registrar(
      @Valid @RequestBody TaxaCambioRequest request) {
    var taxaCambio =
        cambioService.registrar(
            request.moedaOrigem(), request.moedaDestino(), request.valor(), request.vigenteEm());
    return ResponseEntity.ok(TaxaCambioResponse.de(taxaCambio));
  }

  @GetMapping
  public ResponseEntity<TaxaCambioResponse> buscarVigente(
      @RequestParam String moedaOrigem, @RequestParam String moedaDestino) {
    return ResponseEntity.ok(
        TaxaCambioResponse.de(cambioService.buscarTaxaVigente(moedaOrigem, moedaDestino)));
  }
}
