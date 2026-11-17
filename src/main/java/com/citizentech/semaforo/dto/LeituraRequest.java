package com.citizentech.semaforo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LeituraRequest(
        @NotNull Long viaId,
        @NotNull @Min(0) @Max(500) Integer quantidadeVeiculos,
        @NotNull @Min(0) @Max(2000) Integer tamanhoFilaMetros,
        boolean veiculoEmergencia) {
}
