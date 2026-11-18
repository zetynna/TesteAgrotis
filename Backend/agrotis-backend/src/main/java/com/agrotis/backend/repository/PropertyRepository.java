package com.agrotis.backend.repository;

import com.agrotis.backend.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    java.util.Optional<Property> findByInscricaoFiscal(String inscricaoFiscal);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "produtor")
    java.util.List<Property> findAllByOrderByNomeAsc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "produtor")
    java.util.List<Property> findAllByProdutor_IdIn(java.util.Collection<Long> producerIds);
}
