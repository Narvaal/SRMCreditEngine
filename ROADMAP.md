# Roadmap — SRM Credit Engine

Documento vivo para discutir e registrar os próximos passos do desenvolvimento. Atualizado incrementalmente conforme avançamos.

## Metodologia

- **SDD (Spec-Driven Development):** antes de codar, entendemos o domínio e escrevemos especificações claras (requisitos, contratos de API, modelos de dados, regras de negócio) que guiam a implementação e os testes. Código nasce a partir da spec, não o contrário.
- **Nível-alvo: Sênior.** Além do funcional, o projeto precisa demonstrar:
  - **Observabilidade** — logs estruturados, métricas, tracing.
  - **Escalabilidade** — arquitetura e modelagem preparadas para volume e concorrência.
  - **Automação** — CI/CD, git hooks, testes automatizados como porta de entrada do código.
- Solução deve ser **robusta e testável**: regras de negócio (principalmente o motor de precificação) cobertas por testes unitários desde o início, não como reboco final.

## Passos

### Passo 1 — Entendimento do Problema _(concluído)_

**Domínio de negócio**

A SRM Asset opera FIDCs: fundos que compram direitos creditórios (duplicatas, cheques pré-datados, contratos) de empresas — as **cedentes** — antecipando a elas o valor desses recebíveis. Em troca de dar liquidez imediata à cedente, o fundo compra o direito de receber o valor de face do título no vencimento, mas paga menos do que esse valor de face hoje. A diferença é o **deságio**: o preço do dinheiro no tempo, mais um prêmio pelo risco de cada tipo de ativo.

Com a carteira internacionalizada, o fundo hoje mantém caixa em mais de uma moeda (BRL e USD). Isso introduz uma segunda variável de risco/precificação além do prazo: a **moeda de liquidação** pode ser diferente da moeda em que o título foi emitido, exigindo conversão cambial dentro do próprio cálculo de precificação.

**O fluxo de negócio, em linhas gerais**

1. A mesa de operações recebe um **lote de recebíveis** de uma cedente (cada item com valor de face, vencimento/prazo, tipo de ativo e moeda do título).
2. Para cada recebível, o sistema aplica uma **regra de risco específica do tipo de ativo** (spread) sobre uma taxa base, trazendo o valor de face a valor presente:
   `VP = Valor Face / (1 + Taxa Base + Spread) ^ Prazo`
   - Duplicata Mercantil → spread 1,5% a.m.
   - Cheque Pré-datado → spread 2,5% a.m.
   - (outros tipos podem ser adicionados — daí o Strategy Pattern: a regra de risco é um algoritmo plugável por tipo de recebível.)
3. Se a moeda de **pagamento à cedente** for diferente da moeda do **título**, o valor presente calculado é convertido pela taxa de câmbio vigente — a conversão acontece **depois** do deságio, não antes.
4. O resultado (valor líquido a pagar) vira uma **transação de liquidação**, que precisa ser registrada de forma atômica e auditável: ou a liquidação acontece por completo (débito do caixa do fundo, crédito à cedente, baixa do recebível), ou não acontece — nunca "pela metade".
5. As mesas de operação depois consultam esse histórico via **extrato de liquidação**, filtrando por período, cedente e moeda — potencialmente sobre grandes volumes, então a leitura precisa ser performática mesmo que a escrita seja transacional e cuidadosa.

**Por que isso é difícil (não é só um CRUD)**

- **Precisão numérica é inegociável.** Estamos calculando dinheiro com juros compostos e conversão cambial encadeados — erro de arredondamento ou uso de ponto flutuante binário (`float`/`double`) pode gerar diferenças de centavos que, em volume, viram um problema de auditoria e compliance. O tipo numérico e a estratégia de arredondamento (quando e como arredondar) fazem parte do domínio, não é detalhe de implementação.
- **A regra de risco varia por tipo de ativo e deve ser extensível.** Novos tipos de recebível (e portanto novos spreads) vão aparecer; a lógica de cálculo não pode exigir `if/else` crescente no motor de precificação — daí o Strategy Pattern citado no desafio.
- **Câmbio é uma dimensão independente do risco do ativo.** A taxa de câmbio muda no tempo e precisa ter uma fonte de verdade (com histórico — "qual taxa estava valendo quando essa liquidação foi feita?" é uma pergunta de auditoria plausível).
- **Concorrência e integridade transacional.** Duas liquidações não podem colidir sobre o mesmo caixa/recebível de forma inconsistente (race condition). Isso pede ACID de verdade e, no nível Sênior, Optimistic Locking como mecanismo explícito de defesa.
- **Auditabilidade.** "Registrar a transação de forma auditável" implica que cada liquidação deve carregar não só o resultado, mas os insumos do cálculo (taxa base, spread aplicado, taxa de câmbio usada, timestamp) — o cálculo precisa ser reconstituível a partir do registro, não só o valor final armazenado.
- **Volume/analytics separado do transacional.** O extrato de liquidação é uma leitura analítica (filtros, paginação, grandes volumes) que não deve competir por lock nem por camada de negócio com o motor de precificação — por isso o desafio já pede separação: 3 camadas para o transacional, 2 camadas (sem passar pela de negócio) para relatórios.

**Entidades centrais que emergem do problema**

- **Moeda** (BRL, USD, ...)
- **Taxa de Câmbio** (par de moedas, valor, vigência/timestamp)
- **Cedente** (empresa que vende os recebíveis)
- **Tipo de Recebível / Produto** (Duplicata Mercantil, Cheque Pré-datado, ...) com sua regra de spread
- **Recebível** (valor de face, vencimento, moeda do título, tipo, cedente)
- **Transação de Liquidação** (recebível liquidado, valor presente calculado, moeda de pagamento, taxa de câmbio aplicada, valor líquido pago, timestamp, status)

**Decisões já tomadas**

- **Granularidade da liquidação: por recebível, não por lote.** Um lote é só a forma de entrada (várias linhas mandadas juntas numa mesma requisição), mas cada recebível é calculado e liquidado de forma independente. Se um item do lote falhar (dado inválido, erro de cálculo, etc.), os demais continuam sendo processados normalmente — não é tudo-ou-nada.
  - Implicação prática: a **transação atômica (ACID)** é no nível de 1 recebível → 1 transação de liquidação, não no nível do lote inteiro.
  - Implicação de API: o endpoint que recebe um lote precisa devolver um resultado **por item** (o que liquidou, o que falhou e por quê) — não um único status de sucesso/erro pra requisição toda. É um resultado parcial, não binário.

