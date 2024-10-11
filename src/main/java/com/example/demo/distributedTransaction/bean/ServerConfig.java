package com.example.demo.distributedTransaction.bean;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
public class ServerConfig {
    @Value("localhost:8081")
    public String userServer;
    @Value("localhost:8082")
    public String characterServer;
}
