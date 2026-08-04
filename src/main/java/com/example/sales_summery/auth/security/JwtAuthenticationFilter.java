package com.example.sales_summery.auth.security;

import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.domain.UserStatus;
import com.example.sales_summery.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserRepository userRepository,
                                   RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    // 토큰 사용자도 매 요청 DB 상태를 확인해 정지·탈퇴 후 즉시 접근을 막는다.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = bearerToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            AuthenticatedUser principal = jwtTokenService.parse(token);
            User user = userRepository.findById(principal.userId()).orElseThrow(InvalidAuthentication::new);
            if (user.getStatus() != UserStatus.ACTIVE) throw new InvalidAuthentication();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException | InvalidAuthentication exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new InvalidAuthentication());
        }
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private static class InvalidAuthentication extends AuthenticationException {
        InvalidAuthentication() { super("Invalid authentication"); }
    }
}
