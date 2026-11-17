package com.citizentech.semaforo.service;

import com.citizentech.semaforo.domain.Cruzamento;
import com.citizentech.semaforo.domain.DecisaoSemaforo;
import com.citizentech.semaforo.domain.LeituraTrafego;
import com.citizentech.semaforo.domain.Via;
import com.citizentech.semaforo.dto.CruzamentoRequest;
import com.citizentech.semaforo.dto.CruzamentoResponse;
import com.citizentech.semaforo.dto.DecisaoResponse;
import com.citizentech.semaforo.dto.StatusCruzamentoResponse;
import com.citizentech.semaforo.dto.StatusCruzamentoResponse.ViaStatus;
import com.citizentech.semaforo.exception.RecursoNaoEncontradoException;
import com.citizentech.semaforo.exception.RegraNegocioException;
import com.citizentech.semaforo.repository.CruzamentoRepository;
import com.citizentech.semaforo.repository.DecisaoRepository;
import com.citizentech.semaforo.repository.LeituraRepository;
import com.citizentech.semaforo.repository.ViaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CruzamentoService {

    private final CruzamentoRepository cruzamentos;
    private final ViaRepository vias;
    private final LeituraRepository leituras;
    private final DecisaoRepository decisoes;

    public CruzamentoService(CruzamentoRepository cruzamentos, ViaRepository vias,
                             LeituraRepository leituras, DecisaoRepository decisoes) {
        this.cruzamentos = cruzamentos;
        this.vias = vias;
        this.leituras = leituras;
        this.decisoes = decisoes;
    }

    @Transactional(readOnly = true)
    public Page<CruzamentoResponse> listar(Pageable pageable) {
        return cruzamentos.findAll(pageable).map(CruzamentoResponse::de);
    }

    @Transactional(readOnly = true)
    public CruzamentoResponse buscar(Long id) {
        return CruzamentoResponse.de(obter(id));
    }

    @Transactional
    public CruzamentoResponse criar(CruzamentoRequest req) {
        Cruzamento c = new Cruzamento();
        aplicar(c, req);
        return CruzamentoResponse.de(cruzamentos.save(c));
    }

    @Transactional
    public CruzamentoResponse atualizar(Long id, CruzamentoRequest req) {
        Cruzamento c = obter(id);
        aplicar(c, req);
        return CruzamentoResponse.de(cruzamentos.save(c));
    }

    @Transactional
    public void excluir(Long id) {
        Cruzamento c = obter(id);
        cruzamentos.delete(c);
        cruzamentos.flush(); // se houver vias/decisões/relatos ligados, o handler global devolve 409
    }

    /** Visão do cidadão: fila atual por via + última decisão (com a explicação gerada pela IA). */
    @Transactional(readOnly = true)
    public StatusCruzamentoResponse status(Long id) {
        Cruzamento c = obter(id);

        List<ViaStatus> viasStatus = vias.findByCruzamentoIdOrderByIdAsc(id).stream()
                .map(v -> leituras.findFirstByViaIdOrderByRegistradoEmDescIdDesc(v.getId())
                        .map(l -> viaStatus(v, l))
                        .orElse(new ViaStatus(v.getId(), v.getNome(), v.getSentido(), null, null, null)))
                .toList();

        DecisaoResponse ultima = decisoes.findFirstByCruzamentoIdOrderByCriadoEmDescIdDesc(id)
                .map(DecisaoResponse::de)
                .orElse(null);

        return new StatusCruzamentoResponse(c.getId(), c.getNome(), c.getEndereco(), c.isAtivo(), viasStatus, ultima);
    }

    private ViaStatus viaStatus(Via v, LeituraTrafego l) {
        return new ViaStatus(v.getId(), v.getNome(), v.getSentido(),
                l.getQuantidadeVeiculos(), l.getTamanhoFilaMetros(), l.getRegistradoEm());
    }

    private Cruzamento obter(Long id) {
        return cruzamentos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cruzamento " + id + " não encontrado"));
    }

    private void aplicar(Cruzamento c, CruzamentoRequest req) {
        if (req.tempoMinVerde() > req.tempoFixo() || req.tempoFixo() > req.tempoMaxVerde()) {
            throw new RegraNegocioException("Os tempos devem respeitar: tempoMinVerde <= tempoFixo <= tempoMaxVerde");
        }
        c.setNome(req.nome().trim());
        c.setEndereco(req.endereco().trim());
        c.setLatitude(req.latitude());
        c.setLongitude(req.longitude());
        c.setAtivo(req.ativo() == null || req.ativo());
        c.setTempoMinVerde(req.tempoMinVerde());
        c.setTempoMaxVerde(req.tempoMaxVerde());
        c.setTempoFixo(req.tempoFixo());
    }
}
