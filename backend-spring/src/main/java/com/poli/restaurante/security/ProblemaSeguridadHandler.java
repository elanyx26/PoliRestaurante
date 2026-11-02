package com.poli.restaurante.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Respuestas 401 y 403 en formato RFC 7807 (application/problem+json), generadas desde los filtros de seguridad. */
@Component
public class ProblemaSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        escribir(response, 401, "No autenticado", "Se requiere un token válido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        escribir(response, 403, "Acceso denegado", "No tiene permisos para este recurso");
    }

    private static void escribir(HttpServletResponse response, int estado, String titulo, String detalle)
            throws IOException {
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":" + estado
                + ",\"detail\":\"" + detalle + "\"}");
    }
}
