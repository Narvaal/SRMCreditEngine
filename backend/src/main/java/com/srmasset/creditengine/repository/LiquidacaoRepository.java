package com.srmasset.creditengine.repository;

import com.srmasset.creditengine.domain.Liquidacao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiquidacaoRepository extends JpaRepository<Liquidacao, UUID> {

  boolean existsByLiquidacaoEstornada_Id(UUID liquidacaoEstornadaId);
}
