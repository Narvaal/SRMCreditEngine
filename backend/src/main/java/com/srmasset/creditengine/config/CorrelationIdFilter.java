package com.srmasset.creditengine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Popula {@code requestId} no MDC pra toda a duração da requisição — o formatter de log estruturado
 * (ECS) promove chaves do MDC automaticamente pro JSON de saída, então isso é o que correlaciona
 * todas as linhas de log de uma mesma requisição (incluindo as de {@link
 * com.srmasset.creditengine.exception.GlobalExceptionHandler}) sem precisar passar o id manualmente
 * por cada camada.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  private static final String MDC_KEY = "requestId";
  private static final String HEADER = "X-Request-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }

    MDC.put(MDC_KEY, requestId);
    response.setHeader(HEADER, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
