package com.citizentech.semaforo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RelatoRequest(
        @NotNull Long cruzamentoId,
        @NotBlank @Size(min = 10, max = 1000, message = "a descrição deve ter entre 10 e 1000 caracteres") String descricao) {
}
