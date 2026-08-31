package com.teamproject.japan_newhire_rag_backend.domain.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import com.teamproject.japan_newhire_rag_backend.domain.auth.security.JwtAuthenticationFilter;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAccessDeniedHandler;
import com.teamproject.japan_newhire_rag_backend.domain.auth.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({AuthCookieProperties.class, AuthCorsProperties.class})
public class SecurityConfig {

    private static final String[] APPLICATION_ROLES = {
            "EMPLOYEE",
            "MANAGER",
            "HR_MANAGER",
            "SYSTEM_ADMIN"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final AuthCookieProperties cookieProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            AuthCookieProperties cookieProperties
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.cookieProperties = cookieProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(new OrRequestMatcher(
                                PathPatternRequestMatcher.pathPattern(
                                        HttpMethod.POST, "/api/auth/refresh"),
                                PathPatternRequestMatcher.pathPattern(
                                        HttpMethod.POST, "/api/auth/logout"))))
                .cors(cors -> { })
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(requestCache -> requestCache
                        .requestCache(new NullRequestCache()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/health")
                        .permitAll()
                        .requestMatchers("/api/admin/users/**")
                        .hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/audit-logs")
                        .hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/hr/employees/*/manager")
                        .hasRole("HR_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/documents")
                        .hasAnyRole("HR_MANAGER", "SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/documents/categories")
                        .hasAnyRole("HR_MANAGER", "SYSTEM_ADMIN")
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/documents/*/versions/*/publish")
                        .hasAnyRole("HR_MANAGER", "SYSTEM_ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/documents/*/versions/*/access-rule")
                        .hasAnyRole("HR_MANAGER", "SYSTEM_ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/hr/document-processing-jobs")
                        .hasRole("HR_MANAGER")
                        .anyRequest()
                        .hasAnyRole(APPLICATION_ROLES))
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AuthCorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite()));
        return repository;
    }
}
