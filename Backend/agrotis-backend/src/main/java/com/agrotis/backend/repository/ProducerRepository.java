package com.agrotis.backend.repository;

import com.agrotis.backend.domain.Producer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducerRepository extends JpaRepository<Producer, Long> {

    Optional<Producer> findByInscricaoFiscal(String inscricaoFiscal);

    @EntityGraph(attributePaths = {"propriedades", "laboratorio"})
    List<Producer> findAllByOrderByNomeAsc();
}
