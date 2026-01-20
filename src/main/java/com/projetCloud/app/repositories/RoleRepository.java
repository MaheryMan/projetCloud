package com.projetCloud.app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.projetCloud.app.models.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

}
