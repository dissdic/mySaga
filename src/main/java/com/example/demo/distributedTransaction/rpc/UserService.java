package com.example.demo.distributedTransaction.rpc;

import com.example.demo.distributedTransaction.bean.User;
import com.example.demo.distributedTransaction.saga.SagaAction;

public interface UserService {
    @SagaAction(rollbackMethodFullName = "removeUser",retry = 1)
    public int addArbitraryUser();

    public void removeUser(Integer id);
}
