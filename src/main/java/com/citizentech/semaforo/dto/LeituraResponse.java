package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.LeituraTrafego;

import java.time.Instant;

public record LeituraResponse(
        Long id, Long viaId, int quantidadeVeiculos, int tamanhoFilaMetros,
        boolean veiculoEmergencia, Instant registradoEm) {

    public static LeituraResponse de(LeituraTrafego l) {
        return new LeituraResponse(l.getId(), l.getVia().getId(), l.getQuantidadeVeiculos(),
                l.getTamanhoFilaMetros(), l.isVeiculoEmergencia(), l.getRegistradoEm());
    }
}
