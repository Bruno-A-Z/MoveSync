package com.citizentech.semaforo.messaging;

import com.citizentech.semaforo.config.RabbitConfig;
import com.citizentech.semaforo.exception.RecursoNaoEncontradoException;
import com.citizentech.semaforo.service.TrafegoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consome (de forma assíncrona) as leituras vindas da API REST ou dos sensores MQTT. */
@Component
public class LeituraConsumer {

    private static final Logger log = LoggerFactory.getLogger(LeituraConsumer.class);

    private final TrafegoService trafegoService;

    public LeituraConsumer(TrafegoService trafegoService) {
        this.trafegoService = trafegoService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void receber(LeituraEvent evento) {
        if (evento == null || !evento.valido()) {
            log.warn("Leitura descartada (payload inválido): {}", evento);
            return;
        }
        try {
            trafegoService.processarLeitura(evento);
        } catch (RecursoNaoEncontradoException e) {
            log.warn("Leitura descartada: {}", e.getMessage());
        }
    }
}
