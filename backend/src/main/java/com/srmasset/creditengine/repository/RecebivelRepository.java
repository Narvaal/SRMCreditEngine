package com.srmasset.creditengine.repository;

import com.srmasset.creditengine.domain.Recebivel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecebivelRepository extends JpaRepository<Recebivel, UUID> {}
