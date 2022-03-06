package com.sabi.datacollection.service.repositories;

import com.sabi.datacollection.core.models.CommentDictionary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentDictionaryRepository extends JpaRepository<CommentDictionary, Long> {
    CommentDictionary findByName(String name);

    List<CommentDictionary> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM CommentDictionary c WHERE ((:name IS NULL) OR (:name IS NOT NULL AND c.name like %:name%)) order by c.id desc")
    Page<CommentDictionary> findCommentDictionaries(@Param("name") String name, Pageable pageable);
}
