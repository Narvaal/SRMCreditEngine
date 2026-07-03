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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "moeda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Moeda {

  @Id
  @Column(name = "codigo", length = 3)
  private String codigo;

  @Column(name = "nome", nullable = false)
  private String nome;

  @Column(name = "casas_decimais", nullable = false)
  private Short casasDecimais;

  @Generated(event = EventType.INSERT)
  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;
}
