package com.schbus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Integer> {
    Optional<Person> findByUserId(String userId);
    Optional<Person> findByNumber(Integer user);
}
