package com.example.workhub.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.example.workhub.model.User;

@Mapper
public interface UserMapper {

    @Select("""
        SELECT * FROM users
        WHERE id = #{id}
        """)
    User findById(int id);
}