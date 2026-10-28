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

    /** Atomic worker claim; reclaim work if a process died more than two minutes ago. */
    @Update("""
        UPDATE ticket_notifications
        SET status = 'PROCESSING', processing_started_at = NOW(), attempts = attempts + 1
        WHERE id = #{id} AND attempts < 10
          AND (status = 'PENDING' OR
               (status = 'PROCESSING' AND processing_started_at < DATE_SUB(NOW(), INTERVAL 2 MINUTE)))
        """)
    int claim(@Param("id") long id);

    @Update("""
        UPDATE ticket_notifications
        SET status = 'READY', message = #{message}, processed_at = NOW(), processing_started_at = NULL
        WHERE id = #{id} AND status = 'PROCESSING'
        """)
    int markReady(@Param("id") long id, @Param("message") String message);

    @Update("""
        UPDATE ticket_notifications
        SET status = CASE WHEN attempts >= 10 THEN 'FAILED' ELSE 'PENDING' END,
            processing_started_at = NULL
        WHERE id = #{id} AND status = 'PROCESSING'
        """)
    int releaseForRetry(@Param("id") long id);

    @Select("""
        SELECT id FROM ticket_notifications
        WHERE (status = 'PENDING' OR
              (status = 'PROCESSING' AND processing_started_at < DATE_SUB(NOW(), INTERVAL 2 MINUTE)))
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
        UPDATE ticket_notifications SET status='PENDING', attempts=0,
            processing_started_at=NULL
        WHERE id=#{id} AND status='FAILED'
        """)
    int requeueFailed(@Param("id") long id);
}
