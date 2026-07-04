package com.srmasset.creditengine.repository;

import com.srmasset.creditengine.domain.TaxaCambio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxaCambioRepository extends JpaRepository<TaxaCambio, UUID> {

  Optional<TaxaCambio> findFirstByMoedaOrigem_CodigoAndMoedaDestino_CodigoOrderByVigenteEmDesc(
      String moedaOrigemCodigo, String moedaDestinoCodigo);
}
