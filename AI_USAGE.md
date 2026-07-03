# AI_USAGE.md

Registro do uso de IA (Claude Code) no desenvolvimento do SRM Credit Engine, conforme a política de uso de IA do desafio (ver `CLAUDE.md`, seção 2). Este documento é atualizado incrementalmente ao longo do desenvolvimento, não apenas ao final.

## Ferramenta utilizada

- **Claude Code** (Anthropic), modelo Sonnet 5 — sessão via CLI.

## Prompts estratégicos utilizados

| Data | Prompt (resumo) | Objetivo | Resultado |
|------|------------------|----------|-----------|
| 2026-07-03 | "escreva um claude md com essas informações [enunciado completo do desafio]" | Registrar o enunciado do desafio como contexto persistente do projeto, para orientar sessões futuras de desenvolvimento assistido por IA | `CLAUDE.md` criado com o enunciado completo estruturado por seção, mais notas práticas de orientação (stack, precisão decimal, workflow de Git por senioridade) |
| 2026-07-03 | "crie tmb o AI_USAGE.md" | Criar o arquivo de rastreabilidade de uso de IA exigido pelo desafio | Este arquivo |
| 2026-07-03 | Sequência de perguntas pedindo pra explicar o problema de negócio "da forma mais simples possível" (fluxo de deságio, ordem da conversão de moeda, o que é um lote de recebíveis) | Construir entendimento do domínio antes de tomar qualquer decisão técnica (metodologia SDD) | Explicações em linguagem simples com exemplos numéricos concretos, sem gerar código — usadas como base para as decisões registradas em seguida |
| 2026-07-03 | Fechamento guiado das perguntas em aberto do `ROADMAP.md` (granularidade da liquidação por lote vs. por item; origem da Taxa Base — mercado vs. interna; convenção de prazo em dias vs. meses; existência de estorno/cancelamento; controle de saldo de caixa por moeda) | Eliminar as ambiguidades de negócio do enunciado antes de especificar modelo de dados e API | 5 decisões de domínio registradas no `ROADMAP.md`, cada uma com a justificativa de negócio por trás: (1) liquidação independente por recebível, não por lote; (2) Taxa Base é taxa de mercado (CDI/SOFR) com histórico, spread é política interna da SRM; (3) prazo em meses, mês fixo = 30 dias, convenção simplificada e assumida conscientemente; (4) histórico append-only, correções via estorno, nunca reescrita do passado; (5) caixa com saldo controlado por moeda, habilitando o cenário real de Optimistic Locking do nível Sênior |

> Novas entradas devem ser adicionadas à tabela conforme prompts relevantes forem usados (scaffolding, geração de massa de dados, refatoração de queries, geração de testes, etc.).

## Trechos onde a IA alucinou ou gerou código inseguro

_Nenhum código de aplicação foi gerado até o momento — o repositório contém apenas documentação inicial. Esta seção será preenchida assim que houver implementação, com trecho, causa da falha e correção aplicada._

## Análise crítica

_A ser preenchida ao longo do desenvolvimento. Pontos a cobrir:_
- Onde a IA economizou tempo (ex.: boilerplate, scaffolding, documentação, testes repetitivos).
- Onde a IA atrapalhou ou exigiu retrabalho (ex.: sugestões incorretas para precisão decimal/monetária, padrões inadequados a contexto financeiro, over-engineering).
- Decisões de arquitetura e de negócio que foram deliberadamente feitas pelo desenvolvedor, e não apenas aceitas de sugestões da IA.
