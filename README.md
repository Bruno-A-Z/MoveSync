# CitizenTech — Semáforo Inteligente (Global Solution 2026/2 · Java Advanced)

Backend em **Java 21 + Spring Boot 3.4** que recebe leituras de sensores/câmeras (fila de veículos por via),
decide qual via abrir e por quanto tempo, e **explica a decisão ao cidadão com IA generativa (Spring AI)**.
O app Mobile (React Native) consome esta mesma API.

## O problema e o cidadão
Semáforos de tempo fixo dão o mesmo verde para uma rua vazia e para uma avenida congestionada.
Beneficiados: motoristas, motociclistas, passageiros de transporte público, pedestres e serviços de emergência.
O cidadão **vê a situação do cruzamento e o motivo da decisão**, e **reporta problemas** (semáforo apagado,
travessia insegura, falta de acessibilidade) que a IA classifica e prioriza para o gestor.

## Requisitos da disciplina → onde estão no código
| Requisito | Onde |
|---|---|
| 1. API REST, ≥ 3 entidades relacionadas, CRUD JPA, paginação | `domain/`, `repository/`, `controller/` (6 entidades; `PagedModel` em todas as listagens) |
| 2. Bean Validation + `@RestControllerAdvice` | `dto/*Request`, `exception/GlobalExceptionHandler` (formato único `ApiError`) |
| 3. Spring Security + JWT, 2 papéis | `security/` — `CIDADAO` e `GESTOR`, regras por rota em `SecurityConfig` |
| 4. Spring AI integrado ao fluxo | `ai/IaService` (em segundo plano, `@Async`): (a) explica cada decisão do semáforo, (b) classifica relatos do cidadão |
| 5. **Arquitetura distribuída — opção A (RabbitMQ)** | `config/RabbitConfig`, `messaging/` — evento publicado e consumido de forma assíncrona |
| 6. Swagger, README, integração com Mobile | `/swagger-ui.html`, este arquivo |

## Arquitetura (opção A — RabbitMQ)
``` 
ESP32 / simulador ──MQTT──► RabbitMQ (amq.topic, sensores.#) ─┐
                                                              ├─► fila trafego.leituras ─► LeituraConsumer
API REST  POST /api/leituras ──► trafego.exchange ────────────┘        │
                                                                       ▼
                                          TrafegoService: salva leitura → MotorDecisao → IaService (LLM) → salva DecisaoSemaforo
                                                                       │
              App do cidadão / painel do gestor  ◄── GET /api/cruzamentos/{id}/status ◄─┘
```
O mesmo consumidor atende o REST e o MQTT (o plugin MQTT do RabbitMQ converte o tópico `sensores/1/2` na routing key `sensores.1.2`).

### IA em segundo plano (resiliência)
A decisão do semáforo vem do `MotorDecisao` (regras), **não da IA**. A decisão e o relato são gravados na hora com um texto padrão, e o `IaService` chama o LLM em threads separadas (`iaExecutor`) para trocar esse texto pela explicação ou classificação gerada. Assim, lentidão, falta de saldo ou erro 503 do provedor nunca atrasam a API; no pior caso o texto padrão permanece. Os logs `Decisão N: explicação atualizada pela IA` e `Relato N: classificação atualizada pela IA` confirmam que o LLM respondeu.

### Regras do `MotorDecisao` (classe pura, testada em `MotorDecisaoTest`)
1. **EMERGENCIA** — via com veículo de emergência (leitura recente) recebe o tempo máximo de verde.
2. **FALLBACK_FIXO** — se algum sensor está sem leitura recente (`validade-leitura-segundos`, padrão 120 s), o sistema volta ao tempo fixo e reveza as vias. O semáforo nunca depende 100% do sensor.
3. **ADAPTATIVO** — demanda da via = `veículos + fila(m)/6`; abre a via de maior demanda com verde proporcional à sua fatia, sempre entre `tempoMinVerde` e `tempoMaxVerde` do cruzamento.

Uma nova decisão é gravada a cada janela (`intervalo-minimo-decisao-segundos`, padrão 30 s) ou quando o modo muda (ex.: entrou emergência). A API **recomenda** e registra; não aciona semáforo físico.

## Como rodar
Pré-requisitos: **JDK 21**, **Maven 3.9+**, **Docker** (para o RabbitMQ).

```bash
# 1) RabbitMQ (AMQP 5672, painel 15672 guest/guest, MQTT 1883)
docker compose up -d

# 2) API (banco H2 em memória por padrão; Swagger em http://localhost:8080/swagger-ui.html)
export OPENAI_API_KEY=sk-...      # opcional: sem chave a API usa textos padrão no lugar do LLM
mvn spring-boot:run

# testes
mvn test
```
Variáveis opcionais: `OPENAI_MODEL` (padrão `gpt-4o-mini`), `JWT_SECRET` (**defina em produção**, 32+ caracteres), `RABBITMQ_HOST/PORT/USER/PASSWORD/VHOST/SSL`, `PORT`.

