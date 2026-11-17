package com.citizentech.semaforo.controller;

import com.citizentech.semaforo.dto.DecisaoResponse;
import com.citizentech.semaforo.service.DecisaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/decisoes")
@Tag(name = "Decisões do semáforo")
public class DecisaoController {

    private final DecisaoService service;

    public DecisaoController(DecisaoService service) {
        this.service = service;
    }

    @Operation(summary = "Histórico de decisões de um cruzamento, com a explicação gerada pela IA (paginado)")
    @GetMapping
    public PagedModel<DecisaoResponse> listar(
            @RequestParam Long cruzamentoId,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.listar(cruzamentoId, pageable));
    }
}
