package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.request.TaxaMercadoRequest;
import com.srmasset.creditengine.dto.response.TaxaMercadoResponse;
import com.srmasset.creditengine.service.TaxaMercadoService;
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

/** Taxa de mercado (CDI/SOFR) — armazenamento e atualização manual/mockada, com histórico. */
@Tag(name = "Taxa de Mercado")
@RestController
@RequestMapping("/api/taxas-mercado")
@RequiredArgsConstructor
public class TaxaMercadoController {

  private final TaxaMercadoService taxaMercadoService;

  @PostMapping
  public ResponseEntity<TaxaMercadoResponse> registrar(
      @Valid @RequestBody TaxaMercadoRequest request) {
    var taxaMercado =
        taxaMercadoService.registrar(
            request.moedaCodigo(), request.indicador(), request.valor(), request.vigenteEm());
    return ResponseEntity.ok(TaxaMercadoResponse.de(taxaMercado));
  }

  @GetMapping
  public ResponseEntity<TaxaMercadoResponse> buscarVigente(@RequestParam String moedaCodigo) {
    return ResponseEntity.ok(
        TaxaMercadoResponse.de(taxaMercadoService.buscarTaxaVigente(moedaCodigo)));
  }
}
