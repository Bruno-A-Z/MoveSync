package com.citizentech.semaforo.controller;

import com.citizentech.semaforo.config.DataLoader;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Painel de testes para o desenvolvedor (Thymeleaf), em /dev.
 * A página só entrega HTML/JS: todos os dados vêm da API real, com JWT, então as regras de perfil continuam valendo.
 * Desligue no deploy com DEV_UI=false (app.dev-ui.enabled=false).
 */
@Hidden
@Controller
@RequestMapping("/dev")
@ConditionalOnProperty(prefix = "app.dev-ui", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevUiController {

    public record UsuarioTeste(String perfil, String email, String senha) {
    }

    @GetMapping({"", "/"})
    public String painel(Model model) {
        model.addAttribute("titulo", "Semáforo Inteligente · Painel de testes");
        model.addAttribute("usuariosTeste", List.of(
                new UsuarioTeste("GESTOR", DataLoader.GESTOR_EMAIL, DataLoader.GESTOR_SENHA),
                new UsuarioTeste("CIDADAO", DataLoader.CIDADAO_EMAIL, DataLoader.CIDADAO_SENHA)));
        return "dev/index";
    }
}
