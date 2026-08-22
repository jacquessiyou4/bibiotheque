package com.ibizabroker.bibliotheque.dao;

import com.ibizabroker.bibliotheque.entity.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdministratorRepository extends JpaRepository<Administrator, Integer> {
    Optional<Administrator> findByUsername(String username);
}
