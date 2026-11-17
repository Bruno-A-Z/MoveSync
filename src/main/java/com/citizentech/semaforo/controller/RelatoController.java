package com.citizentech.semaforo.controller;

import com.citizentech.semaforo.domain.StatusRelato;
import com.citizentech.semaforo.dto.RelatoRequest;
import com.citizentech.semaforo.dto.RelatoResponse;
import com.citizentech.semaforo.dto.StatusRelatoRequest;
import com.citizentech.semaforo.security.UsuarioLogado;
import com.citizentech.semaforo.service.RelatoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relatos")
@Tag(name = "Relatos do cidadão")
public class RelatoController {

    private final RelatoService service;

    public RelatoController(RelatoService service) {
        this.service = service;
    }

    @Operation(summary = "[CIDADAO] Reporta um problema; a IA classifica categoria, gravidade e gera um resumo")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelatoResponse criar(@AuthenticationPrincipal UsuarioLogado usuario,
                                @Valid @RequestBody RelatoRequest req) {
        return service.criar(usuario.id(), req);
    }

    @Operation(summary = "[CIDADAO] Meus relatos (paginado)")
    @GetMapping("/meus")
    public PagedModel<RelatoResponse> meus(
            @AuthenticationPrincipal UsuarioLogado usuario,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.listarDoUsuario(usuario.id(), pageable));
    }

    @Operation(summary = "[GESTOR] Todos os relatos, com filtro opcional por status (paginado)")
    @GetMapping
    public PagedModel<RelatoResponse> listar(
            @RequestParam(required = false) StatusRelato status,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.listarTodos(status, pageable));
    }

    @Operation(summary = "[GESTOR] Atualiza o status de um relato")
    @PatchMapping("/{id}/status")
    public RelatoResponse atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusRelatoRequest req) {
        return service.atualizarStatus(id, req.status());
    }
}
