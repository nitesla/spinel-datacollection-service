package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.ProjectOwner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectOwnerRepository extends JpaRepository<ProjectOwner, Long> {
    ProjectOwner findByUserId(Long userId);
    ProjectOwner findProjectOwnerById(Long id);
    ProjectOwner findProjectOwnerByEmail(String email);
    ProjectOwner findProjectOwnerByPhone(String phone);

    List<ProjectOwner> findByIsActive(Boolean isActive);
    Integer countAllByIsActive(Boolean isActive);

    Integer countAllByIsActive(boolean isActive);

    ProjectOwner findProjectOwnerByUserId(Long userId);

    @Query("SELECT p FROM ProjectOwner p WHERE ((:firstname IS NULL) OR (:firstname IS NOT NULL AND p.firstname like %:firstname%))" +
            " AND ((:lastname IS NULL) OR (:lastname IS NOT NULL AND p.lastname like  %:lastname%)) " +
            " AND ((:email IS NULL) OR (:email IS NOT NULL AND p.email like %:email%)) order by p.id desc")
    Page<ProjectOwner> findProjectOwners(@Param("firstname") String firstname,
                                         @Param("lastname") String lastname,
                                         @Param("email") String email,
                                         Pageable pageable);
    List<ProjectOwner> findAll();
}
