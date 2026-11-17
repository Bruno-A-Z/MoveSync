package com.citizentech.semaforo.repository;

import com.citizentech.semaforo.domain.Cruzamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CruzamentoRepository extends JpaRepository<Cruzamento, Long> {
}
