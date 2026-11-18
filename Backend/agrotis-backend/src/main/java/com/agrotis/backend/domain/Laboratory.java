package com.agrotis.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "laboratories")
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "inscricao_fiscal", nullable = false, unique = true)
    private String inscricaoFiscal;

    @Embedded
    private Address endereco;

    @OneToMany(mappedBy = "laboratorio")
    private List<Producer> produtores = new ArrayList<>();

    public Laboratory() {
    }

    public Laboratory(String nome, String inscricaoFiscal, Address endereco) {
        this.nome = nome;
        this.inscricaoFiscal = inscricaoFiscal;
        this.endereco = endereco;
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

    public List<Producer> getProdutores() {
        return produtores;
    }
}
