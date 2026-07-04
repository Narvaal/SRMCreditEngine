package com.srmasset.creditengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Saldo controlado por moeda. Débito/crédito e o insert em {@link Liquidacao} acontecem na mesma
 * transação atômica; {@code version} habilita Optimistic Locking para proteger contra duas
 * liquidações concorrentes disputando o mesmo caixa.
 */
@Entity
@Table(name = "caixa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caixa {

  @Id
  @Column(name = "moeda_codigo", length = 3)
  private String moedaCodigo;

  @Column(name = "saldo", nullable = false, precision = 18, scale = 2)
  private BigDecimal saldo;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @UpdateTimestamp
  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;
}
