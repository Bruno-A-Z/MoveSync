package com.citizentech.semaforo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ct_decisao")
@Getter
@Setter
@NoArgsConstructor
public class DecisaoSemaforo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cruzamento_id", nullable = false)
    private Cruzamento cruzamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "via_prioritaria_id", nullable = false)
    private Via viaPrioritaria;

    @Column(name = "tempo_verde_segundos", nullable = false)
    private int tempoVerdeSegundos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModoDecisao modo;

    /** Texto gerado pelo LLM (Spring AI) explicando a decisão em linguagem simples. */
    @Column(nullable = false, length = 1000)
    private String explicacao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}
