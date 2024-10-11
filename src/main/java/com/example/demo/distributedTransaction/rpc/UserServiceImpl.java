package com.example.demo.distributedTransaction.rpc;

import com.example.demo.distributedTransaction.bean.User;
import com.example.demo.distributedTransaction.mapper.UserMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@DubboService(version = "1.0.0", interfaceClass = UserService.class)
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public int addArbitraryUser() {
        User user = new User();
        user.setName("tom");
        user.setDeleted(false);
        userMapper.addUser(user);
        return user.getId();
    }

    @Override
    public void removeUser(Integer id) {
        userMapper.removeUser(id);
    }
}
