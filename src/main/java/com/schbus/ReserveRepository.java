package com.schbus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReserveRepository extends JpaRepository<Reserve, Integer> {
    Optional<Reserve> findByUser(String user);
    List<Reserve> findByBusAndDate(String selectedBus, String selectedDate);

}
