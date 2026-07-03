# Diagrama ER — SRM Credit Engine

Modelo de dados implementado nas migrations Flyway em [`backend/src/main/resources/db/migration`](../backend/src/main/resources/db/migration). Reflete as decisões de domínio registradas em [`ROADMAP.md`](../ROADMAP.md).

## Diagrama

```mermaid
erDiagram
    MOEDA {
        varchar_3 codigo PK
        varchar nome
        smallint casas_decimais
    }

    TIPO_RECEBIVEL {
        varchar codigo PK
        varchar nome
        boolean ativo
    }

    CEDENTE {
        uuid id PK
        varchar nome
        varchar documento UK
    }

    LOTE_IMPORTACAO {
        uuid id PK
        varchar origem
    }

    TAXA_CAMBIO {
        uuid id PK
        varchar_3 moeda_origem FK
        varchar_3 moeda_destino FK
        numeric valor
        timestamptz vigente_em
    }

    TAXA_MERCADO {
        uuid id PK
        varchar_3 moeda_codigo FK
        varchar indicador
        numeric valor
        timestamptz vigente_em
    }

    RECEBIVEL {
        uuid id PK
        uuid cedente_id FK
        varchar tipo_recebivel_codigo FK
        uuid lote_importacao_id FK
        numeric valor_face
        varchar_3 moeda_titulo FK
        date data_vencimento
        varchar status
        bigint version
    }

    CAIXA {
        varchar_3 moeda_codigo PK_FK
        numeric saldo
        bigint version
    }

    LIQUIDACAO {
        uuid id PK
        uuid recebivel_id FK
        uuid cedente_id FK
        varchar tipo
        uuid liquidacao_estornada_id FK
        numeric valor_face
        varchar_3 moeda_titulo FK
        numeric taxa_base_usada
        uuid taxa_base_ref_id FK
        numeric spread_usado
        numeric prazo_meses_usado
        numeric valor_presente
        varchar_3 moeda_pagamento FK
        numeric taxa_cambio_usada
        uuid taxa_cambio_ref_id FK
        numeric valor_liquido
    }

    MOEDA ||--o{ TAXA_CAMBIO : "origem/destino"
    MOEDA ||--o{ TAXA_MERCADO : ""
    MOEDA ||--o{ RECEBIVEL : "moeda_titulo"
    MOEDA ||--|| CAIXA : ""
    MOEDA ||--o{ LIQUIDACAO : "titulo/pagamento"
    TIPO_RECEBIVEL ||--o{ RECEBIVEL : ""
    CEDENTE ||--o{ RECEBIVEL : ""
    CEDENTE ||--o{ LIQUIDACAO : "denormalizado"
    LOTE_IMPORTACAO ||--o{ RECEBIVEL : ""
    RECEBIVEL ||--o{ LIQUIDACAO : ""
    TAXA_MERCADO ||--o{ LIQUIDACAO : ""
    TAXA_CAMBIO ||--o{ LIQUIDACAO : ""
    LIQUIDACAO ||--o| LIQUIDACAO : "estorno de"
```

## Decisões de tipo (por que cada `NUMERIC` tem a precisão que tem)

Nunca `float`/`double` em nenhum campo monetário ou de taxa — sempre `NUMERIC` (decimal exato), tanto no banco quanto em `BigDecimal` na aplicação. `NUMERIC` sem `(precision, scale)` foi evitado deliberadamente: sem escala fixa, nada impede gravar um valor com casas decimais inconsistentes num sistema financeiro.

