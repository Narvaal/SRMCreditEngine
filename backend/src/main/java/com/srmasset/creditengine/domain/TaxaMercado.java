package com.srmasset.creditengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Histórico append-only de taxas de mercado (CDI para BRL, SOFR para USD) — taxa externa, o fundo
 * só consome, nunca decide o valor.
 */
@Entity
@Table(name = "taxa_mercado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxaMercado {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moeda_codigo", nullable = false)
  private Moeda moeda;

  @Column(name = "indicador", nullable = false, length = 20)
  private String indicador;

  @Column(name = "valor", nullable = false, precision = 9, scale = 6)
  private BigDecimal valor;

  @Column(name = "vigente_em", nullable = false)
  private Instant vigenteEm;

  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;
}
