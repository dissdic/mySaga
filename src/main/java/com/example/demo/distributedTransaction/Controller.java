package com.example.demo.distributedTransaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "rpc.consumer",havingValue = "true")
public class Controller {
    @Autowired
    private Service service;

    @GetMapping("/service")
    public String service(){
        service.business();
        return "success";
    }

    @GetMapping("/testSaga")
    public String saga(){
        service.testSaga();
        return "success";
    }

}
