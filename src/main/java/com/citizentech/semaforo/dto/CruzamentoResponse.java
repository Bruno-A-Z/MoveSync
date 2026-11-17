package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.Cruzamento;

public record CruzamentoResponse(
        Long id, String nome, String endereco, Double latitude, Double longitude, boolean ativo,
        int tempoMinVerde, int tempoMaxVerde, int tempoFixo) {

    public static CruzamentoResponse de(Cruzamento c) {
        return new CruzamentoResponse(c.getId(), c.getNome(), c.getEndereco(), c.getLatitude(), c.getLongitude(),
                c.isAtivo(), c.getTempoMinVerde(), c.getTempoMaxVerde(), c.getTempoFixo());
    }
}
