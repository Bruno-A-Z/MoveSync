package com.citizentech.semaforo.service;

import com.citizentech.semaforo.domain.Cruzamento;
import com.citizentech.semaforo.domain.Via;
import com.citizentech.semaforo.dto.ViaRequest;
import com.citizentech.semaforo.dto.ViaResponse;
import com.citizentech.semaforo.exception.RecursoNaoEncontradoException;
import com.citizentech.semaforo.repository.CruzamentoRepository;
import com.citizentech.semaforo.repository.ViaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViaService {

    private final ViaRepository vias;
    private final CruzamentoRepository cruzamentos;

    public ViaService(ViaRepository vias, CruzamentoRepository cruzamentos) {
        this.vias = vias;
        this.cruzamentos = cruzamentos;
    }

    @Transactional(readOnly = true)
    public Page<ViaResponse> listar(Long cruzamentoId, Pageable pageable) {
        Page<Via> pagina = cruzamentoId == null
                ? vias.findAll(pageable)
                : vias.findByCruzamentoId(cruzamentoId, pageable);
        return pagina.map(ViaResponse::de);
    }

    @Transactional(readOnly = true)
    public ViaResponse buscar(Long id) {
        return ViaResponse.de(obter(id));
    }

    @Transactional
    public ViaResponse criar(ViaRequest req) {
        Via v = new Via();
        aplicar(v, req);
        return ViaResponse.de(vias.save(v));
    }

    @Transactional
    public ViaResponse atualizar(Long id, ViaRequest req) {
        Via v = obter(id);
        aplicar(v, req);
        return ViaResponse.de(vias.save(v));
    }

    @Transactional
    public void excluir(Long id) {
        vias.delete(obter(id));
        vias.flush(); // via com leituras/decisões => 409 pelo handler global
    }

    private Via obter(Long id) {
        return vias.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Via " + id + " não encontrada"));
    }

    private void aplicar(Via v, ViaRequest req) {
        Cruzamento c = cruzamentos.findById(req.cruzamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cruzamento " + req.cruzamentoId() + " não encontrado"));
        v.setCruzamento(c);
        v.setNome(req.nome().trim());
        v.setSentido(req.sentido().trim());
    }
}
