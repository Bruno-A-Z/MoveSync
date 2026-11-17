package com.citizentech.semaforo.dto;

import jakarta.validation.constraints.*;

public record CruzamentoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 200) String endereco,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        Boolean ativo,
        @NotNull @Min(5) @Max(180) Integer tempoMinVerde,
        @NotNull @Min(5) @Max(180) Integer tempoMaxVerde,
        @NotNull @Min(5) @Max(180) Integer tempoFixo) {
}
