package org.example.bookingtower.infrastructure.repository;

import org.example.bookingtower.domain.entity.WorkspaceSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий WorkspaceSeatRepository для доступа к данным BookingTower.
 */
@Repository
public interface WorkspaceSeatRepository extends JpaRepository<WorkspaceSeat, Long> {
    
    List<WorkspaceSeat> findByWorkspaceIdAndActiveTrue(Long workspaceId);
    
    List<WorkspaceSeat> findByActiveTrue();
    
    Optional<WorkspaceSeat> findByIdAndActiveTrue(Long id);
    
    Optional<WorkspaceSeat> findByWorkspaceIdAndCode(Long workspaceId, String code);
    
    @Query("SELECT ws FROM WorkspaceSeat ws WHERE ws.workspace.id = :workspaceId AND ws.active = true ORDER BY ws.code")
    List<WorkspaceSeat> findByWorkspaceIdAndActiveTrueOrderByCode(@Param("workspaceId") Long workspaceId);
    
    @Query("SELECT ws FROM WorkspaceSeat ws WHERE ws.workspace.coworking.id = :coworkingId AND ws.active = true ORDER BY ws.workspace.name, ws.code")
    List<WorkspaceSeat> findByCoworkingIdAndActiveTrueOrderByWorkspaceAndCode(@Param("coworkingId") Long coworkingId);
    
    @Query("SELECT ws FROM WorkspaceSeat ws WHERE LOWER(ws.code) LIKE LOWER(CONCAT('%', :code, '%')) AND ws.active = true")
    List<WorkspaceSeat> findByCodeContainingIgnoreCaseAndActiveTrue(@Param("code") String code);
    
    @Query("SELECT COUNT(ws) FROM WorkspaceSeat ws WHERE ws.workspace.id = :workspaceId AND ws.active = true")
    long countByWorkspaceIdAndActiveTrue(@Param("workspaceId") Long workspaceId);
    
    @Query("SELECT COUNT(ws) FROM WorkspaceSeat ws WHERE ws.workspace.coworking.id = :coworkingId AND ws.active = true")
    long countByCoworkingIdAndActiveTrue(@Param("coworkingId") Long coworkingId);
    
    @Query("SELECT ws FROM WorkspaceSeat ws WHERE ws.workspace.id IN :workspaceIds AND ws.active = true ORDER BY ws.workspace.name, ws.code")
    List<WorkspaceSeat> findByWorkspaceIdInAndActiveTrueOrderByWorkspaceAndCode(@Param("workspaceIds") List<Long> workspaceIds);
    
    boolean existsByWorkspaceIdAndCode(Long workspaceId, String code);
    
    @Query("SELECT ws FROM WorkspaceSeat ws WHERE ws.workspace.id = :workspaceId AND ws.code = :code AND ws.active = true")
    Optional<WorkspaceSeat> findByWorkspaceIdAndCodeAndActiveTrue(@Param("workspaceId") Long workspaceId, @Param("code") String code);
}