| Família | Onde aparece | Tipo | Por quê |
|---|---|---|---|
| Valor monetário final | `valor_face`, `valor_liquido`, `caixa.saldo` | `NUMERIC(18,2)` | 2 casas fixas cobre BRL/USD. Limitação conhecida: não escala para moedas com 0 casas (JPY) ou 3 (BHD) — ver `moeda.casas_decimais` abaixo. |
| Valor intermediário de cálculo | `valor_presente` | `NUMERIC(20,6)` | Mais casas que o valor final, para não perder precisão antes do arredondamento definitivo em `valor_liquido`. |
| Taxas percentuais (base, spread) | `taxa_base_usada`, `spread_usado`, `taxa_mercado.valor` | `NUMERIC(9,6)` | Armazenado como fração decimal (`0.015000` = 1,5% a.m.), granularidade suficiente para taxas de mercado. |
| Taxa de câmbio | `taxa_cambio.valor`, `taxa_cambio_usada` | `NUMERIC(19,8)` | Mercado de câmbio usa tipicamente mais casas decimais que dinheiro "de verdade". |
| Prazo (fracionário) | `prazo_meses_usado` | `NUMERIC(9,4)` | Prazo = dias/30 não é inteiro (45 dias = 1,5 mês) — nunca `INTEGER`, senão o VP fica sistematicamente errado para vencimentos fora de múltiplos exatos de 30 dias. |

`moeda.casas_decimais` documenta a premissa assumida (BRL/USD = 2 casas) sem resolver escala dinâmica agora — é um limite conhecido, não uma lacuna esquecida.

## Decisões de modelagem

- **`tipo_recebivel` é catálogo puro** (sem coluna de spread): o valor do spread mora exclusivamente na implementação do Strategy Pattern no código. O banco só referencia o `codigo` usado pelo Strategy correspondente — uma única fonte de verdade para a regra de risco.
- **`taxa_cambio` e `taxa_mercado` são append-only**: nunca `UPDATE`/`DELETE`; uma nova cotação é sempre uma linha nova, preservando histórico para auditoria ("qual taxa valia no dia X?").
- **`liquidacao` é o ledger append-only** do sistema: nunca editado/apagado. Correção de um erro gera uma linha `ESTORNO` (referenciando a original via `liquidacao_estornada_id`) e, se necessário, uma nova `LIQUIDACAO` corretiva — nunca reescreve o passado.
- **`recebivel.status` (3 estados) + `version`** e **`caixa.saldo` + `version`** são o **estado corrente projetado**, atualizado atomicamente junto com o insert em `liquidacao`, na mesma transação SQL. Isso é complementar ao ledger imutável, não uma contradição: sem uma linha física mutável para travar/versionar, não há como impedir duas liquidações concorrentes de operarem sobre o mesmo recebível/caixa (a race condition que o Optimistic Locking existe para prevenir).
- **`liquidacao` guarda os insumos do cálculo como snapshot** (`taxa_base_usada`, `spread_usado`, `prazo_meses_usado`, `taxa_cambio_usada`) — valor direto (leitura sem join) **e** FK para a fonte (`taxa_base_ref_id`, `taxa_cambio_ref_id`) para rastreabilidade completa.
- **`cedente_id` é denormalizado em `liquidacao`** (redundante com `recebivel.cedente_id`) para permitir que a consulta de Extrato de Liquidação filtre por cedente sem join, otimizado com índice `INCLUDE` (covering index).
- **`lote_importacao` existe só para rastreabilidade** — a liquidação em si é sempre por recebível, nunca por lote (um item do lote pode falhar sem afetar os demais).

## Gaps conhecidos (fora de escopo por ora, não esquecidos)

- **`movimento_caixa` separado**: hoje `liquidacao` acumula a função de "o que foi calculado" e, implicitamente, "o efeito no caixa" (interpretado via `tipo`). Uma tabela dedicada de movimentos de caixa (delta assinado, referenciando `liquidacao`) daria um invariante de reconciliação clássico (`SUM(movimento_caixa.valor) = caixa.saldo`), mas foi deixada como extensão futura para manter o escopo atual enxuto.
- **Sem autoria (`criado_por`)**: nenhuma tabela transacional guarda quem executou a ação, porque ainda não existe modelo de usuário/autenticação no projeto.
- **Sem mecanismo de aporte de caixa**: o saldo inicial em `caixa` (seed) é um artefato de teste/demo, não uma regra de negócio real — não existe ainda um evento de capitalização do fundo.
- **Particionamento de `liquidacao`**: para o volume atual, índices B-tree bem desenhados são suficientes. Se o volume crescer (regime de "1 milhão de transações/minuto"), particionar por `RANGE (criado_em)` é o caminho natural — decidir isso cedo é importante porque converter uma tabela populada em particionada depois é uma migração custosa, mas implementar agora seria otimização prematura.
