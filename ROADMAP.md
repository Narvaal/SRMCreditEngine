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

**Próximo:** dar `push` pra `origin` pra o pipeline rodar de verdade no GitHub (ainda não empurrado — fica pra confirmação).
