package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Liquidacao;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.dto.request.RecebivelRequest;
import com.srmasset.creditengine.dto.response.LiquidacaoItemResultado;
import com.srmasset.creditengine.dto.response.LoteLiquidacaoResponse;
import com.srmasset.creditengine.exception.NegocioException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orquestra o lote — <b>não</b> é {@code @Transactional}, deliberadamente. Cada item chama {@link
 * LiquidacaoService#liquidar} através do proxy do Spring (bean diferente, injetado), nunca por
 * self-invocation — senão o {@code @Transactional} do método chamado seria silenciosamente ignorado
 * e o lote inteiro rodaria numa única transação implícita, violando a regra de "transação por item,
 * resultado parcial".
 *
 * <p>Sem retry automático em conflito de concorrência: o item falho simplesmente aparece como
 * {@code FALHA} no resultado — reenviar aquele item específico depois é seguro e trivial (o
 * recebível continua {@code PENDENTE}).
 */
@Service
@RequiredArgsConstructor
public class LiquidacaoBatchService {

  private final RecebivelService recebivelService;
  private final LiquidacaoService liquidacaoService;

  public LoteLiquidacaoResponse processarLote(List<RecebivelRequest> itens) {
    List<LiquidacaoItemResultado> resultados = itens.stream().map(this::processarItem).toList();
    return LoteLiquidacaoResponse.de(resultados);
  }

  private LiquidacaoItemResultado processarItem(RecebivelRequest request) {
    Recebivel recebivel;
    try {
      recebivel = recebivelService.criar(request);
    } catch (NegocioException e) {
      return LiquidacaoItemResultado.falha(null, e.getCodigo(), e.getMessage());
    }

    try {
      Liquidacao liquidacao =
          liquidacaoService.liquidar(recebivel.getId(), request.moedaPagamento());
      return LiquidacaoItemResultado.sucesso(recebivel, liquidacao);
    } catch (NegocioException e) {
      return LiquidacaoItemResultado.falha(recebivel.getId(), e.getCodigo(), e.getMessage());
    }
  }
}
