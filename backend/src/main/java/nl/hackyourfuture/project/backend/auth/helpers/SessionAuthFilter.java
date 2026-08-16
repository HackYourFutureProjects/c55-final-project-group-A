package nl.hackyourfuture.project.backend.auth.helpers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import nl.hackyourfuture.project.backend.auth.SessionRepository;
import nl.hackyourfuture.project.backend.auth.TokenService;
import nl.hackyourfuture.project.backend.user.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@AllArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {

  private final SessionRepository sessionRepository;
  private final UserRepository userRepository;
  private final TokenService tokenService;
  private final CookieUtils cookieUtils;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String rawAccessToken = cookieUtils.extractToken(request);

    if (rawAccessToken != null) {
      String hashedAccessToken = tokenService.hashToken(rawAccessToken);
      sessionRepository.findSessionByAccessTokenHash(hashedAccessToken)
          .filter(session -> session.getAccessExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
          .flatMap(session -> userRepository.findUserById(session.getUserId()))
          .ifPresent(user -> {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                var auth = new UsernamePasswordAuthenticationToken(
                    user.getId(), null, authorities
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
              }

          );
    }

    filterChain.doFilter(request, response);
  }

}
