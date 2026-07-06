package com.srmasset.creditengine.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.domain.Liquidacao;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.domain.StatusRecebivel;
import com.srmasset.creditengine.domain.TaxaCambio;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.domain.TipoRecebivel;
import com.srmasset.creditengine.repository.CedenteRepository;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.RecebivelRepository;
import com.srmasset.creditengine.repository.TaxaCambioRepository;
import com.srmasset.creditengine.repository.TaxaMercadoRepository;
import com.srmasset.creditengine.repository.TipoRecebivelRepository;
import com.srmasset.creditengine.service.LiquidacaoService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Caminho de 2 camadas (Controller → Repository, SQL nativo) — exercitado contra um Postgres real
 * via Testcontainers, não mockado: o que importa aqui é a montagem dinâmica do WHERE (só inclui os
 * predicados informados) e o plano de query real, não a lógica de negócio.
 *
 * <p>{@code @Transactional}: o container Postgres é único pra classe inteira (não recriado por
 * método), então cada teste precisa do próprio rollback — sem isso, o segundo método a rodar
 * bateria na UNIQUE constraint do {@code documento} do cedente semeado no primeiro, e os totais de
 * {@code totalElements} acumulariam entre testes.
 *
 * <p>Mesma limitação de ambiente que {@link
 * com.srmasset.creditengine.service.LiquidacaoConcorrenciaIT}: requer Docker Engine com {@code
 * MinAPIVersion <= 1.32}; roda normalmente no CI do GitHub.
 */
@SpringBootTest
@Testcontainers
@Transactional
class ExtratoLiquidacaoRepositoryIT {

  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private ExtratoLiquidacaoRepository extratoLiquidacaoRepository;
  @Autowired private LiquidacaoService liquidacaoService;
  @Autowired private RecebivelRepository recebivelRepository;
  @Autowired private CedenteRepository cedenteRepository;
  @Autowired private MoedaRepository moedaRepository;
  @Autowired private TipoRecebivelRepository tipoRecebivelRepository;
  @Autowired private TaxaMercadoRepository taxaMercadoRepository;
  @Autowired private TaxaCambioRepository taxaCambioRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  private Cedente cedenteA;
  private Cedente cedenteB;
  private UUID liquidacaoL1Id; // cedente A, pagamento BRL, criado_em = 2026-01-10 (estornada)
  private UUID liquidacaoL2Id; // cedente B, pagamento USD, criado_em = 2026-01-20
  private UUID liquidacaoL3Id; // cedente A, pagamento BRL, criado_em = 2026-01-30
  private UUID estornoE1Id; // ESTORNO de L1 (cedente A, BRL), criado_em = 2026-02-05

  @BeforeEach
  void seed() {
    Moeda brl = moedaRepository.findById("BRL").orElseThrow();
    Moeda usd = moedaRepository.findById("USD").orElseThrow();
    TipoRecebivel duplicata = tipoRecebivelRepository.findById("DUPLICATA_MERCANTIL").orElseThrow();

    cedenteA =
        cedenteRepository.save(
            Cedente.builder().nome("Cedente A").documento("11111111111").build());
    cedenteB =
        cedenteRepository.save(
            Cedente.builder().nome("Cedente B").documento("22222222222").build());

    taxaMercadoRepository.save(
        TaxaMercado.builder()
            .moeda(brl)
            .indicador("CDI")
            .valor(new BigDecimal("0.010000"))
            .vigenteEm(Instant.now().minusSeconds(60))
            .build());
    taxaCambioRepository.save(
        TaxaCambio.builder()
            .moedaOrigem(brl)
            .moedaDestino(usd)
            .valor(new BigDecimal("0.18500000"))
            .vigenteEm(Instant.now().minusSeconds(60))
            .build());

    Liquidacao l1 = liquidar(cedenteA, duplicata, brl, "BRL");
    Liquidacao l2 = liquidar(cedenteB, duplicata, brl, "USD");
    Liquidacao l3 = liquidar(cedenteA, duplicata, brl, "BRL");
    Liquidacao e1 = liquidacaoService.estornar(l1.getId());

    liquidacaoL1Id = l1.getId();
    liquidacaoL2Id = l2.getId();
    liquidacaoL3Id = l3.getId();
    estornoE1Id = e1.getId();

    // O teste inteiro roda numa única transação (@Transactional, rollback no fim — nunca commita).
    // O INSERT da última liquidação criada fica pendente no persistence context do Hibernate até
    // um flush acontecer; como as leituras daqui pra frente são SQL puro via JdbcTemplate/
    // NamedParameterJdbcTemplate (não passam pelo EntityManager), elas nunca disparariam esse
    // flush sozinhas — sem isso, a liquidação mais recente simplesmente não aparecia pra query.
    entityManager.flush();

    // criado_em é gerado pelo banco no INSERT (@Generated) — sobrescrito aqui via SQL direto pra
    // controlar datas determinísticas nos testes de filtro por período/ordenação.
    forcarCriadoEm(liquidacaoL1Id, Instant.parse("2026-01-10T00:00:00Z"));
    forcarCriadoEm(liquidacaoL2Id, Instant.parse("2026-01-20T00:00:00Z"));
    forcarCriadoEm(liquidacaoL3Id, Instant.parse("2026-01-30T00:00:00Z"));
    forcarCriadoEm(estornoE1Id, Instant.parse("2026-02-05T00:00:00Z"));
  }

