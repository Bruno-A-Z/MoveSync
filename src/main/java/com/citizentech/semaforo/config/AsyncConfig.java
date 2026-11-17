package com.citizentech.semaforo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Threads dedicadas às chamadas ao LLM. Assim, a lentidão ou a queda do provedor de IA
 * nunca trava o consumidor do RabbitMQ nem a resposta da API.
 * A IA é opcional: se a fila encher, a tarefa é descartada e o texto padrão permanece.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "iaExecutor")
    public Executor iaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ia-");
        executor.setRejectedExecutionHandler((tarefa, pool) ->
                log.warn("Fila da IA cheia: tarefa descartada (o texto padrão foi mantido)"));
        return executor;
    }
}
