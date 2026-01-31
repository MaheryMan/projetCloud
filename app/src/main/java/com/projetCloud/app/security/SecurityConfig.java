package com.projetCloud.app.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthFilter authFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Permettre l'accès aux endpoints publics
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/**"
                ).permitAll()
                // Création d'utilisateur publique
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/users").permitAll()
                // Lecture des signalements publique
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/signalements/**").permitAll()
                // Mobile_User: accès limité
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/signalements").hasAnyAuthority("Mobile_User", "Manager")
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/types-signalement/**",
                        "/api/entreprises/**"
                ).hasAnyAuthority("Mobile_User", "Manager")
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/api/entreprises"
                ).hasAnyAuthority("Mobile_User", "Manager")
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
}