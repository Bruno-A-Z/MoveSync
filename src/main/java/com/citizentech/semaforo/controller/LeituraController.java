package com.citizentech.semaforo.controller;

import com.citizentech.semaforo.dto.LeituraRequest;
import com.citizentech.semaforo.dto.LeituraResponse;
import com.citizentech.semaforo.dto.MensagemResponse;
import com.citizentech.semaforo.service.LeituraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leituras")
@Tag(name = "Leituras de sensores (GESTOR)")
public class LeituraController {

    private final LeituraService service;

    public LeituraController(LeituraService service) {
        this.service = service;
    }

    @Operation(summary = "Injeta uma leitura de sensor: publica o evento no RabbitMQ (processamento assíncrono)")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MensagemResponse publicar(@Valid @RequestBody LeituraRequest req) {
        service.publicar(req);
        return new MensagemResponse("Leitura enviada para processamento");
    }

    @Operation(summary = "Histórico de leituras de uma via (paginado)")
    @GetMapping
    public PagedModel<LeituraResponse> listar(
            @RequestParam Long viaId,
            @ParameterObject @PageableDefault(size = 10, sort = "registradoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.listar(viaId, pageable));
    }
}
