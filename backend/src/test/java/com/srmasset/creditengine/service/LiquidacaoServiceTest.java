package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.Caixa;
import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.domain.Liquidacao;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.domain.StatusRecebivel;
import com.srmasset.creditengine.domain.TaxaCambio;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.domain.TipoLiquidacao;
import com.srmasset.creditengine.domain.TipoRecebivel;
import com.srmasset.creditengine.exception.ConflitoConcorrenciaException;
import com.srmasset.creditengine.exception.EstornoInvalidoException;
import com.srmasset.creditengine.exception.MoedaNaoEncontradaException;
import com.srmasset.creditengine.exception.RecebivelJaLiquidadoException;
import com.srmasset.creditengine.exception.RecebivelNaoEncontradoException;
import com.srmasset.creditengine.exception.SaldoInsuficienteException;
import com.srmasset.creditengine.metrics.MetricasNegocio;
import com.srmasset.creditengine.pricing.DuplicataMercantilPricingStrategy;
import com.srmasset.creditengine.pricing.MotorPrecificacao;
import com.srmasset.creditengine.pricing.PrazoCalculator;
import com.srmasset.creditengine.pricing.Precisao;
import com.srmasset.creditengine.pricing.PricingStrategyResolver;
import com.srmasset.creditengine.repository.CaixaRepository;
import com.srmasset.creditengine.repository.LiquidacaoRepository;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.RecebivelRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Mocka os repositórios/services de I/O (persistência e leitura de taxas), mas usa {@link
 * PricingStrategyResolver}, {@link MotorPrecificacao} e {@link PrazoCalculator} reais — igual ao
 * padrão de {@link SimulacaoServiceTest} — pra validar a fórmula de ponta a ponta, não só a
 * orquestração.
 */
@ExtendWith(MockitoExtension.class)
class LiquidacaoServiceTest {

  private static final Instant AGORA = Instant.parse("2026-07-01T00:00:00Z");

  @Mock private RecebivelRepository recebivelRepository;
  @Mock private CaixaRepository caixaRepository;
  @Mock private LiquidacaoRepository liquidacaoRepository;
  @Mock private MoedaRepository moedaRepository;
  @Mock private TaxaMercadoService taxaMercadoService;
  @Mock private CambioService cambioService;

  private MeterRegistry meterRegistry;
  private LiquidacaoService liquidacaoService;

  private Moeda brl;
  private Moeda usd;
  private Cedente cedente;
  private TipoRecebivel duplicata;
  private Recebivel recebivel;
  private TaxaMercado cdi;
  private Caixa caixaBrl;
  private Caixa caixaUsd;

  @BeforeEach
  void setUp() {
    PricingStrategyResolver resolver =
        new PricingStrategyResolver(List.of(new DuplicataMercantilPricingStrategy()));
    MotorPrecificacao motorPrecificacao = new MotorPrecificacao();
    Clock clockFixo = Clock.fixed(AGORA, ZoneOffset.UTC);
    PrazoCalculator prazoCalculator = new PrazoCalculator(clockFixo);

    // Registry real (não mock): os asserts de métricas leem o contador de verdade.
    meterRegistry = new SimpleMeterRegistry();

    liquidacaoService =
        new LiquidacaoService(
            recebivelRepository,
            caixaRepository,
            liquidacaoRepository,
            moedaRepository,
            resolver,
            motorPrecificacao,
            prazoCalculator,
            taxaMercadoService,
            cambioService,
            new MetricasNegocio(meterRegistry));

    brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    usd = Moeda.builder().codigo("USD").nome("Dólar Americano").build();
    cedente = Cedente.builder().id(UUID.randomUUID()).nome("Acme Ltda").documento("123").build();
    duplicata =
        TipoRecebivel.builder()
            .codigo("DUPLICATA_MERCANTIL")
            .nome("Duplicata Mercantil")
            .ativo(true)
            .build();
    recebivel =
        Recebivel.builder()
            .id(UUID.randomUUID())
            .cedente(cedente)
            .tipoRecebivel(duplicata)
            .valorFace(new BigDecimal("1000.00"))
            .moedaTitulo(brl)
            .dataVencimento(LocalDate.of(2026, 7, 31))
            .status(StatusRecebivel.PENDENTE)
            .version(0L)
            .build();
    cdi =
        TaxaMercado.builder().moeda(brl).indicador("CDI").valor(new BigDecimal("0.010000")).build();
    caixaBrl =
        Caixa.builder().moedaCodigo("BRL").saldo(new BigDecimal("100000.00")).version(0L).build();
    caixaUsd =
        Caixa.builder().moedaCodigo("USD").saldo(new BigDecimal("100000.00")).version(0L).build();
  }

