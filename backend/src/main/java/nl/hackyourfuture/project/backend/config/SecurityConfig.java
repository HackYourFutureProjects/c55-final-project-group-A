package nl.hackyourfuture.project.backend.config;

import nl.hackyourfuture.project.backend.auth.helpers.CustomAuthenticationEntryPoint;
import nl.hackyourfuture.project.backend.auth.helpers.SessionAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SessionAuthFilter sessionAuthFilter,
                                                   CustomAuthenticationEntryPoint entryPoint) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/docs/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/auth/logout").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/events/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/events/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/events/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}