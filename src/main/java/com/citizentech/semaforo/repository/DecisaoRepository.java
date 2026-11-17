package com.citizentech.semaforo.repository;

import com.citizentech.semaforo.domain.DecisaoSemaforo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface DecisaoRepository extends JpaRepository<DecisaoSemaforo, Long> {

    @EntityGraph(attributePaths = "viaPrioritaria")
    Page<DecisaoSemaforo> findByCruzamentoId(Long cruzamentoId, Pageable pageable);

    @EntityGraph(attributePaths = "viaPrioritaria")
    Optional<DecisaoSemaforo> findFirstByCruzamentoIdOrderByCriadoEmDescIdDesc(Long cruzamentoId);

    /** Chamado em segundo plano quando o LLM responde. */
    @Modifying
    @Transactional
    @Query("update DecisaoSemaforo d set d.explicacao = :texto where d.id = :id")
    int atualizarExplicacao(@Param("id") Long id, @Param("texto") String texto);
}