- **Taxa Base vem do mercado, não é inventada pela SRM.** Espelhando o mundo real: em operações em BRL a referência é o **CDI**, em USD é o **SOFR** — taxas públicas de mercado que mudam diariamente. O fundo não controla essa taxa, só consome. Quem *é* decisão da SRM é o **spread** (o risco por tipo de ativo), que fica por conta da política interna do fundo.
  - Implicação de modelagem: a Taxa Base é conceitualmente igual à Taxa de Câmbio — **um valor externo que varia no tempo e precisa de histórico** (auditoria: "qual CDI valia no dia dessa liquidação?"). Vamos modelar como uma "taxa de mercado" (valor + data de vigência), alimentada manualmente ou por integração mockada, no mesmo padrão pensado para câmbio — simulando como um sistema real se integraria a uma fonte de mercado (ex.: BACEN/B3 para CDI, uma API de FX para câmbio), mesmo que aqui seja mockado.

- **Prazo é em meses, com mês fixo de 30 dias.** O expoente da fórmula usa meses (compatível com o spread, que é "a.m."). Quando for preciso derivar o prazo a partir de datas (hoje → vencimento), a conversão é `dias_corridos / 30` — convenção simplificada (equivalente a ACT/30).
  - **Trade-off consciente:** isso diverge levemente do calendário real (meses têm 28–31 dias, não 30), mas é assim de propósito — mercado financeiro usa esse tipo de convenção fixa (30/360, ACT/360, ACT/30, etc.) exatamente para eliminar ambiguidade de calendário (bissexto, mês curto/longo) e garantir que o cálculo seja sempre previsível, reprodutível e fácil de testar. A distorção é conhecida e documentada, não uma falha.

- **Histórico é append-only — nunca editar/apagar uma liquidação registrada.** Correção de erro não reescreve o passado: gera uma transação nova de **estorno** (referenciando a original) e, se necessário, uma nova liquidação correta. Igual a um livro-razão contábil (ledger) — só se adiciona, nunca se apaga. É o padrão em qualquer sistema de crédito/financeiro de verdade, e é o que de fato entrega o requisito de "transação auditável" do desafio: dá pra reconstituir o erro, o estorno e a correção, sem perder rastro.

- **Caixa do fundo é modelado com saldo controlado, por moeda.** Existe uma "conta" por moeda (ex.: Caixa BRL, Caixa USD) com saldo real. Antes de liquidar, o sistema verifica se há saldo suficiente naquela moeda; se não houver, a liquidação é recusada. Débito do saldo e registro da liquidação acontecem na mesma transação atômica.
  - **Por quê:** é o cenário real onde a race condition do requisito de concorrência (Sênior) se manifesta de fato — duas liquidações simultâneas competindo pelo mesmo saldo. Sem saldo controlado, não haveria nada de concreto pra proteger com Optimistic Locking. Com saldo controlado, o Optimistic Locking garante que, se duas liquidações concorrentes disputarem o mesmo caixa, uma falha e é reprocessada, em vez de deixar o saldo ir negativo.

**Perguntas em aberto:** nenhuma pendente no momento — entendimento do problema fechado. Próximo passo é transformar essas decisões em especificação.

### Passo 2 — Especificação: Stack, Estrutura e Git Workflow _(concluído)_

**Stack definida**

- **Backend:** Java + Spring (Boot), build com **Gradle**.
- **Frontend:** TypeScript + React.
- **Testes unitários (backend):** JUnit — cobrindo principalmente o motor de precificação (Strategy Pattern).
- Justificativa fica pendente de detalhar no README (seção "Fundamentação Teórica" do critério de avaliação), mas alinhado ao pedido do desafio: tipagem forte, frameworks maduros para ambiente financeiro.

**Estrutura do repositório: Monorepo**

Um único repositório Git com backend e frontend lado a lado:

```
/backend    → Java / Spring / Gradle (API, regras de negócio, persistência)
/frontend   → TypeScript / React (Painel do Operador, Grid de Transações)
/docs       → Diagrama ER, DDL, diagrama C4, ADRs (conforme o nível Sênior avança)
README.md   → setup, arquitetura, decisões
```

Motivo: 1 repositório público (como pede a entrega), 1 README raiz, 1 pipeline de CI orquestrando os dois lados, histórico de commits correlacionando mudanças de API com mudanças de UI — sem a sobrecarga de gerenciar 2 repositórios pra um projeto solo.

**Git Workflow (nível Sênior)**

Acumulando os requisitos de Júnior + Pleno + Sênior do desafio:

