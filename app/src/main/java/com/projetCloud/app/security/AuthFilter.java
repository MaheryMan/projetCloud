package com.projetCloud.app.security;

import com.projetCloud.app.utilisateurs.AuthService;
import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filtre d'authentification pour vérifier les tokens de session
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthService authService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Exclure les endpoints publics et ceux du manager par défaut
        if (path.startsWith("/api/auth/login") || 
            path.startsWith("/api/auth/register") ||
            path.startsWith("/api/auth/register/") ||
            path.startsWith("/swagger") || 
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/actuator") ||
            ("POST".equalsIgnoreCase(method) && "/api/users".equals(path)) ||
            ("GET".equalsIgnoreCase(method) && path.startsWith("/api/signalements"))||
            ("GET".equalsIgnoreCase(method) && path.startsWith("/api/entreprises"))) {
             System.out.println("YUP C EST REGISTER ");
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

        Utilisateur user = userOpt.get();

        // Déclarer l'utilisateur comme authentifié pour Spring Security
        List<SimpleGrantedAuthority> authorities = Collections.emptyList();
        List<String> roleLibelles = utilisateurService.getUserRoles(user.getId());
        if (roleLibelles != null) {
            authorities = roleLibelles.stream()
                    .filter(r -> r != null)
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Stocker l'utilisateur dans la requête pour les contrôleurs
        request.setAttribute("currentUser", user);

        filterChain.doFilter(request, response);
    }
}