package com.citizentech.semaforo.service;

import com.citizentech.semaforo.domain.Perfil;
import com.citizentech.semaforo.domain.Usuario;
import com.citizentech.semaforo.dto.PerfilRequest;
import com.citizentech.semaforo.dto.UsuarioRequest;
import com.citizentech.semaforo.dto.UsuarioResponse;
import com.citizentech.semaforo.exception.ConflitoException;
import com.citizentech.semaforo.exception.RecursoNaoEncontradoException;
import com.citizentech.semaforo.exception.RegraNegocioException;
import com.citizentech.semaforo.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Perfil perfil, Pageable pageable) {
        Page<Usuario> pagina = perfil == null
                ? usuarios.findAll(pageable)
                : usuarios.findByPerfil(perfil, pageable);
        return pagina.map(UsuarioResponse::de);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(obter(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest req) {
        if (req.senha() == null || req.senha().isBlank()) {
            throw new RegraNegocioException("A senha é obrigatória ao criar um usuário");
        }
        String email = normalizar(req.email());
        if (usuarios.existsByEmail(email)) {
            throw new ConflitoException("E-mail já cadastrado");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenha(encoder.encode(req.senha()));
        u.setPerfil(req.perfil());
        return UsuarioResponse.de(usuarios.save(u));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest req, Long idLogado) {
        Usuario u = obter(id);
        if (u.getId().equals(idLogado) && req.perfil() != Perfil.GESTOR) {
            throw new RegraNegocioException("Você não pode remover o seu próprio perfil de GESTOR");
        }
        aplicarDadosBasicos(u, req.nome(), req.email(), req.senha());
        u.setPerfil(req.perfil());
        return UsuarioResponse.de(usuarios.save(u));
    }

    @Transactional
    public void excluir(Long id, Long idLogado) {
        if (id.equals(idLogado)) {
            throw new RegraNegocioException("Você não pode excluir a sua própria conta");
        }
        usuarios.delete(obter(id));
        usuarios.flush(); // usuário com relatos => 409 pelo handler global
    }

    @Transactional(readOnly = true)
    public UsuarioResponse meuPerfil(Long idLogado) {
        return UsuarioResponse.de(obter(idLogado));
    }

    @Transactional
    public UsuarioResponse atualizarMeuPerfil(Long idLogado, PerfilRequest req) {
        Usuario u = obter(idLogado);
        aplicarDadosBasicos(u, req.nome(), req.email(), req.senha());
        return UsuarioResponse.de(usuarios.save(u));
    }

    private void aplicarDadosBasicos(Usuario u, String nome, String email, String senha) {
        String emailNormalizado = normalizar(email);
        if (usuarios.existsByEmailAndIdNot(emailNormalizado, u.getId())) {
            throw new ConflitoException("E-mail já cadastrado");
        }
        u.setNome(nome.trim());
        u.setEmail(emailNormalizado);
        if (senha != null && !senha.isBlank()) {
            u.setSenha(encoder.encode(senha));
        }
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase();
    }

    private Usuario obter(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado"));
    }
}