package com.citizentech.semaforo.repository;

import com.citizentech.semaforo.domain.Perfil;
import com.citizentech.semaforo.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Usuario> findByPerfil(Perfil perfil, Pageable pageable);

    @Query("""
            select u from Usuario u
            where lower(u.nome) like lower(concat('%', :busca, '%'))
               or lower(u.email) like lower(concat('%', :busca, '%'))
               or cast(u.id as string) = :busca
            """)
    Page<Usuario> buscar(@Param("busca") String busca, Pageable pageable);

    @Query("""
            select u from Usuario u
            where u.perfil = :perfil
              and (lower(u.nome) like lower(concat('%', :busca, '%'))
                   or lower(u.email) like lower(concat('%', :busca, '%'))
                   or cast(u.id as string) = :busca)
            """)
    Page<Usuario> buscarPorPerfil(@Param("perfil") Perfil perfil, @Param("busca") String busca, Pageable pageable);
}