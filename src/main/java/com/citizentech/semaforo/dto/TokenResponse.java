package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.Perfil;

public record TokenResponse(String token, String tipo, long expiraEmSegundos, String nome, Perfil perfil) {
}
