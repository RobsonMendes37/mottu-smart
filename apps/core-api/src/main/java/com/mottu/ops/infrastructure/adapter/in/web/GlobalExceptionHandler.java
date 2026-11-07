package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.exception.AccountPendingApprovalException;
import com.mottu.ops.application.exception.AccountRejectedException;
import com.mottu.ops.application.exception.DuplicateResourceException;
import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.domain.exception.BusinessRuleViolationException;
import com.mottu.ops.domain.exception.DomainValidationException;
import com.mottu.ops.infrastructure.adapter.in.web.dto.ErrorResponseDto;
import com.mottu.ops.infrastructure.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.mottu.ops.infrastructure.adapter.in.web")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleDomainValidationException(
            DomainValidationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Validação Inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Requisição Inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Dados Inválidos", detail, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentialsException(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Não Autenticado", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Não Autenticado", "Credenciais inválidas", request);
    }

    @ExceptionHandler(AccountPendingApprovalException.class)
    public ResponseEntity<ErrorResponseDto> handleAccountPendingApprovalException(
            AccountPendingApprovalException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acesso Proibido", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountRejectedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccountRejectedException(
            AccountRejectedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acesso Proibido", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Acesso Proibido", "Você não tem permissão para acessar este recurso.", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso Não Encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Conflito de Recursos", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessRuleViolationException(
            BusinessRuleViolationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de Negócio Violada", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at " + request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno", "Ocorreu um erro interno inesperado.", request);
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(
            HttpStatus status, String title, String detail, HttpServletRequest request) {
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        ErrorResponseDto body = new ErrorResponseDto(title, status.value(), detail, correlationId);
        return ResponseEntity.status(status).body(body);
    }
}
