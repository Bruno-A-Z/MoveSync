package com.citizentech.semaforo.service;

import com.citizentech.semaforo.dto.DecisaoResponse;
import com.citizentech.semaforo.exception.RecursoNaoEncontradoException;
import com.citizentech.semaforo.repository.CruzamentoRepository;
import com.citizentech.semaforo.repository.DecisaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisaoService {

    private final DecisaoRepository decisoes;
    private final CruzamentoRepository cruzamentos;

    public DecisaoService(DecisaoRepository decisoes, CruzamentoRepository cruzamentos) {
        this.decisoes = decisoes;
        this.cruzamentos = cruzamentos;
    }

    @Transactional(readOnly = true)
    public Page<DecisaoResponse> listar(Long cruzamentoId, Pageable pageable) {
        if (!cruzamentos.existsById(cruzamentoId)) {
            throw new RecursoNaoEncontradoException("Cruzamento " + cruzamentoId + " não encontrado");
        }
        return decisoes.findByCruzamentoId(cruzamentoId, pageable).map(DecisaoResponse::de);
    }
}
