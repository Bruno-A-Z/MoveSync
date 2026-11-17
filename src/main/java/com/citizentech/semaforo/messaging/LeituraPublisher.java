package com.citizentech.semaforo.messaging;

import com.citizentech.semaforo.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class LeituraPublisher {

    private final RabbitTemplate rabbitTemplate;

    public LeituraPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(LeituraEvent evento) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, evento);
    }
}
