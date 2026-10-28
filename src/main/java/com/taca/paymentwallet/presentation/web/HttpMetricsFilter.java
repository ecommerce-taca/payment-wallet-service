package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.observability.HttpRequestObservation;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

@Component
public class HttpMetricsFilter extends OncePerRequestFilter {

    private static final String UNMATCHED_ROUTE = "unmatched";

    private final HttpRequestObservation observation;

    public HttpMetricsFilter(HttpRequestObservation observation) {
        this.observation = Objects.requireNonNull(observation);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            observation.record(
                    request.getMethod(),
                    route(request),
                    status,
                    elapsed(startedAt)
            );
        }
    }

    private String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );

        if (pattern instanceof String value && !value.isBlank()) {
            return value;
        }

        return UNMATCHED_ROUTE;
    }

    private Duration elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }
}
