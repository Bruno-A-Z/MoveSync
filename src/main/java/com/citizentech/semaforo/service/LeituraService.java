package com.citizentech.semaforo.service;

import com.citizentech.semaforo.dto.LeituraRequest;
import com.citizentech.semaforo.dto.LeituraResponse;
import com.citizentech.semaforo.exception.RecursoNaoEncontradoException;
import com.citizentech.semaforo.messaging.LeituraEvent;
import com.citizentech.semaforo.messaging.LeituraPublisher;
import com.citizentech.semaforo.repository.LeituraRepository;
import com.citizentech.semaforo.repository.ViaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeituraService {

    private final ViaRepository vias;
    private final LeituraRepository leituras;
    private final LeituraPublisher publisher;

    public LeituraService(ViaRepository vias, LeituraRepository leituras, LeituraPublisher publisher) {
        this.vias = vias;
        this.leituras = leituras;
        this.publisher = publisher;
    }

    /** Publica o evento no RabbitMQ; quem persiste e decide é o consumidor (assíncrono). */
    public void publicar(LeituraRequest req) {
        if (!vias.existsById(req.viaId())) {
            throw new RecursoNaoEncontradoException("Via " + req.viaId() + " não encontrada");
        }
        publisher.publicar(new LeituraEvent(req.viaId(), req.quantidadeVeiculos(),
                req.tamanhoFilaMetros(), req.veiculoEmergencia()));
    }

    @Transactional(readOnly = true)
    public Page<LeituraResponse> listar(Long viaId, Pageable pageable) {
        return leituras.findByViaId(viaId, pageable).map(LeituraResponse::de);
    }
}
