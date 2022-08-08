package com.spinel.datacollection.service.repositories;



import com.spinel.datacollection.core.models.SubmissionComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {

    SubmissionComment findBySubmissionIdAndCommentId(Long submissionId, Long commentId);

    List<SubmissionComment> findByIsActive(Boolean isActive);

    @Query("SELECT s FROM SubmissionComment s WHERE ((:submissionId IS NULL) OR (:submissionId IS NOT NULL AND s.submissionId = :submissionId))" +
            " AND ((:commentId IS NULL) OR (:commentId IS NOT NULL AND s.commentId =  :commentId)) " +
            " AND ((:additionalInfo IS NULL) OR (:additionalInfo IS NOT NULL AND s.additionalInfo like %:additionalInfo%)) order by s.id desc")
    Page<SubmissionComment> findSubmissionComment(@Param("submissionId") Long submissionId,
                                                  @Param("commentId") Long commentId,
                                                  @Param("additionalInfo") String additionalInfo,
                                                  Pageable pageable);

}
