package com.verilogic.ui.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global REST Exception Handler.
 * Intercepts bad requests, unhandled errors, and invalid payloads,
 * emitting structured JSON responses and clean, actionable console logs.
 */
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = "err-" + UUID.randomUUID().toString().substring(0, 8);
        log.warn("[API WARN] [400 BAD_REQUEST] Malformed JSON payload received on URI [{}]: {}", 
                request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("code", "MALFORMED_JSON_PAYLOAD");
        body.put("message", "The request body could not be parsed as valid JSON or violates expected schema.");
        body.put("path", request.getRequestURI());
        body.put("traceId", traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header("X-VeriLogic-Trace-Id", traceId)
                .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String traceId = "err-" + UUID.randomUUID().toString().substring(0, 8);
        log.warn("[API WARN] [400 BAD_REQUEST] Invalid argument on URI [{}]: {}", 
                request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("code", "INVALID_ARGUMENT");
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("traceId", traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header("X-VeriLogic-Trace-Id", traceId)
                .body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String traceId = "err-" + UUID.randomUUID().toString().substring(0, 8);
        log.warn("[API WARN] [409 CONFLICT] Concurrent evaluation on URI [{}]: {}",
                request.getRequestURI(), ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("code", "CASE_ALREADY_IN_PROGRESS");
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("traceId", traceId);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header("X-VeriLogic-Trace-Id", traceId)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = "err-" + UUID.randomUUID().toString().substring(0, 8);
        log.error("[API ERROR] [500 INTERNAL_SERVER_ERROR] Unhandled exception on URI [{}] (Trace: {}): {}", 
                request.getRequestURI(), traceId, ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("code", "UNEXPECTED_ENGINE_ERROR");
        body.put("message", "An unexpected internal error occurred while processing the decision request.");
        body.put("path", request.getRequestURI());
        body.put("traceId", traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-VeriLogic-Trace-Id", traceId)
                .body(body);
    }
}
