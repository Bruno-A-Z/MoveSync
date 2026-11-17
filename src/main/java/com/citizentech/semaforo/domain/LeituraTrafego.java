package com.citizentech.semaforo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ct_leitura",
        indexes = @Index(name = "ix_leitura_via_data", columnList = "via_id, registrado_em"))
@Getter
@Setter
@NoArgsConstructor
public class LeituraTrafego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "via_id", nullable = false)
    private Via via;

    @Column(name = "quantidade_veiculos", nullable = false)
    private int quantidadeVeiculos;

    @Column(name = "tamanho_fila_metros", nullable = false)
    private int tamanhoFilaMetros;

    @Column(name = "veiculo_emergencia", nullable = false)
    private boolean veiculoEmergencia;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;
}
