package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.security.MfaStepUpContext;
import com.taca.paymentwallet.application.security.MfaStepUpContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MfaStepUpFilter extends OncePerRequestFilter {

    private static final String MFA_STEP_UP = "X-MFA-Step-Up";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(MFA_STEP_UP);

        try {
            if (header != null && !header.isBlank()) {
                MfaStepUpContextHolder.set(new MfaStepUpContext(true));
            } else {
                MfaStepUpContextHolder.clear();
            }

            filterChain.doFilter(request, response);
        } finally {
            MfaStepUpContextHolder.clear();
        }
    }
}