package com.citizentech.semaforo.controller;

import com.citizentech.semaforo.domain.Perfil;
import com.citizentech.semaforo.dto.PerfilRequest;
import com.citizentech.semaforo.dto.UsuarioRequest;
import com.citizentech.semaforo.dto.UsuarioResponse;
import com.citizentech.semaforo.security.UsuarioLogado;
import com.citizentech.semaforo.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @Operation(summary = "Meu perfil")
    @GetMapping("/me")
    public UsuarioResponse meuPerfil(@AuthenticationPrincipal UsuarioLogado logado) {
        return service.meuPerfil(logado.id());
    }

    @Operation(summary = "Atualiza meus dados (nome, e-mail e, opcionalmente, senha)")
    @PutMapping("/me")
    public UsuarioResponse atualizarMeuPerfil(@AuthenticationPrincipal UsuarioLogado logado,
                                              @Valid @RequestBody PerfilRequest req) {
        return service.atualizarMeuPerfil(logado.id(), req);
    }

    @Operation(summary = "[GESTOR] Lista usuários (paginado); busca por nome, e-mail ou id; filtro por perfil")
    @GetMapping
    public PagedModel<UsuarioResponse> listar(@RequestParam(required = false) Perfil perfil,
                                              @RequestParam(required = false) String busca,
                                              @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return new PagedModel<>(service.listar(perfil, busca, pageable));
    }

    @Operation(summary = "[GESTOR] Busca um usuário")
    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "[GESTOR] Cria usuário (CIDADAO ou GESTOR)")
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest req) {
        UsuarioResponse criado = service.criar(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(summary = "[GESTOR] Atualiza usuário")
    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id,
                                     @AuthenticationPrincipal UsuarioLogado logado,
                                     @Valid @RequestBody UsuarioRequest req) {
        return service.atualizar(id, req, logado.id());
    }

    @Operation(summary = "[GESTOR] Exclui usuário (409 se tiver relatos; 422 se for você mesmo)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado) {
        service.excluir(id, logado.id());
        return ResponseEntity.noContent().build();
    }
}