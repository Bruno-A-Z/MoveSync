package com.citizentech.semaforo.dto;

import java.time.Instant;
import java.util.List;

/** Visão do cidadão: situação atual do cruzamento e última decisão do semáforo. */
public record StatusCruzamentoResponse(
        Long cruzamentoId, String nome, String endereco, boolean ativo,
        List<ViaStatus> vias, DecisaoResponse ultimaDecisao) {

    public record ViaStatus(
            Long viaId, String nome, String sentido,
            Integer quantidadeVeiculos, Integer tamanhoFilaMetros, Instant atualizadoEm) {
    }
}
