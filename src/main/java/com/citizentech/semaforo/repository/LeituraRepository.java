package com.citizentech.semaforo.repository;

import com.citizentech.semaforo.domain.LeituraTrafego;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeituraRepository extends JpaRepository<LeituraTrafego, Long> {

    Page<LeituraTrafego> findByViaId(Long viaId, Pageable pageable);

    Optional<LeituraTrafego> findFirstByViaIdOrderByRegistradoEmDescIdDesc(Long viaId);
}
