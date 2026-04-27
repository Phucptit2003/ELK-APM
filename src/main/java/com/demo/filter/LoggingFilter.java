package com.demo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // lấy traceId từ APM (auto inject)
        String traceId = MDC.get("trace.id");

        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());
        MDC.put("ip", request.getRemoteAddr());
        MDC.put("traceId", traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
