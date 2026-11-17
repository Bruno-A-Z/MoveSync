package com.citizentech.semaforo.ai;

/** Saída estruturada esperada do LLM ao classificar um relato do cidadão. */
public record ClassificacaoRelato(String categoria, Integer gravidade, String resumo) {
}
