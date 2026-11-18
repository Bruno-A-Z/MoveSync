package com.citizentech.semaforo.repository;

import com.citizentech.semaforo.domain.Perfil;
import com.citizentech.semaforo.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Usuario> findByPerfil(Perfil perfil, Pageable pageable);
}