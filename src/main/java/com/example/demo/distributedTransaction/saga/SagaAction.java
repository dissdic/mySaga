package com.example.demo.distributedTransaction.saga;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE,ElementType.FIELD})
public @interface SagaAction {

    String rollbackMethodFullName() default "";

    int retry() default -1;

    Method[] methods() default {};
}
