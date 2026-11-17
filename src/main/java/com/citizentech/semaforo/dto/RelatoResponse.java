package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.CategoriaRelato;
import com.citizentech.semaforo.domain.Relato;
import com.citizentech.semaforo.domain.StatusRelato;

import java.time.Instant;

public record RelatoResponse(
        Long id, Long cruzamentoId, String cruzamentoNome, String descricao,
        CategoriaRelato categoria, int gravidade, String resumoIa,
        StatusRelato status, Instant criadoEm) {

    public static RelatoResponse de(Relato r) {
        return new RelatoResponse(r.getId(), r.getCruzamento().getId(), r.getCruzamento().getNome(),
                r.getDescricao(), r.getCategoria(), r.getGravidade(), r.getResumoIa(),
                r.getStatus(), r.getCriadoEm());
    }
}
