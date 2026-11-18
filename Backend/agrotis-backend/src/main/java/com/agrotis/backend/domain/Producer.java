package com.agrotis.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "producers")
public class Producer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "inscricao_fiscal", nullable = false, unique = true)
    private String inscricaoFiscal;

    @Embedded
    private Address endereco;

    @OneToMany(mappedBy = "produtor")
    private List<Property> propriedades = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratorio;

    @Column(name = "data_inicial")
    private LocalDateTime dataInicial;

    @Column(name = "data_final")
    private LocalDateTime dataFinal;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    public Producer() {
    }

    public Producer(String nome, String inscricaoFiscal, Address endereco, Laboratory laboratorio,
                    LocalDateTime dataInicial, LocalDateTime dataFinal, String observacoes) {
        this.nome = nome;
        this.inscricaoFiscal = inscricaoFiscal;
        this.endereco = endereco;
        this.laboratorio = laboratorio;
        this.dataInicial = dataInicial;
        this.dataFinal = dataFinal;
        this.observacoes = observacoes;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getInscricaoFiscal() {
        return inscricaoFiscal;
    }

    public void setInscricaoFiscal(String inscricaoFiscal) {
        this.inscricaoFiscal = inscricaoFiscal;
    }

    public Address getEndereco() {
        return endereco;
    }

    public void setEndereco(Address endereco) {
        this.endereco = endereco;
    }

    public List<Property> getPropriedades() {
        return propriedades;
    }

    public void setPropriedades(List<Property> propriedades) {
        this.propriedades = new ArrayList<>(propriedades);
    }

    public Laboratory getLaboratorio() {
        return laboratorio;
    }

    public void setLaboratorio(Laboratory laboratorio) {
        this.laboratorio = laboratorio;
    }

    public LocalDateTime getDataInicial() {
        return dataInicial;
    }

    public void setDataInicial(LocalDateTime dataInicial) {
        this.dataInicial = dataInicial;
    }

    public LocalDateTime getDataFinal() {
        return dataFinal;
    }

    public void setDataFinal(LocalDateTime dataFinal) {
        this.dataFinal = dataFinal;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
