# Migrations (Flyway)

Scripts DDL versionados do banco de dados, seguindo a convenção do Flyway: `V<versão>__<descrição>.sql`.

Ordem de dependências: extensões → catálogos sem dependência (`moeda`, `tipo_recebivel`, `cedente`) → tabelas que dependem de catálogo (`taxa_cambio`, `taxa_mercado`, `lote_importacao`) → `recebivel` → `caixa` → `liquidacao` (a mais dependente) → seed de dados de referência.

Ver [`docs/diagrama-er.md`](../../../../../../../docs/diagrama-er.md) na raiz do repositório para o diagrama ER completo e a justificativa das decisões de tipo/constraint.
