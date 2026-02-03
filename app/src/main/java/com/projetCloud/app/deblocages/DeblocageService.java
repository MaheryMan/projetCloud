package com.projetCloud.app.deblocages;

import com.projetCloud.app.utilisateurs.Utilisateur;
import com.projetCloud.app.utilisateurs.UtilisateurRepository;
import com.projetCloud.app.sync.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeblocageService {

    @Autowired
    private DeblocageRepository deblocageRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private SyncService syncService;

    public List<Deblocage> findAll() {
        return deblocageRepository.findAll();
    }

    public Optional<Deblocage> findById(Long id) {
        return deblocageRepository.findById(id);
    }

    public Deblocage save(Deblocage deblocage) {
        return deblocageRepository.save(deblocage);
    }

    public void deleteById(Long id) {
        deblocageRepository.deleteById(id);
    }

    public List<Deblocage> findByUtilisateurId(Long utilisateurId) {
        return deblocageRepository.findByUtilisateurId(utilisateurId);
    }

    public List<Deblocage> findByManagerId(Long managerId) {
        return deblocageRepository.findByManagerId(managerId);
    }

    /**
     * Débloque un utilisateur bloqué
     * - Réinitialise is_blocked à FALSE dans PostgreSQL
     * - Réinitialise tentatives_connexion à 0
     * - Crée un enregistrement de déblocage
     * 
     * ⚠️ La synchronisation vers Firebase doit être faite manuellement via /api/sync/deblocage/{id}
     * 
     * @param utilisateurId L'ID de l'utilisateur à débloquer
     * @param managerId L'ID du manager qui effectue le déblocage
     * @param motif Le motif du déblocage (optionnel)
     * @return L'utilisateur débloqué, ou empty si l'utilisateur n'existe pas
     */
    public Optional<Utilisateur> unlockUser(Long utilisateurId, Long managerId, String motif) {
        Optional<Utilisateur> userOpt = utilisateurRepository.findById(utilisateurId);
        Optional<Utilisateur> managerOpt = utilisateurRepository.findById(managerId);
        
        if (userOpt.isPresent() && managerOpt.isPresent()) {
            Utilisateur user = userOpt.get();
            Utilisateur manager = managerOpt.get();
            
            // Réinitialiser l'utilisateur dans PostgreSQL
            user.setIsBlocked(false);
            user.setTentativesConnexion(0);
            user.setLastFailedAttempt(null);
            user.setUpdatedAt(LocalDateTime.now());
            
            // Sauvegarder les modifications dans PostgreSQL
            utilisateurRepository.save(user);
            
            // Créer l'enregistrement de déblocage (audit trail)
            Deblocage deblocage = new Deblocage(
                motif != null ? motif : "Déblocage manuel par manager",
                user,
                manager
            );
            deblocageRepository.save(deblocage);
            
            System.out.println("Utilisateur " + user.getEmail() + " débloqué dans PostgreSQL. Appelez /api/sync/deblocage/{id} pour synchroniser vers Firebase.");
            
            return Optional.of(user);
        }
        
        return Optional.empty();
    }
}