package com.citizentech.semaforo.dto;

import com.citizentech.semaforo.domain.StatusRelato;
import jakarta.validation.constraints.NotNull;

public record StatusRelatoRequest(@NotNull StatusRelato status) {
}
