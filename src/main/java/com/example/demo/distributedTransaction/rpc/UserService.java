package com.example.demo.distributedTransaction.rpc;

import com.example.demo.distributedTransaction.bean.User;
import com.example.demo.distributedTransaction.saga.SagaAction;

public interface UserService {

    public int addArbitraryUser();

    public void removeUser(Integer id);
}
