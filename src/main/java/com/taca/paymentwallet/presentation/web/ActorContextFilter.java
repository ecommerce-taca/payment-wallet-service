package com.taca.paymentwallet.presentation.web;

import com.taca.paymentwallet.application.security.ActorContext;
import com.taca.paymentwallet.application.security.ActorContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ActorContextFilter extends OncePerRequestFilter {

    private static final String USER_ID = "X-User-ID";
    private static final String USER_ROLES = "X-User-Roles";
    private static final String USER_PERMISSIONS = "X-User-Permissions";
    private static final String USER_SHOP_SCOPE = "X-User-Shop-Scope";

    private final ActorHeaderParser parser = new ActorHeaderParser();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ActorContext actor = parser.parse(
                request.getHeader(USER_ID),
                request.getHeader(USER_ROLES),
                request.getHeader(USER_PERMISSIONS),
                request.getHeader(USER_SHOP_SCOPE)
        );

        try {
            ActorContextHolder.set(actor);
            filterChain.doFilter(request, response);
        } finally {
            ActorContextHolder.clear();
        }
    }
}