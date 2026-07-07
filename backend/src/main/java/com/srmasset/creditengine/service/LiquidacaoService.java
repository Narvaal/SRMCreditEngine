package com.srmasset.creditengine.service;

import com.srmasset.creditengine.domain.Caixa;
import com.srmasset.creditengine.domain.Liquidacao;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.domain.StatusRecebivel;
import com.srmasset.creditengine.domain.TaxaCambio;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.domain.TipoLiquidacao;
import com.srmasset.creditengine.exception.ConflitoConcorrenciaException;
import com.srmasset.creditengine.exception.EstornoInvalidoException;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.RecebivelJaLiquidadoException;
import com.srmasset.creditengine.exception.RecebivelNaoEncontradoException;
import com.srmasset.creditengine.exception.SaldoInsuficienteException;
import com.srmasset.creditengine.metrics.MetricasNegocio;
import com.srmasset.creditengine.pricing.MotorPrecificacao;
import com.srmasset.creditengine.pricing.PrazoCalculator;
import com.srmasset.creditengine.pricing.Precisao;
import com.srmasset.creditengine.pricing.PricingStrategy;
import com.srmasset.creditengine.pricing.PricingStrategyResolver;
import com.srmasset.creditengine.repository.CaixaRepository;
import com.srmasset.creditengine.repository.LiquidacaoRepository;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.RecebivelRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transação por recebível — nunca por lote inteiro (ver {@link LiquidacaoBatchService}). Cada
 * método público aqui é uma unidade atômica: ou tudo (débito de caixa, mudança de status, inserção
 * no ledger) acontece, ou nada acontece.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidacaoService {

  private final RecebivelRepository recebivelRepository;
  private final CaixaRepository caixaRepository;
  private final LiquidacaoRepository liquidacaoRepository;
  private final MoedaRepository moedaRepository;
  private final PricingStrategyResolver pricingStrategyResolver;
  private final MotorPrecificacao motorPrecificacao;
  private final PrazoCalculator prazoCalculator;
  private final TaxaMercadoService taxaMercadoService;
  private final CambioService cambioService;
  private final MetricasNegocio metricasNegocio;

  @Transactional
  public Liquidacao liquidar(UUID recebivelId, String moedaPagamentoCodigo) {
    Recebivel recebivel =
        recebivelRepository
            .findById(recebivelId)
            .orElseThrow(() -> new RecebivelNaoEncontradoException(recebivelId));

    if (recebivel.getStatus() != StatusRecebivel.PENDENTE) {
      throw new RecebivelJaLiquidadoException(recebivelId, recebivel.getStatus());
    }

    String moedaTituloCodigo = recebivel.getMoedaTitulo().getCodigo();

    PricingStrategy strategy =
        pricingStrategyResolver.resolver(recebivel.getTipoRecebivel().getCodigo());
    BigDecimal spread = strategy.getSpread(recebivel);

    TaxaMercado taxaMercado = taxaMercadoService.buscarTaxaVigente(moedaTituloCodigo);
    BigDecimal prazoMeses = prazoCalculator.calcularPrazoMeses(recebivel.getDataVencimento());

    BigDecimal valorPresente =
        motorPrecificacao.calcularValorPresente(
            recebivel.getValorFace(), taxaMercado.getValor(), spread, prazoMeses);

    // Conversão acontece DEPOIS do deságio — nunca antes (ver ROADMAP.md).
    Optional<TaxaCambio> taxaCambioOpt =
        cambioService.buscarSeNecessario(moedaTituloCodigo, moedaPagamentoCodigo);
    TaxaCambio taxaCambio = taxaCambioOpt.orElse(null);
    BigDecimal taxaCambioUsada = taxaCambioOpt.map(TaxaCambio::getValor).orElse(null);
    BigDecimal valorLiquido =
        taxaCambioOpt
            .map(tc -> valorPresente.multiply(tc.getValor()))
            .orElse(valorPresente)
            .setScale(Precisao.ESCALA_VALOR_MONETARIO, Precisao.ARREDONDAMENTO);

    Caixa caixa =
        caixaRepository
            .findById(moedaPagamentoCodigo)
            .orElseThrow(
                () ->
                    new SaldoInsuficienteException(
                        moedaPagamentoCodigo, BigDecimal.ZERO, valorLiquido));
    if (caixa.getSaldo().compareTo(valorLiquido) < 0) {
      throw new SaldoInsuficienteException(moedaPagamentoCodigo, caixa.getSaldo(), valorLiquido);
    }

    caixa.setSaldo(caixa.getSaldo().subtract(valorLiquido));
    recebivel.setStatus(StatusRecebivel.LIQUIDADO);

    // Flush explícito: força o UPDATE (e a checagem de version) a acontecer AQUI DENTRO do método,
    // pra que o conflito de Optimistic Locking seja capturado e traduzido pra exceção de domínio,
    // em vez de vazar um tipo Spring/Hibernate só no commit da transação, fora do nosso controle.
    try {
      caixaRepository.flush();
    } catch (ObjectOptimisticLockingFailureException e) {
      throw new ConflitoConcorrenciaException(
          "recebível %s / caixa %s".formatted(recebivelId, moedaPagamentoCodigo));
    }

    Moeda moedaPagamento = buscarMoeda(moedaPagamentoCodigo);

    Liquidacao liquidacao =
        Liquidacao.builder()
            .recebivel(recebivel)
            .cedente(recebivel.getCedente())
            .tipo(TipoLiquidacao.LIQUIDACAO)
            .valorFace(recebivel.getValorFace())
            .moedaTitulo(recebivel.getMoedaTitulo())
            .taxaBaseUsada(taxaMercado.getValor())
            .taxaBaseRef(taxaMercado)
            .spreadUsado(spread)
            .prazoMesesUsado(prazoMeses)
            .valorPresente(valorPresente)
            .moedaPagamento(moedaPagamento)
            .taxaCambioUsada(taxaCambioUsada)
            .taxaCambioRef(taxaCambio)
            .valorLiquido(valorLiquido)
            .build();

    Liquidacao salva = liquidacaoRepository.save(liquidacao);
    metricasNegocio.registrarLiquidacao(moedaPagamentoCodigo, valorLiquido);
    log.atInfo()
        .setMessage("Liquidação concluída")
        .addKeyValue("recebivelId", recebivelId)
        .addKeyValue("moedaPagamento", moedaPagamentoCodigo)
        .addKeyValue("valorLiquido", valorLiquido)
        .log();
    return salva;
  }

  @Transactional
  public Liquidacao estornar(UUID liquidacaoOriginalId) {
    Liquidacao original =
        liquidacaoRepository
            .findById(liquidacaoOriginalId)
            .orElseThrow(
                () ->
                    new EstornoInvalidoException(
                        liquidacaoOriginalId, "liquidação não encontrada"));

    if (original.getTipo() != TipoLiquidacao.LIQUIDACAO) {
      throw new EstornoInvalidoException(
          liquidacaoOriginalId, "só é possível estornar uma LIQUIDACAO, não um ESTORNO");
    }
    if (liquidacaoRepository.existsByLiquidacaoEstornada_Id(liquidacaoOriginalId)) {
      throw new EstornoInvalidoException(liquidacaoOriginalId, "já foi estornada anteriormente");
    }

    Recebivel recebivel = original.getRecebivel();
    String moedaPagamentoCodigo = original.getMoedaPagamento().getCodigo();
    Caixa caixa =
        caixaRepository
            .findById(moedaPagamentoCodigo)
            .orElseThrow(() -> new MoedaNaoEncontradaException(moedaPagamentoCodigo));

    caixa.setSaldo(caixa.getSaldo().add(original.getValorLiquido()));
    recebivel.setStatus(StatusRecebivel.ESTORNADO);

    try {
      caixaRepository.flush();
    } catch (ObjectOptimisticLockingFailureException e) {
      throw new ConflitoConcorrenciaException(
          "estorno da liquidação %s".formatted(liquidacaoOriginalId));
    }

    Liquidacao estorno =
        Liquidacao.builder()
            .recebivel(recebivel)
            .cedente(original.getCedente())
            .tipo(TipoLiquidacao.ESTORNO)
            .liquidacaoEstornada(original)
            .valorFace(original.getValorFace())
            .moedaTitulo(original.getMoedaTitulo())
            .taxaBaseUsada(original.getTaxaBaseUsada())
            .taxaBaseRef(original.getTaxaBaseRef())
            .spreadUsado(original.getSpreadUsado())
            .prazoMesesUsado(original.getPrazoMesesUsado())
            .valorPresente(original.getValorPresente())
            .moedaPagamento(original.getMoedaPagamento())
            .taxaCambioUsada(original.getTaxaCambioUsada())
            .taxaCambioRef(original.getTaxaCambioRef())
            .valorLiquido(original.getValorLiquido())
            .build();

    Liquidacao salvo = liquidacaoRepository.save(estorno);
    metricasNegocio.registrarEstorno(moedaPagamentoCodigo, original.getValorLiquido());
    log.atInfo()
        .setMessage("Estorno concluído")
        .addKeyValue("liquidacaoOriginalId", liquidacaoOriginalId)
        .addKeyValue("valorDevolvido", original.getValorLiquido())
        .log();
    return salvo;
  }

  private Moeda buscarMoeda(String codigo) {
    return moedaRepository
        .findById(codigo)
        .orElseThrow(() -> new MoedaNaoEncontradaException(codigo));
  }
}
