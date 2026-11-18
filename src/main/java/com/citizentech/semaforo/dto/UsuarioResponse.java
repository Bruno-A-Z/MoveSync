package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.Perfil;
import com.citizentech.semaforo.domain.Usuario;

public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil());
    }
}