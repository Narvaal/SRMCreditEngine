package com.srmasset.creditengine.repository;

import com.srmasset.creditengine.domain.Cedente;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CedenteRepository extends JpaRepository<Cedente, UUID> {}
