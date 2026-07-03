package com.srmasset.creditengine.report;

import com.srmasset.creditengine.dto.PaginaResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Caminho de <b>2 camadas</b> (Controller → Repository, sem Service): leitura analítica sobre
 * grande volume, otimizada com SQL nativo em vez de JPA/ORM, aproveitando diretamente o índice
 * {@code idx_liquidacao_cedente_periodo} (covering index em {@code cedente_id, criado_em}).
 *
 * <p>O WHERE é montado dinamicamente — só inclui os predicados dos filtros realmente informados —
 * em vez do padrão {@code (:param IS NULL OR coluna = :param)}, que além de gerar um plano de
 * consulta pior, é uma armadilha conhecida do driver JDBC do Postgres para parâmetros nulos em
 * colunas tipadas (ex: {@code uuid}): o driver não consegue inferir o tipo do parâmetro nulo e a
 * query falha com "could not determine data type of parameter".
 */
@Repository
public class ExtratoLiquidacaoRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public ExtratoLiquidacaoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PaginaResponse<ExtratoLiquidacaoLinha> buscar(ExtratoLiquidacaoFiltro filtro) {
    StringBuilder where = new StringBuilder(" where 1 = 1 ");
    MapSqlParameterSource params = new MapSqlParameterSource();

    if (filtro.cedenteId() != null) {
      where.append(" and l.cedente_id = :cedenteId ");
      params.addValue("cedenteId", filtro.cedenteId());
    }
    if (filtro.moeda() != null) {
      where.append(" and l.moeda_pagamento = :moeda ");
      params.addValue("moeda", filtro.moeda());
    }
    if (filtro.dataInicio() != null) {
      where.append(" and l.criado_em >= :dataInicio ");
      params.addValue("dataInicio", Timestamp.from(filtro.dataInicio()));
    }
    if (filtro.dataFim() != null) {
      where.append(" and l.criado_em < :dataFim ");
      params.addValue("dataFim", Timestamp.from(filtro.dataFim()));
    }

    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from liquidacao l" + where, params, Long.class);

    int size = filtro.size();
    int offset = filtro.page() * size;
    params.addValue("limit", size);
    params.addValue("offset", offset);

    String sql =
        """
        select l.id, l.recebivel_id, l.cedente_id, c.nome as cedente_nome, l.tipo,
               l.moeda_titulo, l.moeda_pagamento, l.valor_face, l.valor_liquido, l.criado_em
        from liquidacao l
        join cedente c on c.id = l.cedente_id
        """
            + where
            + """
        order by l.criado_em desc
        limit :limit offset :offset
        """;

    List<ExtratoLiquidacaoLinha> linhas =
        jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) ->
                new ExtratoLiquidacaoLinha(
                    (UUID) rs.getObject("id"),
                    (UUID) rs.getObject("recebivel_id"),
                    (UUID) rs.getObject("cedente_id"),
                    rs.getString("cedente_nome"),
                    rs.getString("tipo"),
                    rs.getString("moeda_titulo"),
                    rs.getString("moeda_pagamento"),
                    rs.getBigDecimal("valor_face"),
                    rs.getBigDecimal("valor_liquido"),
                    rs.getTimestamp("criado_em").toInstant()));

    return PaginaResponse.de(linhas, filtro.page(), size, total == null ? 0 : total);
  }
}
