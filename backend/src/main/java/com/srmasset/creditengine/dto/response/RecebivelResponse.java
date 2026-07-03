package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.domain.Recebivel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecebivelResponse(
    UUID id,
    UUID cedenteId,
    String tipoRecebivelCodigo,
    BigDecimal valorFace,
    String moedaTitulo,
    LocalDate dataVencimento,
    String status) {

  public static RecebivelResponse de(Recebivel recebivel) {
    return new RecebivelResponse(
        recebivel.getId(),
        recebivel.getCedente().getId(),
        recebivel.getTipoRecebivel().getCodigo(),
        recebivel.getValorFace(),
        recebivel.getMoedaTitulo().getCodigo(),
        recebivel.getDataVencimento(),
        recebivel.getStatus().name());
  }
}
