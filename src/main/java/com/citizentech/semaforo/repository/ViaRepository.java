package com.citizentech.semaforo.repository;

import com.citizentech.semaforo.domain.Via;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ViaRepository extends JpaRepository<Via, Long> {

    Page<Via> findByCruzamentoId(Long cruzamentoId, Pageable pageable);

    List<Via> findByCruzamentoIdOrderByIdAsc(Long cruzamentoId);

    @Query("select v from Via v join fetch v.cruzamento where v.id = :id")
    Optional<Via> findComCruzamentoById(@Param("id") Long id);
}
