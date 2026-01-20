package com.projetCloud.app.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import com.projetCloud.app.repositories.RoleRepository;
import com.projetCloud.app.models.Role;

@Service
public class RoleService {
    private final RoleRepository repo;

    @Autowired
    public RoleService(RoleRepository repo) {
        this.repo = repo;
    }

    public List<Role> findAll() {
        return repo.findAll();
    }

    public Optional<Role> findById(Integer id) {
        return repo.findById(id);
    }

    public Role save(Role role) {
        return repo.save(role);
    }

    public void deleteById(Integer id) {
        repo.deleteById(id);
    }
}
