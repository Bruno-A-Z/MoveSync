package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.Via;

public record ViaResponse(Long id, Long cruzamentoId, String nome, String sentido) {

    public static ViaResponse de(Via v) {
        return new ViaResponse(v.getId(), v.getCruzamento().getId(), v.getNome(), v.getSentido());
    }
}
