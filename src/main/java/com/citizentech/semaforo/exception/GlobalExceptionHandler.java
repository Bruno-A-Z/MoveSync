package com.citizentech.semaforo.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Tratamento global: toda resposta de erro da API segue o formato de {@link ApiError}.
 * Estende ResponseEntityExceptionHandler para cobrir também os erros padrão do Spring MVC
 * (JSON inválido, método não suportado, parâmetro ausente etc.).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- erros padrão do Spring MVC ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<ApiError.CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.CampoInvalido(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(status)
                .body(ApiError.de(status, "Dados inválidos", caminho(request), campos));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String detalhe = "Requisição inválida";
        if (ex instanceof ErrorResponse er && er.getBody().getDetail() != null) {
            detalhe = er.getBody().getDetail();
        }
        return ResponseEntity.status(statusCode).headers(headers)
                .body(ApiError.de(statusCode, detalhe, caminho(request)));
    }

    // ---------- erros do domínio ----------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiError> conflito(ConflitoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> ordenacaoInvalida(PropertyReferenceException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Campo de ordenação inválido: " + ex.getPropertyName(), req);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ApiError> usoInvalidoDeDados(InvalidDataAccessApiUsageException ex, HttpServletRequest req) {
        if (ex.getCause() instanceof PropertyReferenceException pre) {
            return resposta(HttpStatus.BAD_REQUEST, "Campo de ordenação inválido: " + pre.getPropertyName(), req);
        }
        log.error("Uso inválido do acesso a dados em {}", req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT,
                "Operação viola a integridade dos dados (registro em uso por outro recurso ou duplicado)", req);
    }

    // ---------- segurança ----------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> credenciais(BadCredentialsException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para este recurso", req);
    }

    // ---------- infraestrutura ----------

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ApiError> mensageria(AmqpException ex, HttpServletRequest req) {
        log.error("Falha de comunicação com o RabbitMQ", ex);
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, "Mensageria indisponível no momento. Tente novamente.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", req);
    }

    // ---------- utilitários ----------

    private ResponseEntity<ApiError> resposta(HttpStatus status, String mensagem, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ApiError.de(status, mensagem, req.getRequestURI()));
    }

    private String caminho(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
    }
}