- **Branching:** nunca commit direto na `main`. Branches de trabalho por tarefa: `feature/<nome>`, `fix/<nome>`, `docs/<nome>`, `chore/<nome>` (ex.: `feature/currency-engine`, `feature/pricing-strategy`).
  - _Nota (auditoria final, 2026-07-04): na prática o fluxo evoluiu pra trunk-based sobre `dev` — commits atômicos direto em `dev`, promoção pra `main` via PR (#1 a #6 até essa auditoria; 9 ao final do projeto) e `prod` reservada ao exercício de gestão de crise. Os prefixos `feature/*` planejados aqui nunca chegaram a ser usados; o plano original fica registrado por honestidade histórica, e a estratégia real (com a justificativa) está no README, seção "Git na prática"._
- **Conventional Commits obrigatório:** `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`, `ci:`, `build:`, `perf:`.
- **Pull Requests mesmo solo:** cada feature vira um PR pra `main` (via `gh pr create`), com descrição do que foi feito — simula revisão de time real.
- **Histórico limpo:** squash/rebase de commits pequenos de correção antes do merge — sem merge commits desnecessários poluindo o histórico.
- **Git Hooks:** configurados na raiz do monorepo (Husky), rodando lint/format nos arquivos alterados antes do commit, e testes relevantes (frontend e/ou backend, conforme o que mudou) antes do push.
- **Semantic Versioning via Tags:** tag `v1.0.0` marcando a entrega final; tags intermediárias opcionais em marcos relevantes.
- **Interactive Rebase:** usado antes de abrir/finalizar um PR pra organizar a sequência de commits (squash de fixups, reordenação lógica) mantendo linearidade profissional.

### Passo 3 — Modelo de Dados (Diagrama ER + DDL/Flyway) _(concluído)_

9 tabelas implementadas em 11 migrations Flyway (`backend/src/main/resources/db/migration/V1` a `V11`), validadas de ponta a ponta num Postgres real via Docker (todas as migrations aplicam limpo, e as constraints de integridade foram testadas na prática com `INSERT`s manuais). Diagrama ER completo, justificativa das decisões de tipo/precisão e gaps documentados em [`docs/diagrama-er.md`](./docs/diagrama-er.md).

Três decisões de escopo fechadas nesta etapa:
- **Spread fica só no código** (Strategy Pattern) — `tipo_recebivel` é catálogo puro (`codigo`, `nome`, `ativo`), sem coluna de spread. Evita duas fontes de verdade sobre a mesma regra de risco.
- **`lote_importacao` é persistido** — rastreia de qual submissão cada recebível veio, reforçando auditabilidade, mesmo a liquidação continuando por recebível (não por lote).
- **Sem ponteiro redundante** `recebivel.ultima_liquidacao_id` — a liquidação ativa de um recebível é derivada por query sob demanda (YAGNI); otimiza-se só se o profiling pedir.

Gaps documentados como escopo futuro (não implementados agora, ver `docs/diagrama-er.md` para detalhes): tabela `movimento_caixa` separada (ledger de efeito-no-caixa desacoplado do ledger de precificação), coluna de autoria (`criado_por` — não há sistema de usuário/auth ainda), mecanismo de aporte/capitalização de caixa, e particionamento de `liquidacao` por período (prematuro para o volume atual).

### Passo 4 — `docker-compose` (API + PostgreSQL + Prometheus + Grafana) _(concluído)_

`docker-compose.yml` na raiz sobe 4 serviços: `postgres` (17), `backend` (`backend/Dockerfile`, build multi-stage com Gradle), `prometheus` (`infra/prometheus/prometheus.yml` já configurado pra fazer scrape de `/actuator/prometheus`) e `grafana` (`infra/grafana/provisioning/` já provisiona o datasource do Prometheus automaticamente).

Validado de ponta a ponta, incluindo um ciclo completo `docker compose down -v` + `up -d` do zero (sem cache/estado anterior) pra garantir reprodutibilidade: as 11 migrations aplicam automaticamente no boot da API, `/actuator/health` responde `UP`, o Prometheus reporta o target `srm-credit-engine` como `up`, e o Grafana já enxerga o datasource do Prometheus sem configuração manual.

### Passo 5 — Camada de Aplicação (Controller / Service / Repository) _(concluído)_

Backend funcional de ponta a ponta: `domain` (9 entidades JPA + 2 enums, mapeadas 1:1 com o schema), `repository` (Spring Data JPA puro, sem SQL nativo), `pricing` (Strategy Pattern — `PricingStrategy` expõe só o spread, `MotorPrecificacao` compartilha a fórmula, usando `ch.obermuhlner:big-math` pra potência fracionária em `BigDecimal` sem nunca cair pra `double`), `service` (`LiquidacaoService` com transação por item + Optimistic Locking real via flush explícito, `LiquidacaoBatchService` orquestrando o lote através do proxy do Spring), `exception` (hierarquia de domínio + `GlobalExceptionHandler`), `controller` + `dto` (endpoints REST, 3 camadas), e `report` (Extrato de Liquidação, 2 camadas, SQL nativo via `JdbcTemplate`).

**Validado de ponta a ponta**, não só "deveria funcionar":
- 15 testes unitários do motor de precificação (incluindo casos de expoente fracionário com resultado exato verificável na mão, e cross-check contra `BigDecimal.pow(int)` nativo para expoentes inteiros).
- Rebuild completo do `docker-compose` do zero (`down -v` + `up -d --build`) com o schema real — `ddl-auto: validate` pegou 2 mismatches reais entre entidade e migration (tipo de coluna) antes de chegarem a produção.
- Fluxo feliz completo via API real: cadastro de cedente, taxas de câmbio/mercado, submissão de lote (mesma moeda e cross-currency), extrato paginado/filtrado, estorno com proteção contra duplo-estorno.
- **Concorrência real**: duas requisições `POST /api/recebiveis/lote` disparadas simultaneamente contra o mesmo caixa — uma sucede, a outra recebe `CONFLITO_CONCORRENCIA`, e o saldo final reflete exatamente 1 débito (validado via saldo antes/depois batendo com o valor da liquidação bem-sucedida). Teste de integração equivalente com Testcontainers foi escrito (`LiquidacaoConcorrenciaIT`), mas não roda neste ambiente de desenvolvimento específico por incompatibilidade entre o Docker Engine local (muito recente) e o probe de conexão do Testcontainers — validação manual supriu a lacuna.
- Bugs reais encontrados e corrigidos durante a verificação: `documento` duplicado de cedente vazava como 500 genérico (virou 409 `CEDENTE_DUPLICADO`), e `criado_em` voltava `null` nas respostas (Hibernate não relia colunas `insertable=false` após o INSERT — corrigido com `@Generated(event = INSERT)`).

**Próximo:** telas do frontend (Painel do Operador, Grid de Transações) consumindo essa API.

### Passo 6 — Frontend (Painel do Operador + Grid de Transações) _(concluído)_

**3 endpoints novos no backend, antes do frontend em si** — não havia forma de calcular o valor líquido em tempo real sem persistir:
- `POST /api/recebiveis/simular` — reaproveita os mesmos beans de `LiquidacaoService` (`PricingStrategyResolver`, `MotorPrecificacao`, `PrazoCalculator`, `TaxaMercadoService`, `CambioService`) num `SimulacaoService` novo, que deliberadamente não injeta nenhum repositório de escrita — a ausência na assinatura da classe é a garantia de que simular nunca tem efeito colateral (confirmado: `totalElements` do extrato não mudou depois de rodar a simulação).
- `GET /api/moedas` e `GET /api/tipos-recebivel` — catálogos que antes só existiam como seed, sem endpoint de listagem.
- Refactor no caminho: extraída `CambioService.buscarSeNecessario` (mesma moeda ⟹ sem conversão) pra não duplicar essa checagem entre `LiquidacaoService` e `SimulacaoService`.

**Frontend**: React + TypeScript + Vite + Tailwind CSS v4 (tema escuro, acento verde único — `canvas`/`surface`/`ink`/`brand` via `@theme`), `react-router-dom` (2 rotas), `@tanstack/react-query` (cache de servidor — resposta ao "gerenciamento de estado global, se necessário": sem Redux/Zustand, três fontes de verdade bem delimitadas — formulário no `react-hook-form`, filtros/paginação na URL, dados de servidor no `react-query`), `react-hook-form` + `zod`. Estrutura em `api/` (client tipado) → `domain/` (hooks de lógica/estado, sem JSX) → `components/ui/` (apresentação pura, nunca importa `api/`/`domain/`) → `features/*` (composição).

**Painel do Operador**: `usePainelOperadorForm` observa os campos que afetam preço (excluindo `cedenteId`, que não influencia o cálculo), aplica debounce de 450ms, e chama `/simular` só quando os campos passam na validação Zod local — exibindo o valor líquido calculado em tempo real antes de submeter o lote de verdade.

**Grid de Transações**: filtros e paginação vivem na URL (compartilhável, sobrevive a refresh, vira a `queryKey`). Conversão do "até" inclusivo do operador pro `dataFim` exclusivo que o backend espera (`criado_em < dataFim`) isolada num helper — evita excluir sistematicamente o último dia do período.

**Validado com Playwright** (headless, já que o ambiente não tem display): simulação atualizando em tempo real ao preencher o formulário, submissão do lote com sucesso, navegação pro Grid, zero erros de console. Um bug real foi pego pelo próprio screenshot, não pelo código: a coluna de "deságio %" comparava `valorFace`/`valorLiquido` em moedas diferentes numa linha cross-currency (BRL→USD), produzindo um percentual sem sentido (82%) — corrigido pra só calcular quando `moedaTitulo === moedaPagamento`, com teste cobrindo o caso.

**Próximo:** CI/CD (pipeline com testes + lint), e opcionalmente adicionar o frontend ao `docker-compose`.

### Passo 7 — CI/CD (GitHub Actions) + Frontend no `docker-compose` _(concluído)_

**Frontend containerizado**: `frontend/Dockerfile` multi-stage (`node:22-alpine` builda, `nginx:1.30-alpine` serve o resultado). O Nginx faz duas coisas — serve os arquivos estáticos com fallback de SPA (`try_files ... /index.html`, necessário pras rotas do `react-router` como `/transacoes`) e faz reverse proxy de `/api/*` pro container do backend pelo nome do serviço (`backend:8080`), replicando o `server.proxy` que o Vite já fazia em dev — nenhuma chamada `fetch('/api/...')` precisou mudar. `docker-compose.yml` ganhou o serviço `frontend` (porta `8081`), 5 containers no total agora.

**CI** (`.github/workflows/ci.yml`), 3 jobs:
- `backend`: `spotlessCheck` (lint, falha rápido) + `./gradlew build` (suíte completa, incluindo `LiquidacaoConcorrenciaIT` com Testcontainers — que **funciona** nos runners do GitHub, ao contrário deste ambiente de desenvolvimento local, cujo Docker Engine é novo demais pro probe do Testcontainers 1.21.x).
- `frontend`: `oxlint` + `build` + `vitest`.
- `docker-compose-smoke-test` (depende dos outros dois): sobe a stack completa via `docker compose up -d --build` e espera a API e o frontend responderem de verdade, não só que cada lado compila isolado.

Triggers: `push`/`pull_request` em `dev`/`main`/`prod` + disparo manual.

**Validado**: `docker compose up -d --build` local confirmando os 5 containers, `curl` provando que `http://localhost:8081/api/moedas` chega no backend através do proxy do Nginx, fallback de SPA respondendo `200` numa rota de cliente (`/transacoes`) direto na URL, e um screenshot Playwright do build de produção (idêntico ao modo dev, zero erros de console). YAML do workflow validado com `actionlint` (sem `act` disponível pra rodar Actions localmente) — zero avisos.

**Push feito e workflow rodou de verdade no GitHub**: `success` em 5m37s, os 3 jobs verdes — incluindo `LiquidacaoConcorrenciaIT` (Testcontainers), que confirma na prática que a limitação era mesmo só deste ambiente de desenvolvimento local, não do código (o Docker Engine dos runners do GitHub está na faixa de API que o Testcontainers espera).

### Passo 8 — Logs Estruturados (ECS) _(concluído)_

**Decisão ECS vs. GELF**: Spring Boot 3.5 suporta ambos nativamente via `logging.structured.format.console`, custo de implementação idêntico (config de 1 linha, zero dependência nova). Escolhido **ECS** (Elastic Common Schema): campos auto-descritivos (`log.level`, `service.name`) que leem bem cru, sem precisar de um coletor dedicado — GELF assume a existência de um Graylog (historicamente Elasticsearch/OpenSearch + MongoDB) que o projeto não roda e não planeja rodar; ECS, ao contrário, é o formato nativo tanto de um eventual stack Elastic quanto do **Grafana Loki** — a extensão natural do Grafana que o projeto já provisiona, caso a stack de observabilidade cresça.

**Escopo além da config**: ligar só o formato JSON estruturaria principalmente logs de framework (Spring/Hibernate/Flyway no boot) — o backend não tinha nenhum log de negócio até então (só 2 linhas em `GlobalExceptionHandler`). Adicionado:
- `CorrelationIdFilter` (`OncePerRequestFilter`, `@Order(HIGHEST_PRECEDENCE)`): popula `requestId` no MDC pra cada requisição (aproveitando `X-Request-Id` de entrada, se vier) — o formatter ECS promove chaves do MDC automaticamente pro JSON, correlacionando todas as linhas de uma mesma requisição sem precisar passar o id manualmente por camada. `GlobalExceptionHandler.handleInesperado` passou a ler esse id do MDC em vez de gerar um `UUID` solto só ali.
- Logs INFO estruturados (API fluente do SLF4J com `addKeyValue`, não só texto interpolado) no caminho feliz de `LiquidacaoService.liquidar`/`estornar` e no resumo de `LiquidacaoBatchService.processarLote` (totais do lote).
- Logs WARN em `GlobalExceptionHandler.handleConflito`/`handleRegraNegocio` — cobre saldo insuficiente, conflito de concorrência, estorno inválido, cedente duplicado etc. num único lugar, sem duplicar chamada de log em cada service.

**Validado de ponta a ponta** via `docker compose up -d --build` + chamadas reais de API: JSON ECS válido desde o boot (`@timestamp`, `log.level`, `service.name`, `ecs.version`), mesmo `requestId` correlacionando todas as linhas de uma mesma requisição (inclusive entre o erro do Hibernate e a tradução em `GlobalExceptionHandler`), `log.warn` disparando corretamente em `GET /api/taxas-mercado` sem taxa cadastrada (422) e em estorno inválido (409), `log.atInfo` de liquidação/lote com os campos de negócio (`recebivelId`, `valorLiquido`, `totalSucesso`) como chaves estruturadas de verdade, não só mensagem de texto.

### Passo 9 — Cobertura de testes completa (backend + frontend) _(concluído)_

Fecha os gaps de teste restantes mapeados no relatório de cobertura (ver seção "Cobertura de testes" abaixo, agora tudo marcado como feito):

- **Backend — `@WebMvcTest` pros 8 controllers** (`CambioController`, `CedenteController`, `LiquidacaoController`, `MoedaController`, `RecebivelController`, `TaxaMercadoController`, `TipoRecebivelController`, `ExtratoLiquidacaoController`): status HTTP, serialização dos DTOs e o mapeamento de exceção via `GlobalExceptionHandler` exercitados de verdade (não só smoke manual). Escrever esses testes achou um **bug real**: um `@RequestParam` obrigatório ausente (ex.: `GET /api/taxas-cambio` sem `moedaOrigem`) devolvia **500** em vez de 400 — o catch-all `@ExceptionHandler(Exception.class)` interceptava `MissingServletRequestParameterException`/`MethodArgumentTypeMismatchException` antes da resolução padrão do Spring MVC. Corrigido com um handler dedicado (`handleParametroInvalido`), coberto por teste em cada controller GET afetado.
- **Backend — `ExtratoLiquidacaoRepositoryIT`**: Testcontainers (mesmo padrão de `LiquidacaoConcorrenciaIT`, mesma limitação de Docker Engine neste ambiente local — roda no CI). Cobre a montagem dinâmica do WHERE (cedente, moeda, período, combinações, sem filtro nenhum — prova que nenhum parâmetro nulo quebra a query), paginação e ordenação. Validado manualmente contra o `docker-compose` real (mesmo cenário, mesmas asserções) já que o ambiente local não roda Testcontainers — e, como o teste só podia ser validado de verdade via CI, o push revelou **2 bugs reais no próprio teste**: falta de isolamento entre métodos (`@BeforeEach` compartilhado batendo na UNIQUE constraint do cedente — corrigido com `@Transactional` na classe) e uma liquidação "sumindo" da leitura por SQL puro porque o INSERT ficava pendente no persistence context do Hibernate dentro da transação de teste que nunca commita (corrigido com `entityManager.flush()` explícito). `testLogging { exceptionFormat = FULL }` foi adicionado ao Gradle pra esse diagnóstico ficar visível nos logs do GitHub Actions.
- **Frontend — `useExtratoLiquidacaoQuery`, `RecebivelForm`, `FiltrosTransacoes`, `PainelOperadorPage`, `GridTransacoesPage`**: hook restante + os 4 componentes de composição que faltavam. Escrever o teste de `FiltrosTransacoes` achou um **segundo bug real**: os campos de filtro (`Select`/`DateField`) não recebiam `name`/`id`, então o `<label>` nunca ficava associado ao controle via `for` — falha de acessibilidade (screen readers não conseguem ligar o rótulo ao campo) que só apareceu ao tentar `getByLabelText` no teste. Corrigido adicionando `name` a cada campo.

### Passo 10 — Diagrama C4 e Critérios de Aceite _(concluído)_

Fecha os dois últimos requisitos de documentação explícitos do desafio (seções 5 e 6 do `CLAUDE.md`):

- **[`docs/diagrama-c4.md`](../docs/diagrama-c4.md)**: Nível 1 (Contexto — operador, time de observabilidade, e a fonte de câmbio/mercado marcada explicitamente como mockada) e Nível 2 (Container — frontend, backend, Postgres, Prometheus, Grafana), em Mermaid (mesma convenção do diagrama ER), mapeando 1:1 com os serviços do `docker-compose.yml`.
- **[`docs/criterios-aceite.md`](../docs/criterios-aceite.md)**: critérios verificáveis de usabilidade, segurança, desempenho e escalabilidade, cada um com status (✅ atendido / ⚠️ mitigado / ❌ gap) e evidência (arquivo, teste ou decisão de domínio) — não uma lista de intenções. Documenta explicitamente os gaps que já existiam implicitamente (sem autenticação, sem TLS, imagens Docker rodando como root, sem teste de carga) em vez de deixá-los tácitos.

### Passo 11 — Primeiro release: `dev → main` + tag `v1.0.0` _(concluído)_

Primeira promoção de `dev` pra `main` desde o commit inicial (0 divergência além disso — fast-forward limpo, sem conflito possível): [PR #1](https://github.com/Narvaal/SRMCreditEngine/pull/1), 71 commits, CI verde tanto no PR quanto depois do merge direto em `main` (validado com `gh run watch` nos dois casos, não só assumido). Merge commit (não squash) — preserva a granularidade dos commits atômicos de `dev`.

Tag anotada `v1.0.0` criada sobre o commit de merge (`101365c`) e uma [GitHub Release](https://github.com/Narvaal/SRMCreditEngine/releases/tag/v1.0.0) publicada a partir dela, marcando a entrega completa do nível Sênior — critério de Git explícito do desafio (`CLAUDE.md`, seção 6: "Semantic Versioning via Tags").

### Passo 12 — Simulação de gestão de crise (`git cherry-pick`) _(concluído)_

Depois do primeiro release, ficou a dúvida de qual seria a utilidade real da branch `prod` — hoje não existe nenhum deploy automatizado atrás dela (o `ci.yml` só roda lint/teste/smoke-test em `push`, não deploya em lugar nenhum). Decisão: `prod` não simula um ambiente real, ela existe especificamente pra demonstrar o exercício de gestão de crise do nível Especialista (`CLAUDE.md`, seção 6: "bug crítico foi para a `main` → demonstrar `git revert` seguro, ou `git cherry-pick` simulando hotfix em produção") — um exercício de processo, documentado como tal, não uma pretensão de infraestrutura que não existe.

**O fix usado como veículo é real, não fabricado**: backend rodando como root nos containers, gap já documentado como `S9` em `docs/criterios-aceite.md`. Fluxo:

1. `prod` sincronizada com `main` (fast-forward, 0 divergência — igual tinha acontecido com `main` no Passo 11).
2. Fix implementado normalmente em `dev` (`backend/Dockerfile`: usuário `app` dedicado, não-root, no estágio de runtime) — validado de verdade: build da imagem, container rodando contra um Postgres real, `docker exec ... id` confirmando `uid=999(app)`, `/actuator/health` respondendo `UP` com as 11 migrations aplicadas.
3. [PR #2](https://github.com/Narvaal/SRMCreditEngine/pull/2) `dev → main`, CI verde, merge normal — o fix passa pelo fluxo de review de sempre antes de qualquer coisa.
4. `git cherry-pick` do commit específico do fix (`79d22f9`, não o merge commit) direto em `prod` — só esse commit, não o restante do que já estava em `main`/`dev` no momento, que é exatamente o ponto de usar cherry-pick em vez de merge aqui: um hotfix crítico não deveria carregar de carona outras mudanças ainda não validadas em produção.
5. Tag `v1.0.1` sobre o commit cherry-picked em `prod`, com [GitHub Release](https://github.com/Narvaal/SRMCreditEngine/releases/tag/v1.0.1) explicando o cenário.

CI verde em `prod` depois do cherry-pick, confirmando que o hotfix isolado builda e sobe sozinho (sem depender do resto de `main`).

### Passo 13 — Resiliência: retry + circuit breaker (Resilience4j) _(concluído)_

Fecha o último requisito explícito do nível Sênior. O problema registrado na pendência era a falta de "alvo": nenhuma chamada HTTP externa existia pra proteger. Solução: uma integração externa **simulada mas com HTTP de verdade** — `MockFxProviderController` (`/mock/fx-provider`, fora do `/api` de propósito) faz o papel do BACEN/B3/FX provider, com knob de falha ajustável em runtime (`PUT /config?failureRate=`) e contador de chamadas (`GET /stats`) que permite asserção determinística de que o retry aconteceu.

**Decisões:**
- **Mock interno, não WireMock/container novo**: funciona igual no `bootRun`, no compose e no CI, zero orquestração extra — o custo de um 6º container não compra nada aqui, já que o ponto é proteger uma chamada HTTP, não simular rede entre containers.
- **Tradução de falha no service, não `fallbackMethod` via AOP**: `FxProviderClient` tem só `@Retry` + `@CircuitBreaker` (retry por fora, cada tentativa conta na janela do circuito; `CallNotPermittedException` no ignore-list do retry ⇒ circuito aberto falha rápido). Quem traduz pra `ProviderIndisponivelException` (novo 503) é `SincronizacaoTaxasService`, com try/catch explícito — mesmo padrão de tradução do resto do projeto (Optimistic Locking, documento duplicado).
- **Fallback com semântica de negócio real, não valor inventado**: provider fora ⇒ só a *atualização* de taxas degrada; liquidação e simulação seguem com a última taxa vigente persistida — exatamente o que o histórico append-only de `taxa_cambio`/`taxa_mercado` já garante. Resiliência de verdade, não anotação decorativa.
- **Sync deliberadamente não-atômico entre as 4 taxas**: cada cotação é uma observação independente no histórico; prender a conexão de banco durante retry/backoff HTTP seria pior que um sync parcial (documentado no javadoc do service).
- **Trigger duplo**: `POST /api/taxas/sincronizar` (demoável via Swagger) + `@Scheduled` default off (`FX_PROVIDER_SYNC_ENABLED`), ligado no `docker-compose` pra manter o circuito ciclando e visível nas métricas.

**Validado nos dois níveis**: `FxProviderResilienceIT` (Testcontainers + `DEFINED_PORT`, roda no CI — verde na primeira execução) prova retry (3 chamadas HTTP no contador), short-circuit (0 chamadas com circuito aberto) e degradação graciosa (liquidação com a última taxa persistida enquanto o provider está fora). Manualmente contra o `docker-compose`: ciclo completo **closed → open → half-open → closed** observado em `resilience4j_circuitbreaker_state` no `/actuator/prometheus`, incluindo o detalhe de que o `half_open` exige 2 chamadas boas (`permitted-number-of-calls-in-half-open-state`) antes de fechar.

### Passo 14 — Auditoria final contra o enunciado _(concluído)_

Leitura sistemática do projeto inteiro contra o `CLAUDE.md`, em 5 dimensões (conformidade acumulativa Júnior→Pleno→Sênior, excessos de nível Especialista, consistência docs↔repo, higiene de código, critérios de avaliação da seção 8). **Conformidade: 100% dos itens acumulativos com evidência concreta.** Achados corrigidos:

- **P0 — Fundamentação teórica da stack não existia** (critério de avaliação 8.1, prometida no Passo 2 deste roadmap e nunca escrita): adicionada ao README, com cada escolha amarrada ao domínio financeiro em vez de preferência genérica.
- **P1 — Pre-push hook permanentemente inutilizado**: rodava a suíte completa incluindo os 3 `*IT` que sempre falham neste ambiente (Testcontainers), forçando `--no-verify` em todo push — o que pulava também os testes de frontend. Corrigido: o hook roda só `--tests '*Test'` (unit/slice); os `*IT` continuam no CI. Hook voltou a funcionar de verdade.
- **P1 — Este roadmap prometia branches `feature/*` que nunca foram usadas**: o fluxo real evoluiu pra trunk-based sobre `dev` com promoção via PR — nota de honestidade adicionada ao Passo 2, apontando pra estratégia real no README.
- **P1 — Contagens de teste desatualizadas** (99→111 unit/slice, 2→3 ITs) e **badge de release preso na tag `v1.0.0`** (→ `/releases/latest`).
- **P2 — Escopo Especialista implícito**: nota explícita no README dizendo o que ficou deliberadamente de fora (ADRs, 1M tx/min, IaC, EDA) e por que as duas exceções de Git entraram.

Higiene: zero TODOs/código de debug, `.gitignore` completo, sem dependências mortas.

### Passo 15 — Nice-to-haves: UI de estorno, cadastro de cedente inline e frontend não-root _(concluído)_

Três polimentos pedidos explicitamente pelo usuário (fica de fora só o dashboard do Grafana):

- **UI de estorno na Grid**: antes, o backend ganhou uma flag `estornada` no extrato (subquery `EXISTS` sobre `liquidacao_estornada_id`) — sem ela a UI ofereceria estorno em linha já estornada e o operador só descobriria no 409. Na Grid: coluna "Ações" com botão "Estornar" só em `LIQUIDACAO` não-estornada, **confirmação inline em dois cliques** (sem `window.confirm` — testável e no tema), badge "Estornada" na linha original após o estorno, e erro da API em alerta (o 409 de corrida entre dois operadores continua possível e é exibido com a mensagem do backend). `TransacoesTable` continua puramente apresentacional — a mutação (`useEstornarLiquidacao`, que invalida o extrato) chega via props da página.
- **Cadastro de cedente inline no Painel**: `CadastroCedenteInline` expande sob o select (via prop-slot `cadastroCedenteSlot` — `RecebivelForm` continua sem lógica), reusa o `cedentesApi.criar` que já existia, invalida o catálogo e auto-seleciona o cedente novo. **Bug real pego pelo teste de composição**: `setValue` direto no `onCriado` roda antes de a `<option>` nova existir no DOM (o refetch do catálogo ainda não renderizou) e deixa o select vazio — corrigido com um efeito condicionado à lista re-buscada, determinístico no teste e no navegador.
- **Frontend não-root**: imagem oficial `nginxinc/nginx-unprivileged` (uid 101, porta 8080 — <1024 exigiria root), `nginx.conf` escutando 8080 e compose mapeando `8081:8080` — externamente nada muda. Fecha o `S9` de `docs/criterios-aceite.md` por completo (backend já era não-root desde `v1.0.1`).

**Validado de ponta a ponta com Playwright** contra o `docker-compose` real: cadastro inline → auto-seleção → simulação → liquidação → estorno via UI com confirmação → badge "Estornada" + linha `ESTORNO` nova, zero erros de console; `docker exec srm-frontend id` → `uid=101(nginx)`; e um detalhe satisfatório — as taxas da simulação vieram do provider mockado via scheduler (Passo 13), ou seja, a stack agora se auto-alimenta de taxas sem nenhum `POST` manual.

### Passo 16 — Hardening do Painel e Grid de estado final _(concluído, pós-v1.2.0)_

Duas frentes de refinamento guiadas por sessões de uso real da UI (cada achado do usuário virou iteração):

- **Hardening do Painel do Operador**: máscara + validação de CPF/CNPJ no cadastro de cedente (com validação espelhada no backend — documento e nome validados na borda da API), separador de milhar automático no valor de face (corrigindo o input `number` nativo que engolia a vírgula silenciosamente), limites de sanidade (valor de face ≤ 1 quatrilhão, vencimento ≤ 100 anos, casas decimais validadas), datas passadas rejeitadas inline e mensagens de erro padronizadas no modo imperativo.
- **Estorno com modal + toast**: a confirmação em dois cliques (Passo 15) virou modal com os detalhes completos da transação; confirmar fecha o modal na hora e o resultado assíncrono (sucesso ou 409 de corrida) chega num `Toast` novo do design system (`role="status"`, auto-dismiss), com todos os botões de estorno travados enquanto a mutação está em voo. De quebra, um bug real do design system: `Th` sobrescrevia o className base em vez de mesclar — cabeçalhos `text-right` renderizavam sem padding.
- **Grid de estado final**: cada operação aparece uma única vez, no seu estado final. O extrato passou a expor a referência exata do estorno (`liquidacao_estornada_id` + data, LEFT JOIN na própria tabela) e a **excluir liquidações já estornadas no SQL** (`NOT EXISTS` — paginação continua exata); a flag `estornada` saiu da cadeia inteira (nunca mais seria verdadeira). A linha do estorno representa a operação e expande a **operação de origem** logo abaixo ("Ver origem"/"Recolher"): mesma estrutura de 8 colunas da grid, badge âmbar "Origem" (novo tom `warning` no design system), texto e fundo rebaixados pra hierarquia visual, seta `↳` marcando o vínculo. Filtro por tipo (Liquidação/Estorno) no backend + select na grid persistido na URL. A taxa em cross-currency é calculada sobre o `valor_presente` (mesma moeda do título) — matematicamente válida pra qualquer par de moedas, coluna 100% preenchida.
- **Identidade visual**: logo SRM como favicon (PNG 256px substituindo o `favicon.svg` genérico).

Iteração digna de nota: a "operação de origem" passou por 3 rodadas de feedback (heurística de pareamento por recebível → referência exata do backend; card → linha alinhada às colunas; timeline com cotovelo animado → seta estática depois que a animação de slide pareceu estranha na prática) — o custo de errar pequeno e corrigir rápido foi menor que o de acertar de primeira. Contagens atualizadas: **131 testes de unidade/slice no backend** e **121 no frontend** (21 arquivos), todos verdes; validação Playwright contra o `docker-compose` real a cada rodada.

### Passo 17 — Dashboard do Grafana com painéis reais _(concluído — fecha o último nice-to-have)_

Três camadas numa entrega só, porque um dashboard sem métrica de domínio seria só um dashboard de JVM:

- **Métricas de negócio no backend**: `MetricasNegocio` (component fino sobre `MeterRegistry`) expõe `srm_liquidacoes_total`/`srm_estornos_total` e `srm_liquidacoes_valor_total`/`srm_estornos_valor_total`, com tag `moeda` (de pagamento), incrementados no fim dos métodos transacionais de `LiquidacaoService`. Decisão consciente documentada no código: o valor monetário vira `double` porque métrica é telemetria — a verdade contábil segue no ledger em `BigDecimal`. Histogramas de `http.server.requests` ligados no `application.yml` pra viabilizar p95 real. Testes: `SimpleMeterRegistry` real no `LiquidacaoServiceTest` — contadores conferidos no sucesso e zerados nos caminhos de falha.
- **Dashboard como código**: provider (`dashboards.yaml`) + `srm-credit-engine.json` em `infra/grafana/provisioning/dashboards/`, carregados no boot; datasource ganhou uid fixo (`prometheus`) pro JSON ser determinístico. 4 linhas: Negócio, API HTTP (RPS/p95/erros por rota), Resiliência (estado do circuit breaker como stat + state-timeline via expressão `closed*1 + half_open*2 + open*3` com value mappings, failure rate, retries) e Runtime (heap, GC, CPU, Hikari). `allowUiUpdates: false` — dashboard é código, edição via export+commit.
- **CI**: o smoke test do compose passou a validar Prometheus (target `up`), Grafana (`/api/health`) e o dashboard provisionado respondendo na API — provisionamento quebrado agora falha o pipeline.

Bug real pego na verificação com dados de verdade: os stats de negócio usavam `increase()` por janela e mostravam **0** com contadores recém-nascidos (a série nasce já em 1 no primeiro incremento — não há delta na janela). Corrigido pro total acumulado (`sum(...)`), com o racional na descrição do painel. Verificado de ponta a ponta: stack do zero (`down -v`), tráfego real via API (cedente → lote de 2 → estorno), métricas conferidas no Prometheus (valores batendo com a resposta da API: 956.35 BRL / 855.30 USD) e screenshot do dashboard com todos os painéis populados.

---

## Pendências (histórico — todas fechadas)

Levantamento original feito ao final do dia 2026-07-03, revisando `CLAUDE.md` (enunciado) contra o que já foi entregue; atualizado conforme os itens abaixo foram fechados (o último em 2026-07-06). Nada aqui é urgente — a aplicação já é funcional de ponta a ponta (backend + frontend + CI/CD) — mas fica registrado pra não perder o fio.

### Requisitos explícitos do desafio ainda em aberto (nível Sênior)

Nenhum — todos os requisitos explícitos do nível Sênior foram fechados (o último, Resiliência, no Passo 13).

- **Interactive Rebase**: ~~usamos Conventional Commits em commits atômicos o tempo todo, mas ainda não demonstramos organizar/squashar commits via `git rebase -i` antes de um merge~~ — **feito**: os 7 commits de teste do backend (ver "Cobertura de testes" abaixo) foram commitados fora de ordem de propósito e reorganizados via `git rebase -i` numa sequência lógica (núcleo transacional → services de domínio → cross-cutting) antes do push.
- **Resiliência (retry/circuit breaker)**: ~~o sistema hoje não tem nenhuma chamada HTTP externa de verdade pra proteger~~ — **feito** (Passo 13): integração simulada com HTTP real + Resilience4j, validada no CI e manualmente com o ciclo completo do circuito.

### Cobertura de testes _(concluído — ver Passo 9)_

- **Backend**: services de negócio (`LiquidacaoService`, `LiquidacaoBatchService`, `CambioService`, `TaxaMercadoService`, `RecebivelService`, `CedenteService`, `SincronizacaoTaxasService`), `GlobalExceptionHandler`, os 9 controllers REST (`@WebMvcTest`), `ExtratoLiquidacaoRepository` e a camada de resiliência — todos com teste dedicado. 132 testes de unidade/slice verdes localmente (contagem pós-Passo 17) + 3 classes de teste de integração com Testcontainers (concorrência, relatório e resiliência — 11 cenários) que rodam no CI mas não neste ambiente de desenvolvimento local (limitação de Docker Engine já documentada).
- **Frontend**: os dois hooks orquestradores (`usePainelOperadorForm`, `useExtratoFiltrosUrlState`), `useExtratoLiquidacaoQuery` e os 4 componentes de composição (`RecebivelForm`, `FiltrosTransacoes`, `PainelOperadorPage`, `GridTransacoesPage`), além dos hooks/componentes de estorno, cadastro inline e da visão de estado final (Passos 15–16) — 121 testes verdes em 21 arquivos.

### Nice-to-have / polish

- ~~Dashboard do Grafana com painéis reais~~ — **feito** (Passo 17). **Nenhum nice-to-have restante.**
- ~~UI de estorno no Grid de Transações~~ — **feito** (Passo 15).
- ~~Cadastro de cedente direto no Painel do Operador~~ — **feito** (Passo 15).
- ~~Rodar o frontend como não-root também~~ — **feito** (Passo 15; `S9` fechado por completo).

### Recapitulando o que já está pronto

Passos 1–17 concluídos: entendimento do domínio → stack/estrutura/git workflow → modelo de dados (ER+DDL) → `docker-compose` (Postgres+Prometheus+Grafana) → camada de aplicação completa (Strategy Pattern, Optimistic Locking, exceções, relatório 2 camadas) → frontend (Painel do Operador com simulação em tempo real + Grid de Transações) → CI/CD (GitHub Actions, 3 jobs) + frontend containerizado no compose → logs estruturados (ECS) com correlation id por requisição → cobertura de testes completa (backend + frontend) → diagrama C4 e critérios de aceite documentados → primeiro release (`dev → main`, tag `v1.0.0`) → simulação de gestão de crise (`git cherry-pick` de hotfix pra `prod`, tag `v1.0.1`) → resiliência com Resilience4j (retry + circuit breaker sobre integração externa simulada, tag `v1.1.0`) → auditoria final contra o enunciado (fundamentação teórica da stack, hook de pre-push funcional de novo, consistência docs↔repo) → nice-to-haves de UX e hardening (estorno pela UI, cadastro de cedente inline, frontend não-root — tag `v1.2.0`) → hardening do Painel (máscaras/validações) e Grid de estado final (estorno com modal+toast, operação de origem expansível, filtro por tipo, taxa cross-currency, favicon SRM — tag `v1.3.0`) → dashboard do Grafana provisionado como código, com métricas de negócio instrumentadas e validação no CI, mais os cenários de demonstração e o README de apresentação (tag `v1.4.0`). **Todos os requisitos explícitos do nível Sênior e todos os nice-to-haves estão fechados.** Aplicação sobe com 1 comando (`docker compose up -d --build`), 5 containers, testada de ponta a ponta manualmente e via CI real no GitHub.
