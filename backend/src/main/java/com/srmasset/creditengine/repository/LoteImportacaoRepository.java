package com.srmasset.creditengine.repository;

import com.srmasset.creditengine.domain.LoteImportacao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteImportacaoRepository extends JpaRepository<LoteImportacao, UUID> {}
