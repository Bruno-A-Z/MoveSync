package com.citizentech.semaforo.repository;

import com.citizentech.semaforo.domain.CategoriaRelato;
import com.citizentech.semaforo.domain.Relato;
import com.citizentech.semaforo.domain.StatusRelato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RelatoRepository extends JpaRepository<Relato, Long> {

    @EntityGraph(attributePaths = "cruzamento")
    Page<Relato> findByUsuarioId(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = "cruzamento")
    Page<Relato> findByStatus(StatusRelato status, Pageable pageable);

    @EntityGraph(attributePaths = "cruzamento")
    Page<Relato> findAll(Pageable pageable);

    /** Chamado em segundo plano quando o LLM classifica o relato. */
    @Modifying
    @Transactional
    @Query("update Relato r set r.categoria = :categoria, r.gravidade = :gravidade, r.resumoIa = :resumo where r.id = :id")
    int atualizarClassificacao(@Param("id") Long id, @Param("categoria") CategoriaRelato categoria,
                               @Param("gravidade") int gravidade, @Param("resumo") String resumo);
}
