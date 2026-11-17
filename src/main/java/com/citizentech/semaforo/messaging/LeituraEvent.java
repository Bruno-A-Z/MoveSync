package com.citizentech.semaforo.messaging;

/**
 * Evento de leitura de sensor. É o mesmo JSON publicado pelo ESP32 via MQTT
 * (tópico sensores/{cruzamentoId}/{viaId}) ou pela API REST.
 */
public record LeituraEvent(
        Long viaId,
        Integer quantidadeVeiculos,
        Integer tamanhoFilaMetros,
        Boolean veiculoEmergencia) {

    public boolean valido() {
        return viaId != null
                && quantidadeVeiculos != null && quantidadeVeiculos >= 0 && quantidadeVeiculos <= 500
                && tamanhoFilaMetros != null && tamanhoFilaMetros >= 0 && tamanhoFilaMetros <= 2000;
    }
}
