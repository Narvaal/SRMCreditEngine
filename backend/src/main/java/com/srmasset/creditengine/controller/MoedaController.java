package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.response.MoedaResponse;
import com.srmasset.creditengine.service.MoedaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Câmbio")
@RestController
@RequestMapping("/api/moedas")
@RequiredArgsConstructor
public class MoedaController {

  private final MoedaService moedaService;

  @GetMapping
  public ResponseEntity<List<MoedaResponse>> listar() {
    return ResponseEntity.ok(moedaService.listarTodas().stream().map(MoedaResponse::de).toList());
  }
}
