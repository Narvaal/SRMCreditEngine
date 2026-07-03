package com.srmasset.creditengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ledger append-only: nunca UPDATE/DELETE. Correção de erro = nova linha ESTORNO referenciando a
 * original ({@link #liquidacaoEstornada}), nunca reescrita do passado.
 *
 * <p>Guarda os insumos do cálculo como snapshot — valor direto (leitura sem join) e FK para a fonte
 * ({@link #taxaBaseRef}, {@link #taxaCambioRef}) para rastreabilidade completa.
 */
@Entity
@Table(name = "liquidacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Liquidacao {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recebivel_id", nullable = false)
  private Recebivel recebivel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cedente_id", nullable = false)
  private Cedente cedente;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 20)
  private TipoLiquidacao tipo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "liquidacao_estornada_id")
  private Liquidacao liquidacaoEstornada;

  @Column(name = "valor_face", nullable = false, precision = 18, scale = 2)
  private BigDecimal valorFace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moeda_titulo", nullable = false)
  private Moeda moedaTitulo;

  @Column(name = "taxa_base_usada", nullable = false, precision = 9, scale = 6)
  private BigDecimal taxaBaseUsada;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "taxa_base_ref_id", nullable = false)
  private TaxaMercado taxaBaseRef;

  @Column(name = "spread_usado", nullable = false, precision = 9, scale = 6)
  private BigDecimal spreadUsado;

  @Column(name = "prazo_meses_usado", nullable = false, precision = 9, scale = 4)
  private BigDecimal prazoMesesUsado;

  @Column(name = "valor_presente", nullable = false, precision = 20, scale = 6)
  private BigDecimal valorPresente;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moeda_pagamento", nullable = false)
  private Moeda moedaPagamento;

  @Column(name = "taxa_cambio_usada", precision = 19, scale = 8)
  private BigDecimal taxaCambioUsada;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "taxa_cambio_ref_id")
  private TaxaCambio taxaCambioRef;

  @Column(name = "valor_liquido", nullable = false, precision = 18, scale = 2)
  private BigDecimal valorLiquido;

  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;
}
