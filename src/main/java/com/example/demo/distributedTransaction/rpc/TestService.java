package com.example.demo.distributedTransaction.rpc;

import org.springframework.stereotype.Component;

@Component
public class TestService {

    public void start() {
        System.out.println("TestService start");
        if(1==1){
            throw new RuntimeException("...");
        }
    }

    public void end(){
        System.out.println("TestService end");
    }
}
