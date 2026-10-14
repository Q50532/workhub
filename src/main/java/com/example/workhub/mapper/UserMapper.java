package com.example.workhub.mapper;

import org.apache.ibatis.annotations.*;

import com.example.workhub.model.User;

@Mapper
public interface UserMapper {

    @Select("""
        SELECT * FROM users
        WHERE id = #{id}
        """)
    User findById(int id);

    @Select("""
            Select * From users
            Where username = #{username}
            """)
    User findByUsername(String name);

    @Update("""
        UPDATE users
        SET password = #{encodedPassword}
        WHERE id = #{id}
        """)
    int updatePassword(int id, String encodedPassword);

    
}