  @Test
  void liquidar_mesmaMoeda_debitaCaixaERegistraAuditoriaCompleta() {
    when(recebivelRepository.findById(recebivel.getId())).thenReturn(Optional.of(recebivel));
    when(taxaMercadoService.buscarTaxaVigente("BRL")).thenReturn(cdi);
    when(cambioService.buscarSeNecessario("BRL", "BRL")).thenReturn(Optional.empty());
    when(caixaRepository.findById("BRL")).thenReturn(Optional.of(caixaBrl));
    when(moedaRepository.findById("BRL")).thenReturn(Optional.of(brl));
    when(liquidacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Liquidacao resultado = liquidacaoService.liquidar(recebivel.getId(), "BRL");

    assertThat(resultado.getTipo()).isEqualTo(TipoLiquidacao.LIQUIDACAO);
    assertThat(resultado.getTaxaCambioUsada()).isNull();
    assertThat(resultado.getTaxaBaseUsada()).isEqualByComparingTo(cdi.getValor());
    assertThat(resultado.getSpreadUsado()).isEqualByComparingTo("0.015000");
    assertThat(resultado.getValorLiquido())
        .isEqualByComparingTo(
            resultado
                .getValorPresente()
                .setScale(Precisao.ESCALA_VALOR_MONETARIO, Precisao.ARREDONDAMENTO));
    assertThat(caixaBrl.getSaldo())
        .isEqualByComparingTo(new BigDecimal("100000.00").subtract(resultado.getValorLiquido()));
    assertThat(recebivel.getStatus()).isEqualTo(StatusRecebivel.LIQUIDADO);
    verify(caixaRepository).flush();
    assertThat(meterRegistry.counter("srm.liquidacoes", "moeda", "BRL").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("srm.liquidacoes.valor", "moeda", "BRL").count())
        .isEqualTo(resultado.getValorLiquido().doubleValue());
  }

  @Test
  void liquidar_crossCurrency_aplicaConversaoDepoisDoDesagio() {
    TaxaCambio taxaCambio =
        TaxaCambio.builder()
            .moedaOrigem(brl)
            .moedaDestino(usd)
            .valor(new BigDecimal("0.18500000"))
            .build();

    when(recebivelRepository.findById(recebivel.getId())).thenReturn(Optional.of(recebivel));
    when(taxaMercadoService.buscarTaxaVigente("BRL")).thenReturn(cdi);
    when(cambioService.buscarSeNecessario("BRL", "USD")).thenReturn(Optional.of(taxaCambio));
    when(caixaRepository.findById("USD")).thenReturn(Optional.of(caixaUsd));
    when(moedaRepository.findById("USD")).thenReturn(Optional.of(usd));
    when(liquidacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Liquidacao resultado = liquidacaoService.liquidar(recebivel.getId(), "USD");

    assertThat(resultado.getTaxaCambioUsada()).isEqualByComparingTo("0.18500000");
    assertThat(resultado.getValorLiquido())
        .isEqualByComparingTo(
            resultado
                .getValorPresente()
                .multiply(taxaCambio.getValor())
                .setScale(Precisao.ESCALA_VALOR_MONETARIO, Precisao.ARREDONDAMENTO));
    assertThat(caixaUsd.getSaldo())
        .isEqualByComparingTo(new BigDecimal("100000.00").subtract(resultado.getValorLiquido()));
  }

  @Test
  void liquidar_recebivelInexistente_lancaExcecaoSemTocarCaixaOuLiquidacao() {
    UUID idInexistente = UUID.randomUUID();
    when(recebivelRepository.findById(idInexistente)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> liquidacaoService.liquidar(idInexistente, "BRL"))
        .isInstanceOf(RecebivelNaoEncontradoException.class);

    verifyNoInteractions(caixaRepository, liquidacaoRepository);
  }

  @Test
  void liquidar_recebivelNaoPendente_lancaRecebivelJaLiquidado() {
    recebivel.setStatus(StatusRecebivel.LIQUIDADO);
    when(recebivelRepository.findById(recebivel.getId())).thenReturn(Optional.of(recebivel));

    assertThatThrownBy(() -> liquidacaoService.liquidar(recebivel.getId(), "BRL"))
        .isInstanceOf(RecebivelJaLiquidadoException.class);

    verifyNoInteractions(caixaRepository, liquidacaoRepository);
  }

  @Test
  void liquidar_caixaInexistente_lancaSaldoInsuficiente() {
    when(recebivelRepository.findById(recebivel.getId())).thenReturn(Optional.of(recebivel));
    when(taxaMercadoService.buscarTaxaVigente("BRL")).thenReturn(cdi);
    when(cambioService.buscarSeNecessario("BRL", "BRL")).thenReturn(Optional.empty());
    when(caixaRepository.findById("BRL")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> liquidacaoService.liquidar(recebivel.getId(), "BRL"))
        .isInstanceOf(SaldoInsuficienteException.class);

    verify(caixaRepository, never()).flush();
    verifyNoInteractions(liquidacaoRepository);
  }

  @Test
  void liquidar_saldoMenorQueValorLiquido_lancaSaldoInsuficienteSemMutarEstado() {
    caixaBrl.setSaldo(new BigDecimal("1.00"));
    when(recebivelRepository.findById(recebivel.getId())).thenReturn(Optional.of(recebivel));
    when(taxaMercadoService.buscarTaxaVigente("BRL")).thenReturn(cdi);
    when(cambioService.buscarSeNecessario("BRL", "BRL")).thenReturn(Optional.empty());
    when(caixaRepository.findById("BRL")).thenReturn(Optional.of(caixaBrl));

    assertThatThrownBy(() -> liquidacaoService.liquidar(recebivel.getId(), "BRL"))
        .isInstanceOf(SaldoInsuficienteException.class);

    assertThat(caixaBrl.getSaldo()).isEqualByComparingTo("1.00");
    assertThat(recebivel.getStatus()).isEqualTo(StatusRecebivel.PENDENTE);
    verify(caixaRepository, never()).flush();
    verifyNoInteractions(liquidacaoRepository);
    // Falha não conta como liquidação nas métricas de negócio.
    assertThat(meterRegistry.counter("srm.liquidacoes", "moeda", "BRL").count()).isZero();
  }

  @Test
  void liquidar_conflitoOptimisticLocking_traduzParaConflitoConcorrencia() {
    when(recebivelRepository.findById(recebivel.getId())).thenReturn(Optional.of(recebivel));
    when(taxaMercadoService.buscarTaxaVigente("BRL")).thenReturn(cdi);
    when(cambioService.buscarSeNecessario("BRL", "BRL")).thenReturn(Optional.empty());
    when(caixaRepository.findById("BRL")).thenReturn(Optional.of(caixaBrl));
    doThrow(new ObjectOptimisticLockingFailureException(Caixa.class, "BRL"))
        .when(caixaRepository)
        .flush();

    assertThatThrownBy(() -> liquidacaoService.liquidar(recebivel.getId(), "BRL"))
        .isInstanceOf(ConflitoConcorrenciaException.class);

    verifyNoInteractions(liquidacaoRepository);
  }

  @Test
  void liquidar_moedaPagamentoInexistente_lancaMoedaNaoEncontrada() {
    when(recebivelRepository.findById(recebivel.getId())).thenReturn(Optional.of(recebivel));
    when(taxaMercadoService.buscarTaxaVigente("BRL")).thenReturn(cdi);
    when(cambioService.buscarSeNecessario("BRL", "BRL")).thenReturn(Optional.empty());
    when(caixaRepository.findById("BRL")).thenReturn(Optional.of(caixaBrl));
    when(moedaRepository.findById("BRL")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> liquidacaoService.liquidar(recebivel.getId(), "BRL"))
        .isInstanceOf(MoedaNaoEncontradaException.class);

    verifyNoInteractions(liquidacaoRepository);
  }

  @Test
  void estornar_caminhoFeliz_devolveSaldoEEstornaRecebivel() {
    recebivel.setStatus(StatusRecebivel.LIQUIDADO);
    Liquidacao original = liquidacaoOriginal();
    when(liquidacaoRepository.findById(original.getId())).thenReturn(Optional.of(original));
    when(liquidacaoRepository.existsByLiquidacaoEstornada_Id(original.getId())).thenReturn(false);
    when(caixaRepository.findById("BRL")).thenReturn(Optional.of(caixaBrl));
    when(liquidacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    BigDecimal saldoAntes = caixaBrl.getSaldo();
    Liquidacao estorno = liquidacaoService.estornar(original.getId());

    assertThat(estorno.getTipo()).isEqualTo(TipoLiquidacao.ESTORNO);
    assertThat(estorno.getLiquidacaoEstornada()).isEqualTo(original);
    assertThat(estorno.getValorLiquido()).isEqualByComparingTo(original.getValorLiquido());
    assertThat(caixaBrl.getSaldo())
        .isEqualByComparingTo(saldoAntes.add(original.getValorLiquido()));
    assertThat(recebivel.getStatus()).isEqualTo(StatusRecebivel.ESTORNADO);
    verify(caixaRepository).flush();
    assertThat(meterRegistry.counter("srm.estornos", "moeda", "BRL").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("srm.estornos.valor", "moeda", "BRL").count())
        .isEqualTo(original.getValorLiquido().doubleValue());
  }

  @Test
  void estornar_liquidacaoInexistente_lancaEstornoInvalido() {
    UUID idInexistente = UUID.randomUUID();
    when(liquidacaoRepository.findById(idInexistente)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> liquidacaoService.estornar(idInexistente))
        .isInstanceOf(EstornoInvalidoException.class);

    verifyNoInteractions(caixaRepository);
  }

  @Test
  void estornar_tentandoEstornarUmEstorno_lancaEstornoInvalido() {
    Liquidacao original = liquidacaoOriginal();
    original.setTipo(TipoLiquidacao.ESTORNO);
    when(liquidacaoRepository.findById(original.getId())).thenReturn(Optional.of(original));

    assertThatThrownBy(() -> liquidacaoService.estornar(original.getId()))
        .isInstanceOf(EstornoInvalidoException.class);

    verifyNoInteractions(caixaRepository);
  }

  @Test
  void estornar_jaEstornadaAnteriormente_lancaEstornoInvalido() {
    Liquidacao original = liquidacaoOriginal();
    when(liquidacaoRepository.findById(original.getId())).thenReturn(Optional.of(original));
    when(liquidacaoRepository.existsByLiquidacaoEstornada_Id(original.getId())).thenReturn(true);

    assertThatThrownBy(() -> liquidacaoService.estornar(original.getId()))
        .isInstanceOf(EstornoInvalidoException.class);

    verifyNoInteractions(caixaRepository);
  }

  @Test
  void estornar_conflitoOptimisticLocking_traduzParaConflitoConcorrencia() {
    Liquidacao original = liquidacaoOriginal();
    when(liquidacaoRepository.findById(original.getId())).thenReturn(Optional.of(original));
    when(liquidacaoRepository.existsByLiquidacaoEstornada_Id(original.getId())).thenReturn(false);
    when(caixaRepository.findById("BRL")).thenReturn(Optional.of(caixaBrl));
    doThrow(new ObjectOptimisticLockingFailureException(Caixa.class, "BRL"))
        .when(caixaRepository)
        .flush();

    assertThatThrownBy(() -> liquidacaoService.estornar(original.getId()))
        .isInstanceOf(ConflitoConcorrenciaException.class);

    verify(liquidacaoRepository, never()).save(any());
  }

  @Test
  void estornar_caixaInexistente_lancaMoedaNaoEncontrada() {
    Liquidacao original = liquidacaoOriginal();
    when(liquidacaoRepository.findById(original.getId())).thenReturn(Optional.of(original));
    when(liquidacaoRepository.existsByLiquidacaoEstornada_Id(original.getId())).thenReturn(false);
    when(caixaRepository.findById("BRL")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> liquidacaoService.estornar(original.getId()))
        .isInstanceOf(MoedaNaoEncontradaException.class);

    verify(liquidacaoRepository, never()).save(any());
  }

  private Liquidacao liquidacaoOriginal() {
    return Liquidacao.builder()
        .id(UUID.randomUUID())
        .recebivel(recebivel)
        .cedente(cedente)
        .tipo(TipoLiquidacao.LIQUIDACAO)
        .valorFace(recebivel.getValorFace())
        .moedaTitulo(brl)
        .taxaBaseUsada(cdi.getValor())
        .taxaBaseRef(cdi)
        .spreadUsado(new BigDecimal("0.015000"))
        .prazoMesesUsado(new BigDecimal("1.0000"))
        .valorPresente(new BigDecimal("975.609756"))
        .moedaPagamento(brl)
        .valorLiquido(new BigDecimal("975.61"))
        .build();
  }
}
