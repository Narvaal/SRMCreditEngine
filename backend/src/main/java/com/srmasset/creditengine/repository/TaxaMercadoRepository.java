package com.srmasset.creditengine.repository;

import com.srmasset.creditengine.domain.TaxaMercado;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxaMercadoRepository extends JpaRepository<TaxaMercado, UUID> {

  Optional<TaxaMercado> findFirstByMoeda_CodigoAndIndicadorOrderByVigenteEmDesc(
      String moedaCodigo, String indicador);
}
