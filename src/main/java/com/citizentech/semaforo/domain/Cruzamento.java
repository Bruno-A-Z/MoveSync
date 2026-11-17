package com.citizentech.semaforo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ct_cruzamento")
@Getter
@Setter
@NoArgsConstructor
public class Cruzamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 200)
    private String endereco;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Menor tempo de verde permitido (segurança). */
    @Column(name = "tempo_min_verde", nullable = false)
    private int tempoMinVerde;

    /** Maior tempo de verde permitido (evita starvation das outras vias). */
    @Column(name = "tempo_max_verde", nullable = false)
    private int tempoMaxVerde;

    /** Tempo usado quando os sensores estão fora do ar. */
    @Column(name = "tempo_fixo", nullable = false)
    private int tempoFixo;
}
