package com.citizentech.semaforo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ViaRequest(
        @NotNull Long cruzamentoId,
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 60) String sentido) {
}
