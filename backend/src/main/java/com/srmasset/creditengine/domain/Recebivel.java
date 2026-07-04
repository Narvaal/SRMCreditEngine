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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * {@link #status} e {@link #version} são o estado corrente projetado, atualizado atomicamente junto
 * com o insert em {@link Liquidacao} — complementar ao ledger imutável, não uma contradição a ele.
 * {@code version} habilita Optimistic Locking (Hibernate/JPA {@code @Version}).
 */
@Entity
@Table(name = "recebivel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recebivel {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cedente_id", nullable = false)
  private Cedente cedente;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tipo_recebivel_codigo", nullable = false)
  private TipoRecebivel tipoRecebivel;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lote_importacao_id")
  private LoteImportacao loteImportacao;

  @Column(name = "valor_face", nullable = false, precision = 18, scale = 2)
  private BigDecimal valorFace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moeda_titulo", nullable = false)
  private Moeda moedaTitulo;

  @Column(name = "data_vencimento", nullable = false)
  private LocalDate dataVencimento;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private StatusRecebivel status;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Generated(event = EventType.INSERT)
  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;
}