  private Liquidacao liquidar(
      Cedente cedente, TipoRecebivel tipoRecebivel, Moeda moedaTitulo, String moedaPagamento) {
    Recebivel recebivel =
        recebivelRepository.save(
            Recebivel.builder()
                .cedente(cedente)
                .tipoRecebivel(tipoRecebivel)
                .valorFace(new BigDecimal("1000.00"))
                .moedaTitulo(moedaTitulo)
                .dataVencimento(LocalDate.now().plusDays(30))
                .status(StatusRecebivel.PENDENTE)
                .build());
    return liquidacaoService.liquidar(recebivel.getId(), moedaPagamento);
  }

  private void forcarCriadoEm(UUID liquidacaoId, Instant criadoEm) {
    jdbcTemplate.update(
        "update liquidacao set criado_em = ? where id = ?",
        java.sql.Timestamp.from(criadoEm),
        liquidacaoId);
  }

  @Test
  void buscar_semFiltros_retornaTodasOrdenadasPorCriadoEmDesc() {
    var filtro = new ExtratoLiquidacaoFiltro(null, null, null, null, null, 0, 20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    // também prova que nenhum parâmetro nulo quebra a query (ver javadoc do repository).
    assertThat(resultado.totalElements()).isEqualTo(4);
    assertThat(resultado.content())
        .extracting(ExtratoLiquidacaoLinha::id)
        .containsExactly(estornoE1Id, liquidacaoL3Id, liquidacaoL2Id, liquidacaoL1Id);
  }

  @Test
  void buscar_marcaComoEstornadaSoALiquidacaoQueTemEstorno() {
    var filtro = new ExtratoLiquidacaoFiltro(null, null, null, null, null, 0, 20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    assertThat(resultado.content())
        .extracting(
            ExtratoLiquidacaoLinha::id,
            ExtratoLiquidacaoLinha::tipo,
            ExtratoLiquidacaoLinha::estornada)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(estornoE1Id, "ESTORNO", false),
            org.assertj.core.groups.Tuple.tuple(liquidacaoL3Id, "LIQUIDACAO", false),
            org.assertj.core.groups.Tuple.tuple(liquidacaoL2Id, "LIQUIDACAO", false),
            org.assertj.core.groups.Tuple.tuple(liquidacaoL1Id, "LIQUIDACAO", true));
  }

  @Test
  void buscar_linhaDeEstorno_trazReferenciaDaLiquidacaoOriginal() {
    var filtro = new ExtratoLiquidacaoFiltro(null, null, null, null, null, 0, 20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    var estorno = resultado.content().get(0);
    assertThat(estorno.id()).isEqualTo(estornoE1Id);
    assertThat(estorno.liquidacaoEstornadaId()).isEqualTo(liquidacaoL1Id);
    assertThat(estorno.liquidacaoEstornadaCriadoEm())
        .isEqualTo(Instant.parse("2026-01-10T00:00:00Z"));
    // liquidações não apontam pra ninguém
    assertThat(resultado.content().get(1).liquidacaoEstornadaId()).isNull();
  }

  @Test
  void buscar_filtrandoPorTipo_retornaSoAqueleTipo() {
    var soEstornos =
        extratoLiquidacaoRepository.buscar(
            new ExtratoLiquidacaoFiltro(null, null, "ESTORNO", null, null, 0, 20));
    var soLiquidacoes =
        extratoLiquidacaoRepository.buscar(
            new ExtratoLiquidacaoFiltro(null, null, "LIQUIDACAO", null, null, 0, 20));

    assertThat(soEstornos.content())
        .extracting(ExtratoLiquidacaoLinha::id)
        .containsExactly(estornoE1Id);
    assertThat(soLiquidacoes.totalElements()).isEqualTo(3);
    assertThat(soLiquidacoes.content()).allMatch(l -> l.tipo().equals("LIQUIDACAO"));
  }

  @Test
  void buscar_filtrandoPorCedente_retornaSoAsDaqueleCedente() {
    var filtro = new ExtratoLiquidacaoFiltro(cedenteA.getId(), null, null, null, null, 0, 20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    // cedente A: L1, L3 e o estorno E1 (o estorno herda o cedente da liquidação original).
    assertThat(resultado.totalElements()).isEqualTo(3);
    assertThat(resultado.content())
        .extracting(ExtratoLiquidacaoLinha::id)
        .containsExactly(estornoE1Id, liquidacaoL3Id, liquidacaoL1Id);
    assertThat(resultado.content()).allMatch(l -> l.cedenteId().equals(cedenteA.getId()));
  }

  @Test
  void buscar_filtrandoPorMoedaPagamento_retornaSoAquelaMoeda() {
    var filtro = new ExtratoLiquidacaoFiltro(null, "USD", null, null, null, 0, 20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    assertThat(resultado.totalElements()).isEqualTo(1);
    assertThat(resultado.content().get(0).id()).isEqualTo(liquidacaoL2Id);
    assertThat(resultado.content().get(0).moedaPagamento()).isEqualTo("USD");
  }

  @Test
  void buscar_filtrandoPorPeriodo_dataFimEExclusiva() {
    var filtro =
        new ExtratoLiquidacaoFiltro(
            null,
            null,
            null,
            Instant.parse("2026-01-15T00:00:00Z"),
            Instant.parse("2026-01-25T00:00:00Z"),
            0,
            20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    assertThat(resultado.totalElements()).isEqualTo(1);
    assertThat(resultado.content().get(0).id()).isEqualTo(liquidacaoL2Id);
  }

  @Test
  void buscar_paginacao_respeitaLimitEOffsetMantendoOrdenacao() {
    var pagina0 =
        extratoLiquidacaoRepository.buscar(
            new ExtratoLiquidacaoFiltro(null, null, null, null, null, 0, 1));
    var pagina1 =
        extratoLiquidacaoRepository.buscar(
            new ExtratoLiquidacaoFiltro(null, null, null, null, null, 1, 1));

    assertThat(pagina0.totalElements()).isEqualTo(4);
    assertThat(pagina0.totalPages()).isEqualTo(4);
    assertThat(pagina0.content())
        .extracting(ExtratoLiquidacaoLinha::id)
        .containsExactly(estornoE1Id);
    assertThat(pagina1.content())
        .extracting(ExtratoLiquidacaoLinha::id)
        .containsExactly(liquidacaoL3Id);
  }

  @Test
  void buscar_filtroSemNenhumaCorrespondencia_retornaPaginaVazia() {
    var filtro = new ExtratoLiquidacaoFiltro(UUID.randomUUID(), null, null, null, null, 0, 20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    assertThat(resultado.content()).isEmpty();
    assertThat(resultado.totalElements()).isZero();
    assertThat(resultado.totalPages()).isZero();
  }

  @Test
  void buscar_combinandoCedenteEMoeda_intersectaOsFiltros() {
    var filtro = new ExtratoLiquidacaoFiltro(cedenteA.getId(), "USD", null, null, null, 0, 20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    // cedente A só tem liquidações em BRL — cedente + moeda combinados não deve trazer nada.
    assertThat(resultado.content()).isEmpty();
  }

  @Test
  void buscar_semUso_naoAfetaOutroTeste_cedenteBSoApareceComFiltroDele() {
    var filtro = new ExtratoLiquidacaoFiltro(cedenteB.getId(), null, null, null, null, 0, 20);

    var resultado = extratoLiquidacaoRepository.buscar(filtro);

    assertThat(resultado.content())
        .extracting(ExtratoLiquidacaoLinha::id)
        .containsExactly(liquidacaoL2Id);
  }
}
