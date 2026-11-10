package com.example.workhub.notification;

import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface NotificationJobMapper {
    @Insert("""
        INSERT INTO ticket_notifications(ticket_id, recipient_id, actor_id,
                                         event_type, old_value, new_value)
        VALUES(#{ticketId}, #{recipientId}, #{actorId},
               #{eventType}, #{oldValue}, #{newValue})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int add(NotificationJob job);

    @Select("SELECT * FROM ticket_notifications WHERE id = #{id}")
    NotificationJob findById(@Param("id") long id);

    /**
     * Atomically claim a pending job or an expired processing lease.
     * Each successful claim replaces the previous claim token.
     */
    @Update("""
        UPDATE ticket_notifications
        SET status = 'PROCESSING', processing_started_at = NOW(),
            attempts = attempts + 1, claim_token = #{claimToken}
        WHERE id = #{id} AND attempts < 10
          AND (status = 'PENDING' OR
               (status = 'PROCESSING'
                AND processing_started_at < DATE_SUB(NOW(), INTERVAL 2 MINUTE)))
        """)
    int claim(@Param("id") long id, @Param("claimToken") String claimToken);

    /** Only the worker holding the current claim may complete the job. */
    @Update("""
        UPDATE ticket_notifications
        SET status = 'READY', message = #{message}, processed_at = NOW(),
            processing_started_at = NULL, claim_token = NULL
        WHERE id = #{id} AND status = 'PROCESSING'
          AND claim_token = #{claimToken}
        """)
    int markReady(@Param("id") long id,
                  @Param("claimToken") String claimToken,
                  @Param("message") String message);

    /** Never release a lease that has been superseded by another worker. */
    @Update("""
        UPDATE ticket_notifications
        SET status = CASE WHEN attempts >= 10 THEN 'FAILED' ELSE 'PENDING' END,
            processing_started_at = NULL, claim_token = NULL
        WHERE id = #{id} AND status = 'PROCESSING'
          AND claim_token = #{claimToken}
        """)
    int releaseForRetry(@Param("id") long id,
                        @Param("claimToken") String claimToken);

    /**
     * A crash after claim #10 leaves status=PROCESSING and attempts=10.
     * Once the lease expires, move it to FAILED for manager inspection/retry.
     */
    @Update("""
        UPDATE ticket_notifications
        SET status = 'FAILED', processing_started_at = NULL, claim_token = NULL
        WHERE status = 'PROCESSING' AND attempts >= 10
          AND processing_started_at < DATE_SUB(NOW(), INTERVAL 2 MINUTE)
        """)
    int expireExhausted();

    @Select("""
        SELECT id FROM ticket_notifications
        WHERE (status = 'PENDING' OR
              (status = 'PROCESSING'
               AND processing_started_at < DATE_SUB(NOW(), INTERVAL 2 MINUTE)))
          AND attempts < 10
        ORDER BY id ASC LIMIT #{limit}
        """)
    List<Long> pendingIds(@Param("limit") int limit);

    @Select("""
        SELECT * FROM ticket_notifications
        WHERE recipient_id = #{recipientId} AND status = 'READY'
        ORDER BY id DESC LIMIT #{limit}
        """)
    List<NotificationJob> readyForRecipient(@Param("recipientId") int recipientId,
                                           @Param("limit") int limit);

    @Select("""
        SELECT * FROM ticket_notifications
        WHERE status = 'FAILED' ORDER BY id DESC LIMIT #{limit}
        """)
    List<NotificationJob> failedJobs(@Param("limit") int limit);

    @Update("""
        UPDATE ticket_notifications
        SET status = 'PENDING', attempts = 0,
            processing_started_at = NULL, claim_token = NULL
        WHERE id = #{id} AND status = 'FAILED'
        """)
    int requeueFailed(@Param("id") long id);
}
