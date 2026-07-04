package com.srmasset.creditengine.controller;

import com.srmasset.creditengine.dto.response.TipoRecebivelResponse;
import com.srmasset.creditengine.service.TipoRecebivelService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recebíveis")
@RestController
@RequestMapping("/api/tipos-recebivel")
@RequiredArgsConstructor
public class TipoRecebivelController {

  private final TipoRecebivelService tipoRecebivelService;

  @GetMapping
  public ResponseEntity<List<TipoRecebivelResponse>> listar() {
    return ResponseEntity.ok(
        tipoRecebivelService.listarAtivos().stream().map(TipoRecebivelResponse::de).toList());
  }
}
