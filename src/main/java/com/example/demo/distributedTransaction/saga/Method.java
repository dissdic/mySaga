package com.example.demo.distributedTransaction.saga;

public @interface Method {

    String name() default "";

    int retry() default -1;

    String rollbackMethodFullName() default "";
}
