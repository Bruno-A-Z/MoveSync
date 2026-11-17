package com.citizentech.semaforo.service;

import com.citizentech.semaforo.ai.IaService;
import com.citizentech.semaforo.domain.*;
import com.citizentech.semaforo.exception.RecursoNaoEncontradoException;
import com.citizentech.semaforo.messaging.LeituraEvent;
import com.citizentech.semaforo.repository.DecisaoRepository;
import com.citizentech.semaforo.repository.LeituraRepository;
import com.citizentech.semaforo.repository.ViaRepository;
import com.citizentech.semaforo.service.MotorDecisao.Resultado;
import com.citizentech.semaforo.service.MotorDecisao.SituacaoVia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Coração do sistema: chamado pelo consumidor do RabbitMQ a cada leitura de sensor.
 * Persiste a leitura, roda o MotorDecisao e, a cada janela de decisão (ou quando o modo muda, ex.: emergência),
 * grava a DecisaoSemaforo com a explicação gerada pelo LLM.
 *
 * A decisão é gravada na hora com texto padrão; a explicação do LLM chega depois, em segundo plano.
 * Não é @Transactional de propósito: cada operação de banco é curta e independente.
 */
@Service
public class TrafegoService {

    private static final Logger log = LoggerFactory.getLogger(TrafegoService.class);

    private final ViaRepository vias;
    private final LeituraRepository leituras;
    private final DecisaoRepository decisoes;
    private final IaService ia;
    private final Duration validadeLeitura;
    private final Duration janelaDecisao;

    public TrafegoService(ViaRepository vias, LeituraRepository leituras, DecisaoRepository decisoes, IaService ia,
                          @Value("${app.semaforo.validade-leitura-segundos}") long validadeSegundos,
                          @Value("${app.semaforo.intervalo-minimo-decisao-segundos}") long janelaSegundos) {
        this.vias = vias;
        this.leituras = leituras;
        this.decisoes = decisoes;
        this.ia = ia;
        this.validadeLeitura = Duration.ofSeconds(validadeSegundos);
        this.janelaDecisao = Duration.ofSeconds(janelaSegundos);
    }

    public void processarLeitura(LeituraEvent evento) {
        Via via = vias.findComCruzamentoById(evento.viaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Via " + evento.viaId() + " não encontrada"));
        Cruzamento cruzamento = via.getCruzamento();
        Instant agora = Instant.now();

        LeituraTrafego leitura = new LeituraTrafego();
        leitura.setVia(via);
        leitura.setQuantidadeVeiculos(evento.quantidadeVeiculos());
        leitura.setTamanhoFilaMetros(evento.tamanhoFilaMetros());
        leitura.setVeiculoEmergencia(Boolean.TRUE.equals(evento.veiculoEmergencia()));
        leitura.setRegistradoEm(agora); // ESP32 normalmente não tem relógio confiável: usa a hora do servidor
        leituras.save(leitura);

        if (!cruzamento.isAtivo()) {
            return;
        }

        List<Via> viasDoCruzamento = vias.findByCruzamentoIdOrderByIdAsc(cruzamento.getId());
        List<SituacaoVia> situacao = viasDoCruzamento.stream().map(this::situacaoDa).toList();

        DecisaoSemaforo ultima = decisoes.findFirstByCruzamentoIdOrderByCriadoEmDescIdDesc(cruzamento.getId())
                .orElse(null);
        Long ultimaViaId = ultima == null ? null : ultima.getViaPrioritaria().getId();

        Resultado resultado = MotorDecisao.decidir(situacao, cruzamento.getTempoMinVerde(),
                cruzamento.getTempoMaxVerde(), cruzamento.getTempoFixo(), agora, validadeLeitura, ultimaViaId);

        boolean janelaVencida = ultima == null || Duration.between(ultima.getCriadoEm(), agora).compareTo(janelaDecisao) >= 0;
        boolean mudouDeModo = ultima != null && ultima.getModo() != resultado.modo();
        if (!janelaVencida && !mudouDeModo) {
            return; // ainda dentro da janela da decisão anterior e sem mudança de modo (ex.: entrou emergência)
        }

        Via viaEscolhida = viasDoCruzamento.stream()
                .filter(v -> v.getId().equals(resultado.viaId()))
                .findFirst().orElseThrow();

        DecisaoSemaforo decisao = new DecisaoSemaforo();
        decisao.setCruzamento(cruzamento);
        decisao.setViaPrioritaria(viaEscolhida);
        decisao.setTempoVerdeSegundos(resultado.tempoVerdeSegundos());
        decisao.setModo(resultado.modo());
        decisao.setExplicacao(ia.explicacaoPadrao(resultado)); // grava já, sem esperar o LLM
        decisao.setCriadoEm(agora);
        DecisaoSemaforo salva = decisoes.save(decisao);

        // a IA troca a explicação padrão em segundo plano (não bloqueia o consumidor do RabbitMQ)
        ia.enriquecerDecisao(salva.getId(), cruzamento.getNome(), situacao, resultado);

        log.info("Cruzamento {}: verde para '{}' por {}s ({})", cruzamento.getId(), resultado.nomeVia(),
                resultado.tempoVerdeSegundos(), resultado.modo());
    }

    private SituacaoVia situacaoDa(Via v) {
        return leituras.findFirstByViaIdOrderByRegistradoEmDescIdDesc(v.getId())
                .map(l -> new SituacaoVia(v.getId(), v.getNome(), l.getQuantidadeVeiculos(),
                        l.getTamanhoFilaMetros(), l.isVeiculoEmergencia(), l.getRegistradoEm()))
                .orElse(new SituacaoVia(v.getId(), v.getNome(), 0, 0, false, null));
    }
}
