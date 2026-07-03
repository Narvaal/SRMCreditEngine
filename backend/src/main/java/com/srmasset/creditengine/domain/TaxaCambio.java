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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Histórico append-only de taxas de câmbio — nunca UPDATE/DELETE, uma nova cotação é sempre uma
 * linha nova.
 */
@Entity
@Table(name = "taxa_cambio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxaCambio {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moeda_origem", nullable = false)
  private Moeda moedaOrigem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moeda_destino", nullable = false)
  private Moeda moedaDestino;

  @Column(name = "valor", nullable = false, precision = 19, scale = 8)
  private BigDecimal valor;

  @Column(name = "vigente_em", nullable = false)
  private Instant vigenteEm;

  @Generated(event = EventType.INSERT)
  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;
}
