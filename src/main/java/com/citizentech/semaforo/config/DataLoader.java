package com.citizentech.semaforo.config;

import com.citizentech.semaforo.domain.Cruzamento;
import com.citizentech.semaforo.domain.Perfil;
import com.citizentech.semaforo.domain.Usuario;
import com.citizentech.semaforo.domain.Via;
import com.citizentech.semaforo.repository.CruzamentoRepository;
import com.citizentech.semaforo.repository.UsuarioRepository;
import com.citizentech.semaforo.repository.ViaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Carga inicial: usuários de teste e um cruzamento de exemplo (só se o banco estiver vazio). */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    public static final String GESTOR_EMAIL = "gestor@citizentech.com";
    public static final String GESTOR_SENHA = "Gestor@123";
    public static final String CIDADAO_EMAIL = "cidadao@citizentech.com";
    public static final String CIDADAO_SENHA = "Cidadao@123";

    private final UsuarioRepository usuarios;
    private final CruzamentoRepository cruzamentos;
    private final ViaRepository vias;
    private final PasswordEncoder encoder;

    public DataLoader(UsuarioRepository usuarios, CruzamentoRepository cruzamentos,
                      ViaRepository vias, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.cruzamentos = cruzamentos;
        this.vias = vias;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() == 0) {
            usuarios.save(usuario("Gestor de Trânsito", GESTOR_EMAIL, GESTOR_SENHA, Perfil.GESTOR));
            usuarios.save(usuario("Maria Cidadã", CIDADAO_EMAIL, CIDADAO_SENHA, Perfil.CIDADAO));
            log.info("Usuários de teste criados: gestor@citizentech.com / cidadao@citizentech.com");
        }

        if (cruzamentos.count() == 0) {
            Cruzamento c = new Cruzamento();
            c.setNome("Av. Paulista x Rua Augusta");
            c.setEndereco("Av. Paulista, 1000 - Bela Vista, São Paulo - SP");
            c.setLatitude(-23.5646);
            c.setLongitude(-46.6527);
            c.setAtivo(true);
            c.setTempoMinVerde(10);
            c.setTempoMaxVerde(60);
            c.setTempoFixo(30);
            c = cruzamentos.save(c);

            vias.save(via(c, "Av. Paulista", "sentido Paraíso"));
            vias.save(via(c, "Rua Augusta", "sentido Centro"));
            vias.save(via(c, "Rua Pamplona", "sentido Jardins"));
            log.info("Cruzamento de exemplo criado (id={}) com 3 vias", c.getId());
        }
    }

    private Usuario usuario(String nome, String email, String senha, Perfil perfil) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(encoder.encode(senha));
        u.setPerfil(perfil);
        return u;
    }

    private Via via(Cruzamento c, String nome, String sentido) {
        Via v = new Via();
        v.setCruzamento(c);
        v.setNome(nome);
        v.setSentido(sentido);
        return v;
    }
}
