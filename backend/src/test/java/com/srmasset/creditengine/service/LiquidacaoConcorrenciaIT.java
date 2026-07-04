package com.srmasset.creditengine.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.srmasset.creditengine.domain.Caixa;
import com.srmasset.creditengine.domain.Cedente;
import com.srmasset.creditengine.domain.Moeda;
import com.srmasset.creditengine.domain.Recebivel;
import com.srmasset.creditengine.domain.StatusRecebivel;
import com.srmasset.creditengine.domain.TaxaMercado;
import com.srmasset.creditengine.domain.TipoRecebivel;
import com.srmasset.creditengine.exception.ConflitoConcorrenciaException;
import com.srmasset.creditengine.repository.CaixaRepository;
import com.srmasset.creditengine.repository.CedenteRepository;
import com.srmasset.creditengine.repository.MoedaRepository;
import com.srmasset.creditengine.repository.RecebivelRepository;
import com.srmasset.creditengine.repository.TaxaMercadoRepository;
import com.srmasset.creditengine.repository.TipoRecebivelRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Demonstração concreta da proteção de Optimistic Locking: duas liquidações concorrentes disputando
 * o mesmo {@link Caixa} — uma deve suceder, a outra deve falhar com {@link
 * ConflitoConcorrenciaException}, e o saldo final precisa refletir exatamente 1 débito, nunca 0 nem
 * 2 (nem negativo).
 *
 * <p>Requer um Docker Engine com {@code MinAPIVersion <= 1.32} (o probe de compatibilidade do
 * Testcontainers 1.21.x, gerenciado pelo Spring Boot 3.5.3, ainda usa essa versão fixa). Em
 * ambientes com Docker Engine muito recente (ex: 29.x, API 1.55) esse probe é rejeitado pelo
 * servidor e o teste falha na inicialização, antes mesmo de rodar — não é um bug deste teste, é uma
 * lacuna de compatibilidade do Testcontainers com engines muito novos. O mesmo cenário foi validado
 * manualmente contra a API real (dois `POST /api/recebiveis/lote` concorrentes via `curl`) sempre
 * que esse ambiente específico não conseguir rodar Testcontainers.
 */
@SpringBootTest
@Testcontainers
class LiquidacaoConcorrenciaIT {

  @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private LiquidacaoService liquidacaoService;
  @Autowired private RecebivelRepository recebivelRepository;
  @Autowired private CedenteRepository cedenteRepository;
  @Autowired private CaixaRepository caixaRepository;
  @Autowired private TaxaMercadoRepository taxaMercadoRepository;
  @Autowired private MoedaRepository moedaRepository;
  @Autowired private TipoRecebivelRepository tipoRecebivelRepository;

  @Test
  void duasLiquidacoesConcorrentesNoMesmoCaixa_umaSucedeAOutraFalhaPorConflito() throws Exception {
    Moeda brl = moedaRepository.findById("BRL").orElseThrow();
    TipoRecebivel duplicata = tipoRecebivelRepository.findById("DUPLICATA_MERCANTIL").orElseThrow();

    Cedente cedente =
        cedenteRepository.save(
            Cedente.builder().nome("Empresa Teste").documento("00000000000191").build());

    taxaMercadoRepository.save(
        TaxaMercado.builder()
            .moeda(brl)
            .indicador("CDI")
            .valor(new BigDecimal("0.010000"))
            .vigenteEm(Instant.now().minusSeconds(60))
            .build());

    Recebivel recebivel1 = criarRecebivelPendente(cedente, duplicata, brl);
    Recebivel recebivel2 = criarRecebivelPendente(cedente, duplicata, brl);

    BigDecimal saldoAntes = caixaRepository.findById("BRL").orElseThrow().getSaldo();

    CyclicBarrier barreira = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    List<Callable<Resultado>> tarefas =
        List.of(
            () -> liquidarComBarreira(barreira, recebivel1.getId()),
            () -> liquidarComBarreira(barreira, recebivel2.getId()));

    List<Future<Resultado>> futures = executor.invokeAll(tarefas);
    executor.shutdown();

    List<Resultado> resultados = futures.stream().map(this::obterResultado).toList();

    long totalSucessos = resultados.stream().filter(Resultado::sucesso).count();
    long totalConflitos = resultados.stream().filter(r -> !r.sucesso()).count();

    assertThat(totalSucessos)
        .as("exatamente uma das duas liquidações concorrentes deve suceder")
        .isEqualTo(1);
    assertThat(totalConflitos)
        .as("a outra deve falhar por ConflitoConcorrenciaException, não silenciosamente")
        .isEqualTo(1);

    BigDecimal valorDebitado =
        resultados.stream().filter(Resultado::sucesso).findFirst().orElseThrow().valorLiquido();
    BigDecimal saldoDepois = caixaRepository.findById("BRL").orElseThrow().getSaldo();

    assertThat(saldoAntes.subtract(saldoDepois))
        .as("saldo final deve refletir exatamente 1 débito, nunca 0 (perdido) nem 2 (duplicado)")
        .isEqualByComparingTo(valorDebitado);
    assertThat(saldoDepois).as("saldo nunca pode ficar negativo").isNotNegative();
  }

  private Recebivel criarRecebivelPendente(
      Cedente cedente, TipoRecebivel tipoRecebivel, Moeda moeda) {
    return recebivelRepository.save(
        Recebivel.builder()
            .cedente(cedente)
            .tipoRecebivel(tipoRecebivel)
            .valorFace(new BigDecimal("1000.00"))
            .moedaTitulo(moeda)
            .dataVencimento(LocalDate.now().plusDays(30))
            .status(StatusRecebivel.PENDENTE)
            .build());
  }

  private Resultado liquidarComBarreira(CyclicBarrier barreira, java.util.UUID recebivelId)
      throws Exception {
    barreira
        .await(); // força as duas threads a começarem juntas, maximizando a chance de colisão real
    try {
      var liquidacao = liquidacaoService.liquidar(recebivelId, "BRL");
      return new Resultado(true, liquidacao.getValorLiquido());
    } catch (ConflitoConcorrenciaException e) {
      return new Resultado(false, null);
    }
  }

  private Resultado obterResultado(Future<Resultado> future) {
    try {
      return future.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private record Resultado(boolean sucesso, BigDecimal valorLiquido) {}
}