### RabbitMQ sem Docker (CloudAMQP)
Se não for possível usar Docker, crie uma instância gratuita em cloudamqp.com e informe a conexão **AMQP** (não a MQTT):

1. Copie `application-local.properties.example` para `src/main/resources/application-local.properties` e preencha host, usuário, senha e vhost (porta `5671` com TLS). Esse arquivo está no `.gitignore`.
2. Rode com o profile `local`: `mvn spring-boot:run -Dspring-boot.run.profiles=local`.

Alternativa: variáveis de ambiente `RABBITMQ_HOST`, `RABBITMQ_PORT=5671`, `RABBITMQ_SSL=true`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST`.
O `RABBITMQ_HOST` leva **só o nome do servidor**. Nunca coloque senhas no `application.properties` versionado.

Os sensores MQTT (ESP32/simulador) só funcionam se o plugin MQTT estiver habilitado na instância (porta 8883, usuário `vhost:usuario`); sem ele, use `POST /api/leituras`, que publica na mesma fila.

O Hibernate cria/atualiza as tabelas `CT_*` automaticamente (`ddl-auto=update`).


## Usuários de teste (criados na primeira execução)
| Perfil | E-mail | Senha |
|---|---|---|
| GESTOR | `gestor@citizentech.com` | `Gestor@123` |
| CIDADAO | `cidadao@citizentech.com` | `Cidadao@123` |

Também há um cruzamento de exemplo (`id=1`, "Av. Paulista x Rua Augusta") com 3 vias (`id` 1, 2 e 3).

## Painel de testes (Thymeleaf) — `/dev`
Página para o desenvolvedor testar o fluxo inteiro sem montar requisições à mão: **http://localhost:8080/dev**

- Entra como **gestor** ou **cidadão** com um clique (usa o login real e guarda o JWT só na aba).
- **Cruzamento:** fila por via e última decisão com a explicação da IA, atualizando a cada 3 s.
- **Simulador (gestor):** edita veículos/fila por via, atalhos "congestionar" e "emergência", e um modo de tráfego automático. Envia `POST /api/leituras`, que passa pelo RabbitMQ.
- **Relatos:** o cidadão envia e vê a classificação da IA; o gestor muda o status.
- **Console** com cada requisição e resposta (as senhas são mascaradas).

A página só entrega HTML/JS; os dados vêm da API com o JWT, então as regras de perfil continuam valendo (cidadão não consegue enviar leitura). É uma ferramenta de desenvolvimento: **no deploy, defina `DEV_UI=false`**. O app Mobile não depende dela.

## Endpoints principais
| Método e rota | Perfil | Descrição |
|---|---|---|
| `POST /api/auth/registro` · `POST /api/auth/login` | público | Cadastro de cidadão e login (devolve o JWT) |
| `GET /api/cruzamentos` · `GET /api/cruzamentos/{id}` | autenticado | Lista (paginada) e detalhe |
| `GET /api/cruzamentos/{id}/status` | autenticado | Fila por via + última decisão com a explicação da IA |
| `POST/PUT/DELETE /api/cruzamentos` | GESTOR | CRUD de cruzamentos |
| `GET/POST/PUT/DELETE /api/vias` | leitura: autenticado · escrita: GESTOR | CRUD de vias (`?cruzamentoId=`) |
| `POST /api/leituras` · `GET /api/leituras?viaId=` | GESTOR | Injeta leitura (vai para o RabbitMQ) e histórico |
| `GET /api/decisoes?cruzamentoId=` | autenticado | Histórico de decisões |
| `POST /api/relatos` | CIDADAO | Reporta problema (a IA classifica) |
| `GET /api/relatos/meus` | CIDADAO | Meus relatos |
| `GET /api/relatos?status=` · `PATCH /api/relatos/{id}/status` | GESTOR | Fila de relatos e mudança de status |

Listagens aceitam `?page=0&size=10&sort=campo,desc`. No Swagger, deixe o campo `sort` em branco ou use um campo real (ex.: `nome,asc`); campo inexistente devolve 400. Erros seguem o formato `{timestamp,status,erro,mensagem,caminho,campos[]}`.

## Segurança
- Senhas com BCrypt; JWT HS256 com expiração de 2 h; sessão stateless.
- Rotas por papel (cidadão não acessa gestão; gestor não cria relato).
- O texto do cidadão vai ao LLM como **dado** (delimitado no prompt), com saída estruturada validada e fallback; falha do LLM não derruba a API.
- Nenhuma credencial no repositório: chaves via variáveis de ambiente.

## Declaração de uso de IA
> **Ajuste este texto com o grupo antes de entregar.** Exemplo: *Parte do código e deste README foi gerada com apoio do assistente Claude (Anthropic) e revisada, testada e compreendida pelo grupo. A funcionalidade de IA da solução usa um LLM via Spring AI para explicar decisões do semáforo e classificar relatos de cidadãos.*

## Deploy
Link do deploy: _(preencher)_ · Repositório: _(preencher)_ · Vídeo pitch: _(preencher)_
