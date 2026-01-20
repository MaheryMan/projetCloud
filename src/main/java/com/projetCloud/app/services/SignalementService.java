package com.projetCloud.app.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import com.projetCloud.app.repositories.SignalementRepository;
import com.projetCloud.app.models.Signalement;

@Service
public class SignalementService {
    private final SignalementRepository repo;

    @Autowired
    public SignalementService(SignalementRepository repo) {
        this.repo = repo;
    }

    public List<Signalement> findAll() {
        return repo.findAll();
    }

    public Optional<Signalement> findById(Integer id) {
        return repo.findById(id);
    }

    public Signalement save(Signalement s) {
        return repo.save(s);
    }

    public void deleteById(Integer id) {
        repo.deleteById(id);
    }
}
