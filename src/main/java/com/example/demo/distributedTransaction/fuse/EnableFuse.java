package com.example.demo.distributedTransaction.fuse;


import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Import(FuseActionRegistrar.class)
public @interface EnableFuse {

    String[] packages() default {};
}
