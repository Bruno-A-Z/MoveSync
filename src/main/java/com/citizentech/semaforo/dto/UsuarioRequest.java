package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 160) String email,
        @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres") String senha,
        @NotNull Perfil perfil) {
}