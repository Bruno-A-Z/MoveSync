package com.citizentech.semaforo.security;

import com.citizentech.semaforo.domain.Perfil;

/** Principal extraído do JWT (sem consultar o banco a cada requisição). */
public record UsuarioLogado(Long id, String email, Perfil perfil) {
}
