package com.example.demo.distributedTransaction.rpc.external;

import com.example.demo.distributedTransaction.saga.Method;
import com.example.demo.distributedTransaction.saga.SagaAction;

@SagaAction(methods = @Method(name="execute"))
public class TestExternal {

    public void execute(){
        System.out.println("execute external action");
        System.out.println("i still miss you i still love you i wanna take step into your heart");
    }
}
