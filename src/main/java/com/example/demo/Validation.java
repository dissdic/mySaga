package com.example.demo;

import java.util.concurrent.Callable;

public class Validation {

    public static void main(String[] args) throws Exception{

        Runnable runnable = ()->{

            throw new RuntimeException();
        };

        Callable<String> call = ()->{
            if(1==1){
                throw new RuntimeException();
            }else{
                return "";
            }
        };

        runnable.run();
        call.call();
    }
}
