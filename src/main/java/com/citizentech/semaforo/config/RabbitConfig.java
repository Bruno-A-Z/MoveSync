package com.citizentech.semaforo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia:
 *  - API REST  -> exchange "trafego.exchange" (routing key leitura.registrada) -> fila "trafego.leituras"
 *  - ESP32/MQTT -> exchange "amq.topic" (plugin MQTT; tópico sensores/1/2 vira routing key sensores.1.2)
 *                  -> mesma fila "trafego.leituras"
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "trafego.exchange";
    public static final String QUEUE = "trafego.leituras";
    public static final String ROUTING_KEY = "leitura.registrada";
    public static final String MQTT_EXCHANGE = "amq.topic";
    public static final String MQTT_ROUTING_KEY = "sensores.#";

    @Bean
    public DirectExchange trafegoExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue leiturasQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding bindingRest(Queue leiturasQueue, DirectExchange trafegoExchange) {
        return BindingBuilder.bind(leiturasQueue).to(trafegoExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding bindingMqtt(Queue leiturasQueue) {
        return BindingBuilder.bind(leiturasQueue).to(new TopicExchange(MQTT_EXCHANGE)).with(MQTT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
