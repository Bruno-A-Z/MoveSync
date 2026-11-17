package com.citizentech.semaforo.service;

import com.citizentech.semaforo.ai.ClassificacaoRelato;
import com.citizentech.semaforo.ai.IaService;
import com.citizentech.semaforo.domain.CategoriaRelato;
import com.citizentech.semaforo.domain.Cruzamento;
import com.citizentech.semaforo.domain.Relato;
import com.citizentech.semaforo.domain.StatusRelato;
import com.citizentech.semaforo.domain.Usuario;
import com.citizentech.semaforo.dto.RelatoRequest;
import com.citizentech.semaforo.dto.RelatoResponse;
import com.citizentech.semaforo.exception.RecursoNaoEncontradoException;
import com.citizentech.semaforo.repository.CruzamentoRepository;
import com.citizentech.semaforo.repository.RelatoRepository;
import com.citizentech.semaforo.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RelatoService {

    private final RelatoRepository relatos;
    private final CruzamentoRepository cruzamentos;
    private final UsuarioRepository usuarios;
    private final IaService ia;

    public RelatoService(RelatoRepository relatos, CruzamentoRepository cruzamentos,
                         UsuarioRepository usuarios, IaService ia) {
        this.relatos = relatos;
        this.cruzamentos = cruzamentos;
        this.usuarios = usuarios;
        this.ia = ia;
    }

    /** Responde na hora com a classificação padrão; a IA atualiza categoria, gravidade e resumo em segundo plano. */
    public RelatoResponse criar(Long usuarioId, RelatoRequest req) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        Cruzamento cruzamento = cruzamentos.findById(req.cruzamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cruzamento " + req.cruzamentoId() + " não encontrado"));

        // classificação padrão agora; o LLM refina em segundo plano
        ClassificacaoRelato classificacao = ia.classificacaoPadrao(req.descricao().trim());

        Relato r = new Relato();
        r.setUsuario(usuario);
        r.setCruzamento(cruzamento);
        r.setDescricao(req.descricao().trim());
        r.setCategoria(CategoriaRelato.deTexto(classificacao.categoria()));
        r.setGravidade(classificacao.gravidade() == null ? 3 : classificacao.gravidade());
        r.setResumoIa(classificacao.resumo());
        r.setStatus(StatusRelato.ABERTO);
        r.setCriadoEm(Instant.now());
        Relato salvo = relatos.save(r);
        ia.enriquecerRelato(salvo.getId(), salvo.getDescricao());
        return RelatoResponse.de(salvo);
    }

    @Transactional(readOnly = true)
    public Page<RelatoResponse> listarDoUsuario(Long usuarioId, Pageable pageable) {
        return relatos.findByUsuarioId(usuarioId, pageable).map(RelatoResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<RelatoResponse> listarTodos(StatusRelato status, Pageable pageable) {
        Page<Relato> pagina = status == null ? relatos.findAll(pageable) : relatos.findByStatus(status, pageable);
        return pagina.map(RelatoResponse::de);
    }

    @Transactional
    public RelatoResponse atualizarStatus(Long id, StatusRelato status) {
        Relato r = relatos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relato " + id + " não encontrado"));
        r.setStatus(status);
        return RelatoResponse.de(relatos.save(r));
    }
}
