package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.metadata.RequestMetadata;
import com.taca.paymentwallet.application.metadata.RequestMetadataContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestMetadataFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "X-Request-ID";
    private static final String TRACEPARENT = "traceparent";
    private static final String TRACESTATE = "tracestate";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RequestMetadata metadata = new RequestMetadata(
                normalize(request.getHeader(REQUEST_ID)),
                normalize(request.getHeader(TRACEPARENT)),
                normalize(request.getHeader(TRACESTATE))
        );

        try {
            RequestMetadataContext.set(metadata);
            filterChain.doFilter(request, response);
        } finally {
            RequestMetadataContext.clear();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}