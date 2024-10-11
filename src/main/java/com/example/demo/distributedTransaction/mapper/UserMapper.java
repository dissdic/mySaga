package com.example.demo.distributedTransaction.mapper;

import com.example.demo.distributedTransaction.bean.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    public int addUser(User user);
    public void removeUser(Integer id);
}
