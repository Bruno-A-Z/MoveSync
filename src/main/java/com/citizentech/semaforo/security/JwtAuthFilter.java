package com.citizentech.semaforo.security;

import com.citizentech.semaforo.domain.Perfil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Lê "Authorization: Bearer <jwt>", valida e popula o SecurityContext com o perfil do usuário. */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.validar(cabecalho.substring(7));
                Perfil perfil = Perfil.valueOf(claims.get("perfil", String.class));
                Long id = ((Number) claims.get("uid")).longValue();

                UsuarioLogado principal = new UsuarioLogado(id, claims.getSubject(), perfil);
                var autenticacao = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name())));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            } catch (JwtException | IllegalArgumentException | NullPointerException e) {
                // token inválido/expirado: segue sem autenticação e o EntryPoint responde 401
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
