package com.srmasset.creditengine.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.dto.request.CedenteRequest;
import com.srmasset.creditengine.exception.CedenteDuplicadoException;
import com.srmasset.creditengine.exception.CedenteNaoEncontradoException;
import com.srmasset.creditengine.service.CedenteService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CedenteController.class)
class CedenteControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CedenteService cedenteService;

  @Test
  void criar_caminhoFeliz_retorna200() throws Exception {
    UUID id = UUID.randomUUID();
    Cedente cedente = Cedente.builder().id(id).nome("Acme Ltda").documento("52998224725").build();
    when(cedenteService.criar(any())).thenReturn(cedente);

    mockMvc
        .perform(
            post("/api/cedentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CedenteRequest("Acme Ltda", "52998224725"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nome").value("Acme Ltda"));
  }

  @Test
  void criar_documentoDuplicado_retorna409() throws Exception {
    when(cedenteService.criar(any())).thenThrow(new CedenteDuplicadoException("52998224725"));

    mockMvc
        .perform(
            post("/api/cedentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CedenteRequest("Acme Ltda", "52998224725"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("CEDENTE_DUPLICADO"));
  }

  @Test
  void criar_nomeEmBranco_retorna400() throws Exception {
    mockMvc
        .perform(
            post("/api/cedentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CedenteRequest("", "52998224725"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.camposInvalidos[0].campo").value("nome"));
  }

  @Test
  void criar_documentoComDigitoVerificadorErrado_retorna400ComCampoDetalhado() throws Exception {
    mockMvc
        .perform(
            post("/api/cedentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CedenteRequest("Acme Ltda", "52998224726"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.camposInvalidos[0].campo").value("documento"))
        .andExpect(
            jsonPath("$.camposInvalidos[0].mensagem").value("Informe um CPF ou CNPJ válido."));
  }

  @Test
  void criar_nomeComCaractereEspecial_retorna400() throws Exception {
    mockMvc
        .perform(
            post("/api/cedentes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CedenteRequest("Acme & Cia", "52998224725"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.camposInvalidos[0].campo").value("nome"));
  }

  @Test
  void buscarPorId_existente_retorna200() throws Exception {
    UUID id = UUID.randomUUID();
    Cedente cedente = Cedente.builder().id(id).nome("Acme Ltda").documento("52998224725").build();
    when(cedenteService.buscarPorId(id)).thenReturn(cedente);

    mockMvc.perform(get("/api/cedentes/{id}", id)).andExpect(status().isOk());
  }

  @Test
  void buscarPorId_inexistente_retorna404() throws Exception {
    UUID id = UUID.randomUUID();
    when(cedenteService.buscarPorId(id)).thenThrow(new CedenteNaoEncontradoException(id));

    mockMvc
        .perform(get("/api/cedentes/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("CEDENTE_NAO_ENCONTRADO"));
  }

  @Test
  void listar_retornaListaDoService() throws Exception {
    Cedente cedente =
        Cedente.builder().id(UUID.randomUUID()).nome("Acme Ltda").documento("52998224725").build();
    when(cedenteService.listarTodos()).thenReturn(List.of(cedente));

    mockMvc
        .perform(get("/api/cedentes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].nome").value("Acme Ltda"));
  }
}
