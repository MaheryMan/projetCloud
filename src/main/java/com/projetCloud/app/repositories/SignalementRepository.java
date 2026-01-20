package com.projetCloud.app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.projetCloud.app.models.Signalement;

public interface SignalementRepository extends JpaRepository<Signalement, Integer> {

}
