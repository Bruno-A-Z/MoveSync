package com.citizentech.semaforo.controller;

import com.citizentech.semaforo.dto.ViaRequest;
import com.citizentech.semaforo.dto.ViaResponse;
import com.citizentech.semaforo.service.ViaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/vias")
@Tag(name = "Vias")
public class ViaController {

    private final ViaService service;

    public ViaController(ViaService service) {
        this.service = service;
    }

    @Operation(summary = "Lista vias (paginado); filtre por cruzamentoId")
    @GetMapping
    public PagedModel<ViaResponse> listar(@RequestParam(required = false) Long cruzamentoId,
                                          @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return new PagedModel<>(service.listar(cruzamentoId, pageable));
    }

    @Operation(summary = "Busca uma via")
    @GetMapping("/{id}")
    public ViaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "[GESTOR] Cria via")
    @PostMapping
    public ResponseEntity<ViaResponse> criar(@Valid @RequestBody ViaRequest req) {
        ViaResponse criada = service.criar(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @Operation(summary = "[GESTOR] Atualiza via")
    @PutMapping("/{id}")
    public ViaResponse atualizar(@PathVariable Long id, @Valid @RequestBody ViaRequest req) {
        return service.atualizar(id, req);
    }

    @Operation(summary = "[GESTOR] Exclui via (409 se tiver leituras ou decisões)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
