package com.example.demo.distributedTransaction.rpc;

import com.example.demo.distributedTransaction.rpc.external.TestExternal;
import com.example.demo.distributedTransaction.saga.Method;
import com.example.demo.distributedTransaction.saga.SagaAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SagaAction(methods = {@Method(name = "ask",rollbackMethodFullName = "quit")})
public class AiService {

    @Autowired
    private ChildService childService;

//    @Autowired
//    private TestExternal testExternal;

    public String ask(String question){
        System.out.println("子属性："+childService);
        if(1==1){
            throw new RuntimeException(childService.toString());
        }
        return "you have asked a sharp question: " + question+" and here is the answer: ok";
    }

    public void quit() {
        System.out.println("rollback");
    }

}
