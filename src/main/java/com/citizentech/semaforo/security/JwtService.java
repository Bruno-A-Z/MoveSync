package com.citizentech.semaforo.security;

import com.citizentech.semaforo.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    /** Valida assinatura e expiração; lança JwtException se o token for inválido. */
    public Claims validar(String token) {
        return Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
    }

    public long getExpiracaoSegundos() {
        return expiracaoMinutos * 60;
    }
}
