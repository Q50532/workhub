package com.example.workhub.mapper;

import org.apache.ibatis.annotations.*;

import com.example.workhub.model.Comment;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("""
        Insert Into comments
        (ticket_id, author_id, content)
        Values
        (#{ticketId}, #{authorId}, #{content})
        """)
    int add(Comment comment);

    

    @Select("""
            Select * from comments where ticket_id=#{ticketId}
            """)
    List<Comment> findByTicketId(int ticketId);

    
}
