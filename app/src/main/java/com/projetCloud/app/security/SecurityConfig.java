package com.projetCloud.app.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthFilter authFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                // Permettre l'accès aux endpoints publics
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/register/",
                    "/api/auth/register-google",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/**",
                    "/api/connectivity/**",
                    "/api/sync/**",
                    "/uploads/photos/**"
                ).permitAll()
                // Upload de photos publique
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/photos/upload").permitAll()
                // Création de signalements publique (pour visiteurs non authentifiés)                // Lecture des signalements publique
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/signalements/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/entreprises").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/types-signalement").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/types-signalement/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/historiques/**").permitAll()
                // Mobile_User: accès limité
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/entreprises/**"
                ).hasAnyAuthority("Mobile_User", "Manager")
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/api/entreprises"
                ).hasAnyAuthority("Mobile_User", "Manager")
                // Configurations: uniquement pour les Managers
                .requestMatchers("/api/configurations/**").hasAuthority("Manager")
                // Manager: accès total au reste de l'API
                .requestMatchers("/api/**").hasAuthority("Manager")
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}