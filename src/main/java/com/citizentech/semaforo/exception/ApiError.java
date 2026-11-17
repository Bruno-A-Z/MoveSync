package com.citizentech.semaforo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.List;

/** Formato padrão de erro de toda a API. */
public record ApiError(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {
    }

    public static ApiError de(HttpStatusCode status, String mensagem, String caminho, List<CampoInvalido> campos) {
        HttpStatus resolvido = HttpStatus.resolve(status.value());
        String erro = resolvido != null ? resolvido.getReasonPhrase() : "Erro";
        return new ApiError(Instant.now(), status.value(), erro, mensagem, caminho, campos);
    }

    public static ApiError de(HttpStatusCode status, String mensagem, String caminho) {
        return de(status, mensagem, caminho, List.of());
    }
}
