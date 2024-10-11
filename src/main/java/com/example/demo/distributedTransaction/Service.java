package com.example.demo.distributedTransaction;

import com.example.demo.distributedTransaction.bean.ServerConfig;
import com.example.demo.distributedTransaction.rpc.AiService;
import com.example.demo.distributedTransaction.rpc.CharacteristicService;
import com.example.demo.distributedTransaction.rpc.TestService;
import com.example.demo.distributedTransaction.rpc.UserService;
import com.example.demo.distributedTransaction.saga.Method;
import com.example.demo.distributedTransaction.saga.SagaAction;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rpc.consumer",havingValue = "true")
public class Service {

    @Autowired
    private ServerConfig serverConfig;

    @DubboReference(url="dubbo://127.0.0.1:12345",version = "1.0.0")
    private UserService userService;

    @DubboReference(url="dubbo://127.0.0.1:1234",version = "1.0.0")
    private CharacteristicService characteristicService;

    @Autowired
    public AiService aiService;

    @SagaAction(methods = {@Method(name = "start",rollbackMethodFullName = "end")})
    public TestService testService;

    private String xxx = "123";

    public void testSaga(){
//        aiService.ask("...");
        testService.start();
    }


    public void business(){
        //调用接口
        Integer id = userService.addArbitraryUser();
        //调用接口
        characteristicService.addArbitraryCharacteristic(id);

        System.out.println("操作成功");
    }

}
