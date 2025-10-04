package org.example.bookingtower.infrastructure.repository;

import org.example.bookingtower.domain.entity.Coworking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий CoworkingRepository для доступа к данным BookingTower.
 */
@Repository
public interface CoworkingRepository extends JpaRepository<Coworking, Long> {
    
    List<Coworking> findByActiveTrue();
    
    Optional<Coworking> findByIdAndActiveTrue(Long id);
    
    Optional<Coworking> findByName(String name);
    
    boolean existsByName(String name);
    
    @Query("SELECT c FROM Coworking c WHERE c.active = true ORDER BY c.name")
    List<Coworking> findAllActiveOrderByName();
    
    @Query("SELECT c FROM Coworking c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.active = true")
    List<Coworking> findByNameContainingIgnoreCaseAndActiveTrue(String name);
    
    @Query("SELECT c FROM Coworking c WHERE LOWER(c.address) LIKE LOWER(CONCAT('%', :address, '%')) AND c.active = true")
    List<Coworking> findByAddressContainingIgnoreCaseAndActiveTrue(String address);
}
