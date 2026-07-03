package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.TaxaCambio;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.domain.TipoRecebivel;
import com.srmasset.creditengine.dto.request.SimulacaoRecebivelRequest;
import com.srmasset.creditengine.dto.response.SimulacaoRecebivelResponse;
import com.srmasset.creditengine.exception.TipoRecebivelNaoSuportadoException;
import com.srmasset.creditengine.pricing.DuplicataMercantilPricingStrategy;
import com.srmasset.creditengine.pricing.MotorPrecificacao;
import com.srmasset.creditengine.pricing.PrazoCalculator;
import com.srmasset.creditengine.pricing.Precisao;
import com.srmasset.creditengine.pricing.PricingStrategyResolver;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.TipoRecebivelRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mocka só os repositórios/services de LEITURA — não há {@code CaixaRepository}, {@code
 * LiquidacaoRepository} nem {@code RecebivelRepository} pra mockar, porque {@link SimulacaoService}
 * não os injeta (é a prova, em teste, de que a simulação não tem efeito colateral).
 */
@ExtendWith(MockitoExtension.class)
class SimulacaoServiceTest {

  @Mock private TipoRecebivelRepository tipoRecebivelRepository;
  @Mock private MoedaRepository moedaRepository;
  @Mock private TaxaMercadoService taxaMercadoService;
  @Mock private CambioService cambioService;

  private SimulacaoService simulacaoService;

  @BeforeEach
  void setUp() {
    PricingStrategyResolver resolver =
        new PricingStrategyResolver(List.of(new DuplicataMercantilPricingStrategy()));
    MotorPrecificacao motorPrecificacao = new MotorPrecificacao();
    Clock clockFixo = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);
    PrazoCalculator prazoCalculator = new PrazoCalculator(clockFixo);

    simulacaoService =
        new SimulacaoService(
            tipoRecebivelRepository,
            moedaRepository,
            resolver,
            motorPrecificacao,
            prazoCalculator,
            taxaMercadoService,
            cambioService);
  }

  @Test
  void mesmaMoeda_naoAplicaConversaoCambial() {
    TipoRecebivel duplicata =
        TipoRecebivel.builder()
            .codigo("DUPLICATA_MERCANTIL")
            .nome("Duplicata Mercantil")
            .ativo(true)
            .build();
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();

    when(tipoRecebivelRepository.findById("DUPLICATA_MERCANTIL"))
        .thenReturn(Optional.of(duplicata));
    when(moedaRepository.findById("BRL")).thenReturn(Optional.of(brl));
    when(taxaMercadoService.buscarTaxaVigente("BRL"))
        .thenReturn(
            TaxaMercado.builder()
                .moeda(brl)
                .indicador("CDI")
                .valor(new BigDecimal("0.010000"))
                .build());
    when(cambioService.buscarSeNecessario("BRL", "BRL")).thenReturn(Optional.empty());

    SimulacaoRecebivelResponse resposta =
        simulacaoService.simular(
            new SimulacaoRecebivelRequest(
                "DUPLICATA_MERCANTIL",
                new BigDecimal("1000.00"),
                "BRL",
                LocalDate.of(2026, 7, 31),
                "BRL"));

    assertThat(resposta.taxaCambioUsada()).isNull();
    assertThat(resposta.valorLiquido())
        .isEqualByComparingTo(
            resposta
                .valorPresente()
                .setScale(Precisao.ESCALA_VALOR_MONETARIO, Precisao.ARREDONDAMENTO));
  }

  @Test
  void moedasDiferentes_aplicaConversaoDepoisDoDesagio() {
    TipoRecebivel duplicata =
        TipoRecebivel.builder()
            .codigo("DUPLICATA_MERCANTIL")
            .nome("Duplicata Mercantil")
            .ativo(true)
            .build();
    Moeda brl = Moeda.builder().codigo("BRL").nome("Real Brasileiro").build();
    TaxaCambio taxaCambio = TaxaCambio.builder().valor(new BigDecimal("0.18500000")).build();

    when(tipoRecebivelRepository.findById("DUPLICATA_MERCANTIL"))
        .thenReturn(Optional.of(duplicata));
    when(moedaRepository.findById("BRL")).thenReturn(Optional.of(brl));
    when(taxaMercadoService.buscarTaxaVigente("BRL"))
        .thenReturn(
            TaxaMercado.builder()
                .moeda(brl)
                .indicador("CDI")
                .valor(new BigDecimal("0.010000"))
                .build());
    when(cambioService.buscarSeNecessario("BRL", "USD")).thenReturn(Optional.of(taxaCambio));

    SimulacaoRecebivelResponse resposta =
        simulacaoService.simular(
            new SimulacaoRecebivelRequest(
                "DUPLICATA_MERCANTIL",
                new BigDecimal("1000.00"),
                "BRL",
                LocalDate.of(2026, 7, 31),
                "USD"));

    assertThat(resposta.taxaCambioUsada()).isEqualByComparingTo("0.18500000");
    assertThat(resposta.valorLiquido())
        .isEqualByComparingTo(
            resposta
                .valorPresente()
                .multiply(new BigDecimal("0.18500000"))
                .setScale(Precisao.ESCALA_VALOR_MONETARIO, Precisao.ARREDONDAMENTO));
  }

  @Test
  void tipoRecebivelInexistente_lancaExcecaoSemChamarNadaDepois() {
    when(tipoRecebivelRepository.findById("INEXISTENTE")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                simulacaoService.simular(
                    new SimulacaoRecebivelRequest(
                        "INEXISTENTE",
                        new BigDecimal("1000.00"),
                        "BRL",
                        LocalDate.of(2026, 7, 31),
                        "BRL")))
        .isInstanceOf(TipoRecebivelNaoSuportadoException.class);
  }
}
