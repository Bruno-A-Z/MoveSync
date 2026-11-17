package com.citizentech.semaforo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
                                           RestSecurityHandlers handlers) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .authorizeHttpRequests(a -> a
                        // público
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/dev/**").permitAll()
                        // leitura de cruzamentos, vias e decisões: qualquer usuário autenticado
                        .requestMatchers(HttpMethod.GET, "/api/cruzamentos/**", "/api/vias/**",
                                "/api/decisoes/**").authenticated()
                        // escrita/gestão: somente GESTOR
                        .requestMatchers("/api/cruzamentos/**", "/api/vias/**", "/api/leituras/**").hasRole("GESTOR")
                        // relatos: cidadão cria e vê os seus; gestor vê todos e muda o status
                        .requestMatchers(HttpMethod.POST, "/api/relatos").hasRole("CIDADAO")
                        .requestMatchers(HttpMethod.GET, "/api/relatos/meus").hasRole("CIDADAO")
                        .requestMatchers("/api/relatos/**").hasRole("GESTOR")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
