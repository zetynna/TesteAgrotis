package com.agrotis.backend.repository;

import com.agrotis.backend.domain.Laboratory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {

    @EntityGraph(attributePaths = "produtores")
    List<Laboratory> findAllByOrderByNomeAsc();

    java.util.Optional<Laboratory> findByInscricaoFiscal(String inscricaoFiscal);
}
