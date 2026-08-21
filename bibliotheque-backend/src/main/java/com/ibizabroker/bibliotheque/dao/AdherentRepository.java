package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Adherent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdherentRepository extends JpaRepository<Adherent, Integer> {
    Optional<Adherent> findByUsername(String username);
}
