package com.srmasset.creditengine.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo puro: o spread de risco não mora aqui, mora exclusivamente na implementação de {@link
 * com.srmasset.creditengine.pricing.PricingStrategy} correspondente ao {@link #codigo}.
 */
@Entity
@Table(name = "tipo_recebivel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoRecebivel {

  @Id
  @Column(name = "codigo", length = 50)
  private String codigo;

  @Column(name = "nome", nullable = false)
  private String nome;

  @Column(name = "ativo", nullable = false)
  private Boolean ativo;

  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;
}
