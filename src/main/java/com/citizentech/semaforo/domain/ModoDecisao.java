package com.citizentech.semaforo.domain;

public enum ModoDecisao {
    /** Tempo calculado a partir da demanda das vias. */
    ADAPTATIVO,
    /** Sensor sem leitura recente: volta ao tempo fixo e reveza as vias (fail-safe). */
    FALLBACK_FIXO,
    /** Veículo de emergência detectado: via liberada com o tempo máximo. */
    EMERGENCIA
}
