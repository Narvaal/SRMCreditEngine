package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.domain.TaxaCambio;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.domain.TipoRecebivel;
import com.srmasset.creditengine.dto.request.SimulacaoRecebivelRequest;
import com.srmasset.creditengine.dto.response.SimulacaoRecebivelResponse;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.TipoRecebivelNaoSuportadoException;
import com.srmasset.creditengine.pricing.MotorPrecificacao;
import com.srmasset.creditengine.pricing.PrazoCalculator;
import com.srmasset.creditengine.pricing.Precisao;
import com.srmasset.creditengine.pricing.PricingStrategy;
import com.srmasset.creditengine.pricing.PricingStrategyResolver;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.TipoRecebivelRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Simulação read-only do motor de precificação — nunca persiste nada (sem {@code Recebivel}, sem
 * débito de {@code Caixa}, sem {@code Liquidacao}). Reaproveita os mesmos beans de {@link
 * LiquidacaoService}, mas deliberadamente não injeta nenhum repositório de escrita
 * (Recebivel/Caixa/Liquidacao/Cedente) — a ausência na assinatura da classe já deixa explícito que
 * não há efeito colateral.
 */
@Service
@RequiredArgsConstructor
public class SimulacaoService {

  private final TipoRecebivelRepository tipoRecebivelRepository;
  private final MoedaRepository moedaRepository;
  private final PricingStrategyResolver pricingStrategyResolver;
  private final MotorPrecificacao motorPrecificacao;
  private final PrazoCalculator prazoCalculator;
  private final TaxaMercadoService taxaMercadoService;
  private final CambioService cambioService;

  public SimulacaoRecebivelResponse simular(SimulacaoRecebivelRequest request) {
    TipoRecebivel tipoRecebivel =
        tipoRecebivelRepository
            .findById(request.tipoRecebivelCodigo())
            .filter(TipoRecebivel::getAtivo)
            .orElseThrow(
                () -> new TipoRecebivelNaoSuportadoException(request.tipoRecebivelCodigo()));

    Moeda moedaTitulo =
        moedaRepository
            .findById(request.moedaTitulo())
            .orElseThrow(() -> new MoedaNaoEncontradaException(request.moedaTitulo()));

    // Transiente — nunca persistido, só existe pra alimentar PricingStrategy.getSpread(recebivel).
    Recebivel recebivelTransiente =
        Recebivel.builder()
            .tipoRecebivel(tipoRecebivel)
            .valorFace(request.valorFace())
            .moedaTitulo(moedaTitulo)
            .dataVencimento(request.dataVencimento())
            .build();

    PricingStrategy strategy = pricingStrategyResolver.resolver(tipoRecebivel.getCodigo());
    BigDecimal spread = strategy.getSpread(recebivelTransiente);

    TaxaMercado taxaMercado = taxaMercadoService.buscarTaxaVigente(request.moedaTitulo());
    BigDecimal prazoMeses = prazoCalculator.calcularPrazoMeses(request.dataVencimento());

    BigDecimal valorPresente =
        motorPrecificacao.calcularValorPresente(
            request.valorFace(), taxaMercado.getValor(), spread, prazoMeses);

    Optional<TaxaCambio> taxaCambioOpt =
        cambioService.buscarSeNecessario(request.moedaTitulo(), request.moedaPagamento());
    BigDecimal taxaCambioUsada = taxaCambioOpt.map(TaxaCambio::getValor).orElse(null);
    BigDecimal valorLiquido =
        taxaCambioOpt
            .map(tc -> valorPresente.multiply(tc.getValor()))
            .orElse(valorPresente)
            .setScale(Precisao.ESCALA_VALOR_MONETARIO, Precisao.ARREDONDAMENTO);

    return new SimulacaoRecebivelResponse(
        request.valorFace(),
        request.moedaTitulo(),
        taxaMercado.getValor(),
        spread,
        prazoMeses,
        valorPresente,
        request.moedaPagamento(),
        taxaCambioUsada,
        valorLiquido);
  }
}
