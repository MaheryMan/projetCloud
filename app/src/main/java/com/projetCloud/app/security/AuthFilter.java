package com.projetCloud.app.security;

import com.projetCloud.app.utilisateurs.AuthService;
import com.projetCloud.app.utilisateurs.Utilisateur;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtre d'authentification pour vérifier les tokens de session
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Exclure les endpoints publics et ceux du manager par défaut
        if (path.startsWith("/api/auth/login") || 
            path.startsWith("/api/auth/register") || 
            path.startsWith("/swagger") || 
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/actuator") ||
            path.startsWith("/api/deblocages") ||
            path.startsWith("/api/users")) {  // Manager par défaut peut accéder sans session
            filterChain.doFilter(request, response);
            return;
        }

        // Récupérer le token du header Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token manquant ou invalide");
            return;
        }

        String token = authHeader.substring(7); // Enlever "Bearer "

        // Valider le token
        Optional<Utilisateur> userOpt = authService.validateToken(token);
        if (userOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token invalide ou expiré");
            return;
        }

        // Stocker l'utilisateur dans la requête pour les contrôleurs
        request.setAttribute("currentUser", userOpt.get());

        filterChain.doFilter(request, response);
    }
}