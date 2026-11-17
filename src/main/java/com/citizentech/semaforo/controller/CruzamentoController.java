package com.citizentech.semaforo.controller;

import com.citizentech.semaforo.dto.CruzamentoRequest;
import com.citizentech.semaforo.dto.CruzamentoResponse;
import com.citizentech.semaforo.dto.StatusCruzamentoResponse;
import com.citizentech.semaforo.service.CruzamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/cruzamentos")
@Tag(name = "Cruzamentos")
public class CruzamentoController {

    private final CruzamentoService service;

    public CruzamentoController(CruzamentoService service) {
        this.service = service;
    }

    @Operation(summary = "Lista cruzamentos (paginado)")
    @GetMapping
    public PagedModel<CruzamentoResponse> listar(@ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return new PagedModel<>(service.listar(pageable));
    }

    @Operation(summary = "Busca um cruzamento")
    @GetMapping("/{id}")
    public CruzamentoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @Operation(summary = "Situação atual do cruzamento: fila por via e última decisão com explicação da IA")
    @GetMapping("/{id}/status")
    public StatusCruzamentoResponse status(@PathVariable Long id) {
        return service.status(id);
    }

    @Operation(summary = "[GESTOR] Cria cruzamento")
    @PostMapping
    public ResponseEntity<CruzamentoResponse> criar(@Valid @RequestBody CruzamentoRequest req) {
        CruzamentoResponse criado = service.criar(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @Operation(summary = "[GESTOR] Atualiza cruzamento")
    @PutMapping("/{id}")
    public CruzamentoResponse atualizar(@PathVariable Long id, @Valid @RequestBody CruzamentoRequest req) {
        return service.atualizar(id, req);
    }

    @Operation(summary = "[GESTOR] Exclui cruzamento (409 se tiver vias, decisões ou relatos)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
