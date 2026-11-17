package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.DecisaoSemaforo;
import com.citizentech.semaforo.domain.ModoDecisao;

import java.time.Instant;

public record DecisaoResponse(
        Long id, Long cruzamentoId, Long viaPrioritariaId, String viaPrioritariaNome,
        int tempoVerdeSegundos, ModoDecisao modo, String explicacao, Instant criadoEm) {

    public static DecisaoResponse de(DecisaoSemaforo d) {
        return new DecisaoResponse(d.getId(), d.getCruzamento().getId(), d.getViaPrioritaria().getId(),
                d.getViaPrioritaria().getNome(), d.getTempoVerdeSegundos(), d.getModo(),
                d.getExplicacao(), d.getCriadoEm());
    }
}
