package com.citizentech.semaforo.service;

import com.citizentech.semaforo.domain.ModoDecisao;
import com.citizentech.semaforo.service.MotorDecisao.Resultado;
import com.citizentech.semaforo.service.MotorDecisao.SituacaoVia;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotorDecisaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T12:00:00Z");
    private static final Duration VALIDADE = Duration.ofSeconds(120);
    private static final int MIN = 10, MAX = 60, FIXO = 30;

    private SituacaoVia via(long id, int veiculos, int fila, boolean emergencia, Instant leitura) {
        return new SituacaoVia(id, "Via " + id, veiculos, fila, emergencia, leitura);
    }

    private Resultado decidir(List<SituacaoVia> vias, Long ultima) {
        return MotorDecisao.decidir(vias, MIN, MAX, FIXO, AGORA, VALIDADE, ultima);
    }

    @Test
    void abreAViaDeMaiorDemandaComTempoProporcional() {
        // demandas: 5+12/6=7 | 20+60/6=30 | 2+0=2  => total 39; via 2 = 76,9% => 10 + 0,769*50 = 48
        Instant fresca = AGORA.minusSeconds(5);
        Resultado r = decidir(List.of(
                via(1, 5, 12, false, fresca),
                via(2, 20, 60, false, fresca),
                via(3, 2, 0, false, fresca)), null);

        assertEquals(2L, r.viaId());
        assertEquals(48, r.tempoVerdeSegundos());
        assertEquals(ModoDecisao.ADAPTATIVO, r.modo());
    }

    @Test
    void tempoNuncaPassaDoMaximoNemFicaAbaixoDoMinimo() {
        Instant fresca = AGORA.minusSeconds(5);

        Resultado unicaVia = decidir(List.of(via(1, 10, 30, false, fresca)), null);
        assertEquals(MAX, unicaVia.tempoVerdeSegundos());

        Resultado semDemanda = decidir(List.of(via(1, 0, 0, false, fresca), via(2, 0, 0, false, fresca)), 1L);
        assertEquals(MIN, semDemanda.tempoVerdeSegundos());
        assertEquals(2L, semDemanda.viaId()); // sem demanda: apenas reveza
    }

    @Test
    void veiculoDeEmergenciaRecebeTempoMaximo() {
        Instant fresca = AGORA.minusSeconds(5);
        Resultado r = decidir(List.of(
                via(1, 40, 120, false, fresca),
                via(2, 1, 6, true, fresca)), null);

        assertEquals(2L, r.viaId());
        assertEquals(MAX, r.tempoVerdeSegundos());
        assertEquals(ModoDecisao.EMERGENCIA, r.modo());
    }

    @Test
    void emergenciaComLeituraAntigaEIgnorada() {
        Instant antiga = AGORA.minusSeconds(600);
        Resultado r = decidir(List.of(
                via(1, 10, 0, false, AGORA.minusSeconds(5)),
                via(2, 1, 0, true, antiga)), null);

        assertEquals(ModoDecisao.FALLBACK_FIXO, r.modo());
    }

    @Test
    void sensorForaDoArVoltaAoTempoFixoERevezaAsVias() {
        Instant fresca = AGORA.minusSeconds(5);
        Instant antiga = AGORA.minusSeconds(300);
        List<SituacaoVia> vias = List.of(
                via(1, 30, 60, false, fresca),
                via(2, 5, 0, false, antiga),
                via(3, 8, 0, false, fresca));

        Resultado primeira = decidir(vias, null);
        assertEquals(1L, primeira.viaId());
        assertEquals(FIXO, primeira.tempoVerdeSegundos());
        assertEquals(ModoDecisao.FALLBACK_FIXO, primeira.modo());

        assertEquals(2L, decidir(vias, 1L).viaId());
        assertEquals(1L, decidir(vias, 3L).viaId()); // volta ao início
    }

    @Test
    void viaSemNenhumaLeituraTambemAtivaOFallback() {
        Resultado r = decidir(List.of(
                via(1, 10, 0, false, AGORA.minusSeconds(5)),
                via(2, 0, 0, false, null)), null);

        assertEquals(ModoDecisao.FALLBACK_FIXO, r.modo());
    }

    @Test
    void cruzamentoSemViasEInvalido() {
        assertThrows(IllegalArgumentException.class, () -> decidir(List.of(), null));
    }
}
