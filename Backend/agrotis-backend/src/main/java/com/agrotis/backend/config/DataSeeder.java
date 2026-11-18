package com.agrotis.backend.config;

import com.agrotis.backend.domain.Address;
import com.agrotis.backend.domain.Laboratory;
import com.agrotis.backend.domain.Producer;
import com.agrotis.backend.domain.Property;
import com.agrotis.backend.repository.LaboratoryRepository;
import com.agrotis.backend.repository.ProducerRepository;
import com.agrotis.backend.repository.PropertyRepository;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private final PropertyRepository propertyRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ProducerRepository producerRepository;

    public DataSeeder(PropertyRepository propertyRepository,
                      LaboratoryRepository laboratoryRepository,
                      ProducerRepository producerRepository) {
        this.propertyRepository = propertyRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.producerRepository = producerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (propertyRepository.count() > 0 || laboratoryRepository.count() > 0) {
            return;
        }

        Property property1 = propertyRepository.save(new Property("Fazenda Sol Nascente", "123.456.789-09", new Address("Rodovia BR 101, Km 45", "Itajubá", "MG", "37500-000")));
        Property property2 = propertyRepository.save(new Property("Fazenda Vista Alegre", "987.654.321-00", new Address("Estrada Municipal, Km 12", "Pouso Alegre", "MG", "37550-000")));
        Property property3 = propertyRepository.save(new Property("Fazenda Boa Esperança", "111.222.333-96", new Address("Rua do Campo, 150", "Lavras", "MG", "37200-000")));
        Property property4 = propertyRepository.save(new Property("Fazenda Serra Verde", "222.333.444-05", new Address("Estrada da Serra, 30", "Poços de Caldas", "MG", "37700-000")));

        Laboratory laboratory1 = laboratoryRepository.save(new Laboratory("Laboratório Modelo 1", "11.222.333/0001-81", new Address("Av. Central, 100", "Belo Horizonte", "MG", "30100-000")));
        Laboratory laboratory2 = laboratoryRepository.save(new Laboratory("Laboratório Modelo 2", "22.333.444/0001-81", new Address("Rua das Flores, 250", "Uberlândia", "MG", "38400-000")));

        Producer producer1 = new Producer(
            "Manoel da Silva Ribeiro",
            "555.666.777-20",
            new Address("Rua Principal, 100", "Pouso Alegre", "MG", "37500-000"),
            laboratory1,
            LocalDateTime.of(2026, 6, 11, 12, 0),
            LocalDateTime.of(2026, 6, 15, 18, 0),
            "Iniciando testes de integração"
        );

        Producer producer2 = new Producer(
            "Ana Maria Costa",
            "666.777.888-30",
            new Address("Praça da Estação, 45", "Lavras", "MG", "37200-555"),
            laboratory1,
            LocalDateTime.of(2026, 7, 1, 8, 0),
            LocalDateTime.of(2026, 7, 10, 12, 0),
            "Dados de produção em análise"
        );

        Producer producer3 = new Producer(
            "Carlos Pereira",
            "777.888.999-41",
            new Address("Estrada da Serra, 22", "Poços de Caldas", "MG", "37700-000"),
            laboratory2,
            LocalDateTime.of(2026, 8, 5, 9, 0),
            LocalDateTime.of(2026, 8, 21, 13, 0),
            "Sincronização de contrato concluída"
        );
        producerRepository.saveAll(java.util.List.of(producer1, producer2, producer3));
        property1.setProdutor(producer1);
        property2.setProdutor(producer1);
        property3.setProdutor(producer2);
        property4.setProdutor(producer3);
        producer1.setPropriedades(java.util.List.of(property1, property2));
        producer2.setPropriedades(java.util.List.of(property3));
        producer3.setPropriedades(java.util.List.of(property4));
        propertyRepository.saveAll(java.util.List.of(property1, property2, property3, property4));
    }
}
