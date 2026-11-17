package com.citizentech.semaforo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ct_relato")
@Getter
@Setter
@NoArgsConstructor
public class Relato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cruzamento_id", nullable = false)
    private Cruzamento cruzamento;

    @Column(nullable = false, length = 1000)
    private String descricao;

    /** Classificação feita pelo LLM. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaRelato categoria;

    /** 1 (baixa) a 5 (risco imediato). Definida pelo LLM. */
    @Column(nullable = false)
    private int gravidade;

    @Column(name = "resumo_ia", nullable = false, length = 300)
    private String resumoIa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRelato status = StatusRelato.ABERTO;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
