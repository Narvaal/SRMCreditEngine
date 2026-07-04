package com.srmasset.creditengine.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srmasset.creditengine.domain.TipoRecebivel;
import com.srmasset.creditengine.service.TipoRecebivelService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TipoRecebivelController.class)
class TipoRecebivelControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TipoRecebivelService tipoRecebivelService;

  @Test
  void listar_retornaSoOsAtivos() throws Exception {
    TipoRecebivel duplicata =
        TipoRecebivel.builder()
            .codigo("DUPLICATA_MERCANTIL")
            .nome("Duplicata Mercantil")
            .ativo(true)
            .build();
    when(tipoRecebivelService.listarAtivos()).thenReturn(List.of(duplicata));

    mockMvc
        .perform(get("/api/tipos-recebivel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].codigo").value("DUPLICATA_MERCANTIL"));
  }
}
