package com.citizentech.semaforo.service;

import com.citizentech.semaforo.domain.Perfil;
import com.citizentech.semaforo.domain.Usuario;
import com.citizentech.semaforo.dto.LoginRequest;
import com.citizentech.semaforo.dto.RegistroRequest;
import com.citizentech.semaforo.dto.TokenResponse;
import com.citizentech.semaforo.exception.ConflitoException;
import com.citizentech.semaforo.repository.UsuarioRepository;
import com.citizentech.semaforo.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    /** Auto-cadastro: sempre cria um CIDADAO (gestores são criados diretamente na base). */
    public TokenResponse registrar(RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmail(email)) {
            throw new ConflitoException("E-mail já cadastrado");
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenha(encoder.encode(req.senha()));
        u.setPerfil(Perfil.CIDADAO);
        return token(usuarios.save(u));
    }

    public TokenResponse login(LoginRequest req) {
        Usuario u = usuarios.findByEmail(req.email().trim().toLowerCase())
                .filter(x -> encoder.matches(req.senha(), x.getSenha()))
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos"));
        return token(u);
    }

    private TokenResponse token(Usuario u) {
        return new TokenResponse(jwtService.gerar(u), "Bearer", jwtService.getExpiracaoSegundos(),
                u.getNome(), u.getPerfil());
    }
}
