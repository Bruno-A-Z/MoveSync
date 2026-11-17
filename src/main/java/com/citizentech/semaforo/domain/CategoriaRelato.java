package com.citizentech.semaforo.domain;

public enum CategoriaRelato {
    SEMAFORO_APAGADO,
    TEMPO_INADEQUADO,
    TRAVESSIA_INSEGURA,
    FALTA_ACESSIBILIDADE,
    SINALIZACAO,
    OUTROS;

    public static CategoriaRelato deTexto(String texto) {
        if (texto == null) {
            return OUTROS;
        }
        try {
            return valueOf(texto.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return OUTROS;
        }
    }
}
