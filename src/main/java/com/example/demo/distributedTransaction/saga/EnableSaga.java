package com.example.demo.distributedTransaction.saga;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SagaActionRegistrar.class)
public @interface EnableSaga {

    String[] basePackages() default {};
}
