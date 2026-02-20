package com.example.demo.repository;

import com.example.demo.entity.CordaNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CordaNodeRepository extends JpaRepository<CordaNode, Long> {
    Optional<CordaNode> findByName(String name);
    boolean existsByName(String name);
    void deleteByName(String name);
}