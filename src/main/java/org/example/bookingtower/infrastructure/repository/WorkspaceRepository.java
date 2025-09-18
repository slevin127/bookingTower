package org.example.bookingtower.infrastructure.repository;

import org.example.bookingtower.domain.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    
    List<Workspace> findByCoworkingIdAndActiveTrue(Long coworkingId);
    
    List<Workspace> findByActiveTrue();
    
    Optional<Workspace> findByIdAndActiveTrue(Long id);
    
    @Query("SELECT w FROM Workspace w WHERE w.coworking.id = :coworkingId AND w.active = true ORDER BY w.name")
    List<Workspace> findByCoworkingIdAndActiveTrueOrderByName(@Param("coworkingId") Long coworkingId);
    
    @Query("SELECT w FROM Workspace w WHERE w.active = true ORDER BY w.coworking.name, w.name")
    List<Workspace> findAllActiveOrderByCoworkingAndName();
    
    @Query("SELECT w FROM Workspace w WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :name, '%')) AND w.active = true")
    List<Workspace> findByNameContainingIgnoreCaseAndActiveTrue(@Param("name") String name);
    
    @Query("SELECT w FROM Workspace w WHERE w.pricePerHour BETWEEN :minPrice AND :maxPrice AND w.active = true")
    List<Workspace> findByPricePerHourBetweenAndActiveTrue(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT w FROM Workspace w WHERE w.seatsTotal >= :minSeats AND w.active = true")
    List<Workspace> findBySeatsGreaterThanEqualAndActiveTrue(@Param("minSeats") Integer minSeats);
    
    @Query("SELECT w FROM Workspace w WHERE w.coworking.id = :coworkingId AND w.pricePerHour BETWEEN :minPrice AND :maxPrice AND w.active = true")
    List<Workspace> findByCoworkingIdAndPriceRangeAndActiveTrue(
            @Param("coworkingId") Long coworkingId, 
            @Param("minPrice") BigDecimal minPrice, 
            @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT COUNT(w) FROM Workspace w WHERE w.coworking.id = :coworkingId AND w.active = true")
    long countByCoworkingIdAndActiveTrue(@Param("coworkingId") Long coworkingId);
    
    @Query("SELECT SUM(w.seatsTotal) FROM Workspace w WHERE w.coworking.id = :coworkingId AND w.active = true")
    Long getTotalSeatsByCoworkingId(@Param("coworkingId") Long coworkingId);
}