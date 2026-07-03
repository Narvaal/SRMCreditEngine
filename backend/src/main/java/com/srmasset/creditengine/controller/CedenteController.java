package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.request.CedenteRequest;
import com.srmasset.creditengine.dto.response.CedenteResponse;
import com.srmasset.creditengine.service.CedenteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cedentes")
@RestController
@RequestMapping("/api/cedentes")
@RequiredArgsConstructor
public class CedenteController {

  private final CedenteService cedenteService;

  @PostMapping
  public ResponseEntity<CedenteResponse> criar(@Valid @RequestBody CedenteRequest request) {
    return ResponseEntity.ok(CedenteResponse.de(cedenteService.criar(request)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<CedenteResponse> buscarPorId(@PathVariable UUID id) {
    return ResponseEntity.ok(CedenteResponse.de(cedenteService.buscarPorId(id)));
  }

  @GetMapping
  public ResponseEntity<List<CedenteResponse>> listar() {
    return ResponseEntity.ok(
        cedenteService.listarTodos().stream().map(CedenteResponse::de).toList());
  }
}
