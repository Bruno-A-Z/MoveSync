package com.citizentech.semaforo.service;

import com.citizentech.semaforo.domain.ModoDecisao;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Algoritmo de decisão do semáforo (classe pura, fácil de testar).
 *
 * Regras, em ordem de prioridade:
 *  1. EMERGENCIA: via com veículo de emergência (leitura recente) recebe o tempo máximo de verde.
 *  2. FALLBACK_FIXO: se alguma via está sem leitura recente (sensor fora do ar), volta ao tempo fixo
 *     e reveza as vias em sequência (fail-safe: o sistema nunca "congela" um semáforo).
 *  3. ADAPTATIVO: abre a via de maior demanda; o tempo de verde é proporcional à fatia de demanda dela,
 *     sempre entre o mínimo e o máximo configurados para o cruzamento.
 */
public final class MotorDecisao {

    /** Metros de fila equivalentes a um veículo (média de ~6 m por carro parado). */
    static final double METROS_POR_VEICULO = 6.0;

    private MotorDecisao() {
    }

    public record SituacaoVia(Long viaId, String nome, int veiculos, int filaMetros,
                              boolean emergencia, Instant atualizadoEm) {

        double demanda() {
            return veiculos + filaMetros / METROS_POR_VEICULO;
        }
    }

    public record Resultado(Long viaId, String nomeVia, int tempoVerdeSegundos, ModoDecisao modo) {
    }

    public static Resultado decidir(List<SituacaoVia> vias, int tempoMin, int tempoMax, int tempoFixo,
                                    Instant agora, Duration validadeLeitura, Long ultimaViaId) {
        if (vias == null || vias.isEmpty()) {
            throw new IllegalArgumentException("O cruzamento precisa ter ao menos uma via");
        }

        // 1) emergência (somente com leitura recente)
        Optional<SituacaoVia> emergencia = vias.stream()
                .filter(v -> v.emergencia() && !desatualizada(v, agora, validadeLeitura))
                .max(Comparator.comparingDouble(SituacaoVia::demanda));
        if (emergencia.isPresent()) {
            SituacaoVia v = emergencia.get();
            return new Resultado(v.viaId(), v.nome(), tempoMax, ModoDecisao.EMERGENCIA);
        }

        // 2) fail-safe: algum sensor sem dados confiáveis
        boolean semDadosConfiaveis = vias.stream().anyMatch(v -> desatualizada(v, agora, validadeLeitura));
        if (semDadosConfiaveis) {
            SituacaoVia proxima = proxima(vias, ultimaViaId);
            return new Resultado(proxima.viaId(), proxima.nome(), tempoFixo, ModoDecisao.FALLBACK_FIXO);
        }

        // 3) adaptativo
        double total = vias.stream().mapToDouble(SituacaoVia::demanda).sum();
        if (total <= 0) {
            SituacaoVia proxima = proxima(vias, ultimaViaId);
            return new Resultado(proxima.viaId(), proxima.nome(), tempoMin, ModoDecisao.ADAPTATIVO);
        }

        SituacaoVia escolhida = vias.stream().max(Comparator.comparingDouble(SituacaoVia::demanda)).orElseThrow();
        double fatia = escolhida.demanda() / total;
        int verde = (int) Math.round(tempoMin + fatia * (tempoMax - tempoMin));
        verde = Math.max(tempoMin, Math.min(tempoMax, verde));
        return new Resultado(escolhida.viaId(), escolhida.nome(), verde, ModoDecisao.ADAPTATIVO);
    }

    private static boolean desatualizada(SituacaoVia v, Instant agora, Duration validade) {
        return v.atualizadoEm() == null || Duration.between(v.atualizadoEm(), agora).compareTo(validade) > 0;
    }

    /** Próxima via na sequência (rodízio) depois da última que recebeu verde. */
    private static SituacaoVia proxima(List<SituacaoVia> vias, Long ultimaViaId) {
        if (ultimaViaId == null) {
            return vias.get(0);
        }
        for (int i = 0; i < vias.size(); i++) {
            if (vias.get(i).viaId().equals(ultimaViaId)) {
                return vias.get((i + 1) % vias.size());
            }
        }
        return vias.get(0);
    }
}
